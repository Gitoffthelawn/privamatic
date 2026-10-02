package com.techtrest.privamatic.data.model

import com.techtrest.privamatic.data.util.PackageManagerUtil

data class FlaggedApp(
    val packageName: String,
    val appName: String,
    val associatedCheck: PrivacyCheck,
    val isBlacklisted: Boolean,
    val isSystemApp: Boolean
) {
    companion object {
        /**
         * Data-broker vendor namespaces. Matching by vendor prefix is deliberate: the
         * blacklist blocks whole vendors, not specific apps. A package matches when it is
         * the namespace itself (com.whatsapp) or sits under it (com.whatsapp.w4b), so
         * lookalikes such as com.whatsappfoo do not match.
         */
        private val BLACKLIST_PREFIXES = listOf(
            "com.google", "com.facebook", "com.instagram",
            "com.whatsapp", "com.microsoft", "com.amazon"
        )

        fun isBlacklisted(packageName: String): Boolean =
            BLACKLIST_PREFIXES.any { packageName == it || packageName.startsWith("$it.") }

        /**
         * microG runs under Google's package name, so it matches the "com.google" blacklist
         * prefix despite being the privacy-respecting replacement. Never blacklist it — the
         * user needs the trust toggle enabled to accept its trade-offs.
         */
        fun isBlacklisted(packageName: String, isMicroGInstalled: Boolean): Boolean =
            isBlacklisted(packageName) &&
                !PackageManagerUtil.isMicroGPackage(packageName, isMicroGInstalled)
    }
}
