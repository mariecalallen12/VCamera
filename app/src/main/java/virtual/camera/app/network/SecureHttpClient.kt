package virtual.camera.app.network

import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Secure HTTP Client - Network security with certificate pinning
 * Implements SSL/TLS pinning for secure API communication
 */
object SecureHttpClient {
    
    // Default timeout values
    private const val CONNECT_TIMEOUT_SECONDS = 30L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    
    /**
     * Configuration class for certificate pinning
     * NOTE: Replace with your actual API domains and certificate hashes
     */
    data class PinningConfig(
        val domain: String,
        val certificateHashes: List<String>
    ) {
        companion object {
            /**
             * Example configuration - REPLACE WITH YOUR ACTUAL VALUES
             * 
             * To get certificate hash, use:
             * openssl s_client -connect yourdomain.com:443 | \
             *   openssl x509 -pubkey -noout | \
             *   openssl pkey -pubin -outform der | \
             *   openssl dgst -sha256 -binary | \
             *   openssl enc -base64
             */
            val DEFAULT = PinningConfig(
                domain = "api.vcamera.app",  // TODO: Replace with actual domain
                certificateHashes = listOf(
                    "sha256/PLACEHOLDER_HASH_1=",  // TODO: Replace with actual hash
                    "sha256/PLACEHOLDER_HASH_2="   // Backup hash
                )
            )
        }
    }
    
    /**
     * Create a secure OkHttpClient with certificate pinning
     */
    fun create(
        config: PinningConfig = PinningConfig.DEFAULT,
        enableLogging: Boolean = false,
        additionalInterceptors: List<Interceptor> = emptyList()
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        // Add certificate pinning if valid hashes provided
        if (config.certificateHashes.isNotEmpty() && 
            !config.certificateHashes.first().contains("PLACEHOLDER")) {
            val certificatePinner = CertificatePinner.Builder()
                .apply {
                    config.certificateHashes.forEach { hash ->
                        add(config.domain, hash)
                    }
                }
                .build()
            builder.certificatePinner(certificatePinner)
        }
        
        // Add logging interceptor for debug builds
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        // Add custom headers interceptor
        builder.addInterceptor(createHeadersInterceptor())
        
        // Add additional custom interceptors
        additionalInterceptors.forEach { interceptor ->
            builder.addInterceptor(interceptor)
        }
        
        return builder.build()
    }
    
    /**
     * Create client without certificate pinning (for development only)
     */
    fun createInsecure(enableLogging: Boolean = true): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        builder.addInterceptor(createHeadersInterceptor())
        
        return builder.build()
    }
    
    /**
     * Create default headers interceptor
     */
    private fun createHeadersInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithHeaders = originalRequest.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "VCamera-Android")
                .method(originalRequest.method, originalRequest.body)
                .build()
            chain.proceed(requestWithHeaders)
        }
    }
    
    /**
     * Create authorization interceptor
     */
    fun createAuthInterceptor(tokenProvider: () -> String?): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = tokenProvider()
            
            val requestWithAuth = if (token != null) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            
            chain.proceed(requestWithAuth)
        }
    }
    
    /**
     * Create retry interceptor
     */
    fun createRetryInterceptor(maxRetries: Int = 3): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            var response = chain.proceed(request)
            var retryCount = 0
            
            while (!response.isSuccessful && retryCount < maxRetries) {
                retryCount++
                response.close()
                response = chain.proceed(request)
            }
            
            response
        }
    }
}
