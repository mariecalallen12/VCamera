package virtual.camera.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.IOException

/**
 * Secure Storage - Encrypted data storage
 * Implements AES-256 encryption for sensitive data
 */
class SecureStorage(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val PREFS_FILE_NAME = "vcamera_secure_prefs"
        private const val FILES_DIR = "secure_files"
    }
    
    // ==================== String Operations ====================
    
    /**
     * Save encrypted string value
     */
    fun saveString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }
    
    /**
     * Get encrypted string value
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return encryptedPrefs.getString(key, defaultValue)
    }
    
    /**
     * Save encrypted string synchronously
     */
    fun saveStringSync(key: String, value: String): Boolean {
        return encryptedPrefs.edit().putString(key, value).commit()
    }
    
    // ==================== Integer Operations ====================
    
    /**
     * Save encrypted integer value
     */
    fun saveInt(key: String, value: Int) {
        encryptedPrefs.edit().putInt(key, value).apply()
    }
    
    /**
     * Get encrypted integer value
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return encryptedPrefs.getInt(key, defaultValue)
    }
    
    // ==================== Long Operations ====================
    
    /**
     * Save encrypted long value
     */
    fun saveLong(key: String, value: Long) {
        encryptedPrefs.edit().putLong(key, value).apply()
    }
    
    /**
     * Get encrypted long value
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return encryptedPrefs.getLong(key, defaultValue)
    }
    
    // ==================== Boolean Operations ====================
    
    /**
     * Save encrypted boolean value
     */
    fun saveBoolean(key: String, value: Boolean) {
        encryptedPrefs.edit().putBoolean(key, value).apply()
    }
    
    /**
     * Get encrypted boolean value
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return encryptedPrefs.getBoolean(key, defaultValue)
    }
    
    // ==================== File Operations ====================
    
    /**
     * Save encrypted file
     */
    @Throws(IOException::class)
    fun saveFile(fileName: String, content: ByteArray) {
        val secureDir = File(context.filesDir, FILES_DIR)
        if (!secureDir.exists()) {
            secureDir.mkdirs()
        }
        
        val file = File(secureDir, fileName)
        
        // Delete existing file if it exists (EncryptedFile doesn't support overwrite)
        if (file.exists()) {
            file.delete()
        }
        
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        encryptedFile.openFileOutput().use { outputStream ->
            outputStream.write(content)
        }
    }
    
    /**
     * Read encrypted file
     */
    @Throws(IOException::class)
    fun readFile(fileName: String): ByteArray? {
        val file = File(File(context.filesDir, FILES_DIR), fileName)
        if (!file.exists()) return null
        
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        
        return encryptedFile.openFileInput().use { inputStream ->
            inputStream.readBytes()
        }
    }
    
    /**
     * Delete encrypted file
     */
    fun deleteFile(fileName: String): Boolean {
        val file = File(File(context.filesDir, FILES_DIR), fileName)
        return if (file.exists()) file.delete() else false
    }
    
    /**
     * Check if encrypted file exists
     */
    fun fileExists(fileName: String): Boolean {
        val file = File(File(context.filesDir, FILES_DIR), fileName)
        return file.exists()
    }
    
    /**
     * Get list of all encrypted files
     */
    fun listFiles(): List<String> {
        val secureDir = File(context.filesDir, FILES_DIR)
        return secureDir.listFiles()?.map { it.name } ?: emptyList()
    }
    
    // ==================== Utility Operations ====================
    
    /**
     * Remove a key from encrypted preferences
     */
    fun remove(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }
    
    /**
     * Check if key exists in encrypted preferences
     */
    fun contains(key: String): Boolean {
        return encryptedPrefs.contains(key)
    }
    
    /**
     * Clear all encrypted preferences
     */
    fun clearPreferences() {
        encryptedPrefs.edit().clear().apply()
    }
    
    /**
     * Clear all encrypted files
     */
    fun clearFiles() {
        val secureDir = File(context.filesDir, FILES_DIR)
        secureDir.listFiles()?.forEach { it.delete() }
    }
    
    /**
     * Clear all secure storage (preferences and files)
     */
    fun clearAll() {
        clearPreferences()
        clearFiles()
    }
}
