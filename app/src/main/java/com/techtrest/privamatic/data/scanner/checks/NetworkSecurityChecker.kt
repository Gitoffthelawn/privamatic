package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue

class NetworkSecurityChecker(private val context: Context) {

    fun checkVpnConnection(): PrivacyIssue {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return unknownVpn("Connectivity service not available on this device")
            val hasVpn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            } else {
                false
            }

            PrivacyIssue(
                check = PrivacyCheck.VPN_CONNECTION,
                isSecure = hasVpn,
                currentStatus = if (hasVpn) "Active" else "Not active",
                technicalDetails = if (hasVpn)
                    "Checked using NetworkCapabilities.TRANSPORT_VPN"
                else
                    "Checked using NetworkCapabilities.TRANSPORT_VPN\nTip: consider enabling Always-On VPN in Settings → Network → VPN"
            )
        } catch (e: Exception) {
            unknownVpn("Error: ${e.message}")
        }
    }

    /**
     * Undetermined results cost 0 points and display as unknown, but stay isSecure = false
     * so the related quick win and tips still appear.
     */
    private fun unknownVpn(details: String) = PrivacyIssue(
        check = PrivacyCheck.VPN_CONNECTION,
        isSecure = false,
        isUnknown = true,
        customPointDeduction = 0,
        currentStatus = context.getString(R.string.status_vpn_unknown),
        technicalDetails = details
    )

    fun checkPrivateDns(): PrivacyIssue = checkPrivateDns(Build.VERSION.SDK_INT)

    /** [sdkInt] is a parameter so tests can take the Android 9+ path (SDK_INT is 0 in unit tests). */
    internal fun checkPrivateDns(sdkInt: Int): PrivacyIssue {
        return try {
            if (sdkInt >= Build.VERSION_CODES.P) {
                val privateDnsMode = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_mode"
                )

                val hostname = Settings.Global.getString(
                    context.contentResolver,
                    "private_dns_specifier"
                )
                val rawIssue = privateDnsIssueForMode(privateDnsMode, hostname)

                // VPN tunnels DNS through the encrypted connection — don't penalise if VPN is active
                if (!rawIssue.isSecure && isVpnActive()) {
                    PrivacyIssue(
                        check = PrivacyCheck.PRIVATE_DNS,
                        isSecure = true,
                        currentStatus = "VPN active — DNS handled by VPN",
                        technicalDetails = "Private DNS is not configured, but an active VPN handles DNS routing"
                    )
                } else {
                    rawIssue
                }
            } else {
                // Private DNS not available before Android 9
                PrivacyIssue(
                    check = PrivacyCheck.PRIVATE_DNS,
                    isSecure = true, // Don't penalize older devices
                    currentStatus = "Not available on Android < 9",
                    technicalDetails = "Private DNS requires Android 9 (API 28) or higher"
                )
            }
        } catch (e: Exception) {
            unknownPrivateDns("Error: ${e.message}")
        }
    }

    /**
     * Maps the private_dns_mode setting to a result. AOSP defines only "off",
     * "opportunistic" and "hostname"; unset means the opportunistic default.
     */
    internal fun privateDnsIssueForMode(privateDnsMode: String?, hostname: String?): PrivacyIssue =
        when (privateDnsMode) {
            "hostname" -> if (!hostname.isNullOrEmpty()) {
                PrivacyIssue(
                    check = PrivacyCheck.PRIVATE_DNS,
                    isSecure = true,
                    currentStatus = "Custom hostname configured",
                    technicalDetails = "Hostname: $hostname"
                )
            } else {
                PrivacyIssue(
                    check = PrivacyCheck.PRIVATE_DNS,
                    isSecure = false,
                    currentStatus = "Hostname mode set but no hostname configured",
                    technicalDetails = "Hostname mode is active but no hostname is provided"
                )
            }
            "opportunistic", null -> PrivacyIssue(
                check = PrivacyCheck.PRIVATE_DNS,
                isSecure = true,
                currentStatus = "Automatic mode (opportunistic)",
                technicalDetails = "Automatic mode"
            )
            "off" -> PrivacyIssue(
                check = PrivacyCheck.PRIVATE_DNS,
                isSecure = false,
                currentStatus = "Not configured",
                technicalDetails = "Disabled"
            )
            // A value outside the AOSP set: the real mode can't be read, so it is unknown
            else -> unknownPrivateDns("Unknown mode: $privateDnsMode")
        }

    /** Same unknown rule as [unknownVpn]: 0 points, unknown display, quick win kept. */
    private fun unknownPrivateDns(details: String) = PrivacyIssue(
        check = PrivacyCheck.PRIVATE_DNS,
        isSecure = false,
        isUnknown = true,
        customPointDeduction = 0,
        currentStatus = context.getString(R.string.status_private_dns_unknown),
        technicalDetails = details
    )

    private fun isVpnActive(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if advertising ID has been manually verified as deleted.
     * Reads a persisted boolean from SharedPreferences written by AdIdVerificationScreen.
     * Auto-passes when Play Services is absent, disabled or replaced by microG — there is no
     * Google-served Advertising ID to delete.
     *
     * Deliberate exception to the unknown rule (unknown = 0 points): the Ad ID state can't
     * be read without declaring Google's AD_ID permission (apps targeting Android 13+ that
     * lack it only get zeros back), so the app never detects it. The -5 until the user
     * confirms is an intentional nudge towards the quick win, and the confirmation is
     * user-reported. The status says "not verified", never that the Ad ID was found active.
     */
    fun checkAdvertisingId(): PrivacyIssue {
        // Only real, enabled Play Services serves a Google Advertising ID. Absent, disabled
        // and microG are read through GoogleServicesChecker so both checks agree.
        val notApplicableStatus = when (GoogleServicesChecker(context).playServicesState()) {
            PlayServicesState.ABSENT -> R.string.status_ad_id_not_applicable
            PlayServicesState.MICROG -> R.string.status_ad_id_not_applicable_microg
            PlayServicesState.DISABLED -> R.string.status_ad_id_not_applicable_disabled
            PlayServicesState.PRIVILEGED, PlayServicesState.SANDBOXED -> null
        }
        if (notApplicableStatus != null) {
            return PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = true,
                currentStatus = context.getString(notApplicableStatus)
            )
        }

        val prefs = context.getSharedPreferences(AD_ID_PREFS_NAME, Context.MODE_PRIVATE)
        val isVerified = prefs.getBoolean(KEY_AD_ID_VERIFIED, false)
        return if (isVerified) {
            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = true,
                currentStatus = "Disabled (self-reported)"
            )
        } else {
            PrivacyIssue(
                check = PrivacyCheck.ADVERTISING_ID,
                isSecure = false,
                currentStatus = context.getString(R.string.status_ad_id_not_verified)
            )
        }
    }

    companion object {
        internal const val AD_ID_PREFS_NAME = "ad_id_prefs"
        internal const val KEY_AD_ID_VERIFIED = "ad_id_verified"
        internal const val KEY_AD_ID_TIMESTAMP = "ad_id_verified_timestamp"
    }
}
