package virtual.camera.app.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import com.scottyab.rootbeer.RootBeer

/**
 * Security Manager - Device security validation
 * Implements root detection, emulator detection, and security checks
 */
class SecurityManager(private val context: Context) {
    
    private val rootBeer = RootBeer(context)
    
    /**
     * Data class representing device security status
     */
    data class SecurityStatus(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggable: Boolean,
        val hasSuspiciousApps: Boolean,
        val hasRootManagementApps: Boolean,
        val hasPotentiallyDangerousApps: Boolean,
        val hasRootCloakingApps: Boolean,
        val isTestKeysBuild: Boolean,
        val hasDangerousProps: Boolean
    ) {
        /**
         * Check if device passes all security checks
         */
        fun isSecure(): Boolean {
            return !isRooted && 
                   !isEmulator && 
                   !isDebuggable && 
                   !hasSuspiciousApps &&
                   !hasRootManagementApps &&
                   !hasPotentiallyDangerousApps &&
                   !hasRootCloakingApps &&
                   !isTestKeysBuild &&
                   !hasDangerousProps
        }
        
        /**
         * Get security score (0-100)
         */
        fun getSecurityScore(): Int {
            var score = 100
            if (isRooted) score -= 30
            if (isEmulator) score -= 20
            if (isDebuggable) score -= 10
            if (hasSuspiciousApps) score -= 10
            if (hasRootManagementApps) score -= 10
            if (hasPotentiallyDangerousApps) score -= 5
            if (hasRootCloakingApps) score -= 10
            if (isTestKeysBuild) score -= 3
            if (hasDangerousProps) score -= 2
            return score.coerceAtLeast(0)
        }
        
        /**
         * Get list of security issues found
         */
        fun getSecurityIssues(): List<String> {
            val issues = mutableListOf<String>()
            if (isRooted) issues.add("Thiết bị đã root")
            if (isEmulator) issues.add("Phát hiện giả lập (Emulator)")
            if (isDebuggable) issues.add("Ứng dụng chế độ debug")
            if (hasSuspiciousApps) issues.add("Phát hiện ứng dụng đáng ngờ")
            if (hasRootManagementApps) issues.add("Phát hiện ứng dụng quản lý root")
            if (hasPotentiallyDangerousApps) issues.add("Phát hiện ứng dụng nguy hiểm")
            if (hasRootCloakingApps) issues.add("Phát hiện ứng dụng ẩn root")
            if (isTestKeysBuild) issues.add("Build với test keys")
            if (hasDangerousProps) issues.add("Phát hiện system properties nguy hiểm")
            return issues
        }
    }
    
    /**
     * Perform comprehensive device security check
     */
    fun isDeviceSecure(): SecurityStatus {
        return SecurityStatus(
            isRooted = checkIsRooted(),
            isEmulator = checkIsEmulator(),
            isDebuggable = checkIsDebuggable(),
            hasSuspiciousApps = checkHasSuspiciousApps(),
            hasRootManagementApps = rootBeer.detectRootManagementApps(),
            hasPotentiallyDangerousApps = rootBeer.detectPotentiallyDangerousApps(),
            hasRootCloakingApps = rootBeer.detectRootCloakingApps(),
            isTestKeysBuild = rootBeer.detectTestKeys(),
            hasDangerousProps = rootBeer.checkForDangerousProps()
        )
    }
    
    /**
     * Quick check if device is rooted
     */
    private fun checkIsRooted(): Boolean {
        return rootBeer.isRooted
    }
    
    /**
     * Check if running on emulator
     */
    private fun checkIsEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }
    
    /**
     * Check if app is debuggable
     */
    private fun checkIsDebuggable(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Check for suspicious apps that might be used for fraud
     */
    private fun checkHasSuspiciousApps(): Boolean {
        val suspiciousPackages = listOf(
            "com.noshufou.android.su",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.topjohnwu.magisk",
            "me.phh.superuser",
            "com.kingouser.com",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.forpda.lp",
            "com.android.vending.billing.InAppBillingService.LUCK"
        )
        
        val packageManager = context.packageManager
        for (packageName in suspiciousPackages) {
            try {
                packageManager.getPackageInfo(packageName, 0)
                return true
            } catch (e: Exception) {
                // Package not found, continue
            }
        }
        return false
    }
    
    /**
     * Perform quick security check (less thorough but faster)
     */
    fun quickSecurityCheck(): Boolean {
        return !checkIsRooted() && !checkIsEmulator() && !checkIsDebuggable()
    }
    
    /**
     * Check if USB debugging is enabled
     */
    fun isUsbDebuggingEnabled(): Boolean {
        return android.provider.Settings.Secure.getInt(
            context.contentResolver,
            android.provider.Settings.Global.ADB_ENABLED,
            0
        ) == 1
    }
    
    /**
     * Check if developer options are enabled
     */
    fun isDeveloperOptionsEnabled(): Boolean {
        return android.provider.Settings.Secure.getInt(
            context.contentResolver,
            android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }
}
