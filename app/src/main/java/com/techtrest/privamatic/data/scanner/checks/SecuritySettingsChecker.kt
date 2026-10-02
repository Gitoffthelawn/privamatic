package com.techtrest.privamatic.data.scanner.checks

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SecuritySettingsChecker(private val context: Context) {

    fun checkScreenLock(): PrivacyIssue {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                ?: return PrivacyIssue(
                    check = PrivacyCheck.SCREEN_LOCK,
                    isSecure = false,
                    currentStatus = "Unable to determine",
                    technicalDetails = "Keyguard service not available on this device"
                )
            val isSecure = keyguardManager.isDeviceSecure

            PrivacyIssue(
                check = PrivacyCheck.SCREEN_LOCK,
                isSecure = isSecure,
                currentStatus = if (isSecure) "Enabled" else "Disabled",
                technicalDetails = "Checked using KeyguardManager.isDeviceSecure"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.SCREEN_LOCK,
                isSecure = false,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkBiometricAuth(): PrivacyIssue {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // For SDK 26-28, biometric check is not reliably available
                return PrivacyIssue(
                    check = PrivacyCheck.BIOMETRIC_AUTH,
                    isSecure = true, // Don't penalize on older devices
                    currentStatus = "Not available on Android < 10",
                    technicalDetails = "BiometricManager API requires Android 10+"
                )
            }

            val hwResult = BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            if (hwResult == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                hwResult == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
                return PrivacyIssue(
                    check = PrivacyCheck.BIOMETRIC_AUTH,
                    isSecure = true,
                    currentStatus = "Not available on this device",
                    technicalDetails = "No biometric hardware (SDK ${Build.VERSION.SDK_INT})"
                )
            }

            val hasBiometric = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                checkBiometricAPI30Plus()
            } else {
                checkBiometricAPI29()
            }

            PrivacyIssue(
                check = PrivacyCheck.BIOMETRIC_AUTH,
                isSecure = hasBiometric,
                currentStatus = if (hasBiometric) "Available and enrolled" else "Not enrolled",
                technicalDetails = "Checked using BiometricManager (SDK ${Build.VERSION.SDK_INT})"
            )
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.BIOMETRIC_AUTH,
                isSecure = true, // Don't penalize on error
                isUnknown = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    fun checkSecurityPatch(): PrivacyIssue {
        return try {
            val patchDate = LocalDate.parse(Build.VERSION.SECURITY_PATCH)
            val today = LocalDate.now()
            val monthsOld = ChronoUnit.MONTHS.between(patchDate, today).toInt()

            when {
                monthsOld < 3 -> PrivacyIssue(
                    check = PrivacyCheck.SECURITY_PATCH,
                    isSecure = true,
                    currentStatus = context.getString(R.string.status_security_patch_current)
                )
                monthsOld < 6 -> PrivacyIssue(
                    check = PrivacyCheck.SECURITY_PATCH,
                    isSecure = true,
                    currentStatus = context.getString(R.string.fmt_security_patch_ageing, monthsOld)
                )
                monthsOld < 12 -> PrivacyIssue(
                    check = PrivacyCheck.SECURITY_PATCH,
                    isSecure = false,
                    currentStatus = context.getString(R.string.fmt_security_patch_outdated, monthsOld),
                    customPointDeduction = 5
                )
                else -> PrivacyIssue(
                    check = PrivacyCheck.SECURITY_PATCH,
                    isSecure = false,
                    currentStatus = context.getString(R.string.fmt_security_patch_critical, monthsOld),
                    customPointDeduction = 10
                )
            }
        } catch (e: Exception) {
            PrivacyIssue(
                check = PrivacyCheck.SECURITY_PATCH,
                isSecure = true,
                isUnknown = true,
                currentStatus = context.getString(R.string.status_security_patch_unknown)
            )
        }
    }

    private fun checkBiometricAPI29(): Boolean {
        val biometricManager = BiometricManager.from(context)
        @Suppress("DEPRECATION")
        val canAuthenticate = biometricManager.canAuthenticate()
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun checkBiometricAPI30Plus(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }
}
