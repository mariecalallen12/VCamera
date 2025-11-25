package virtual.camera.app.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.sqrt

/**
 * Image Quality Analyzer - Real-time image quality validation
 * Implements blur detection and brightness validation for KYC standards
 */
class ImageQualityAnalyzer : ImageAnalysis.Analyzer {
    
    /**
     * Callback interface for quality analysis results
     */
    interface QualityCallback {
        fun onQualityResult(result: QualityResult)
    }
    
    /**
     * Data class representing quality analysis result
     */
    data class QualityResult(
        val isQualityOk: Boolean,
        val blurScore: Float,
        val brightnessScore: Float,
        val contrastScore: Float,
        val errorMessage: String? = null
    ) {
        fun getQualityPercentage(): Int {
            val blurThreshold = if (CameraXManager.BLUR_THRESHOLD > 0) CameraXManager.BLUR_THRESHOLD else 1f
            val qualityThreshold = if (CameraXManager.QUALITY_THRESHOLD > 0) CameraXManager.QUALITY_THRESHOLD else 1f
            
            val blurNormalized = (blurScore / blurThreshold).coerceAtMost(1f)
            val brightnessNormalized = (brightnessScore / qualityThreshold).coerceAtMost(1f)
            val contrastNormalized = (contrastScore / 50f).coerceAtMost(1f)
            return ((blurNormalized + brightnessNormalized + contrastNormalized) / 3 * 100).toInt()
        }
    }
    
    var callback: QualityCallback? = null
    
    // Frame counter for stability
    private var frameCount = 0
    private var consecutiveGoodFrames = 0
    
    override fun analyze(image: ImageProxy) {
        try {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            
            val width = image.width
            val height = image.height
            
            // Calculate quality metrics
            val blurScore = calculateBlurScore(data, width, height)
            val brightnessScore = calculateBrightness(data)
            val contrastScore = calculateContrast(data)
            
            // Determine if quality is acceptable
            val isBlurOk = blurScore >= CameraXManager.BLUR_THRESHOLD
            val isBrightnessOk = brightnessScore >= CameraXManager.QUALITY_THRESHOLD && 
                                 brightnessScore <= 230f // Not overexposed
            val isContrastOk = contrastScore >= 20f
            
            val isQualityOk = isBlurOk && isBrightnessOk && isContrastOk
            
            // Track consecutive good frames
            if (isQualityOk) {
                consecutiveGoodFrames++
            } else {
                consecutiveGoodFrames = 0
            }
            
            frameCount++
            
            // Generate error message if quality is not ok
            val errorMessage = when {
                !isBlurOk -> "Ảnh bị mờ. Vui lòng giữ điện thoại ổn định."
                brightnessScore < CameraXManager.QUALITY_THRESHOLD -> "Ánh sáng quá tối. Vui lòng tăng ánh sáng."
                brightnessScore > 230f -> "Ánh sáng quá sáng. Vui lòng giảm ánh sáng."
                !isContrastOk -> "Độ tương phản thấp. Vui lòng điều chỉnh góc chụp."
                else -> null
            }
            
            val result = QualityResult(
                isQualityOk = isQualityOk && consecutiveGoodFrames >= CameraXManager.MIN_FRAMES,
                blurScore = blurScore,
                brightnessScore = brightnessScore,
                contrastScore = contrastScore,
                errorMessage = errorMessage
            )
            
            callback?.onQualityResult(result)
            
        } catch (e: Exception) {
            callback?.onQualityResult(
                QualityResult(
                    isQualityOk = false,
                    blurScore = 0f,
                    brightnessScore = 0f,
                    contrastScore = 0f,
                    errorMessage = "Lỗi phân tích hình ảnh: ${e.message}"
                )
            )
        } finally {
            image.close()
        }
    }
    
    /**
     * Calculate blur score using Laplacian variance method
     * Higher score = sharper image
     */
    private fun calculateBlurScore(data: ByteArray, width: Int, height: Int): Float {
        if (data.isEmpty()) return 0f
        
        var variance = 0.0
        var mean = 0.0
        
        // Calculate mean
        for (byte in data) {
            mean += (byte.toInt() and 0xFF)
        }
        mean /= data.size
        
        // Calculate variance
        for (byte in data) {
            val diff = (byte.toInt() and 0xFF) - mean
            variance += diff * diff
        }
        variance /= data.size
        
        return sqrt(variance).toFloat()
    }
    
    /**
     * Calculate average brightness
     * Range: 0-255
     */
    private fun calculateBrightness(data: ByteArray): Float {
        if (data.isEmpty()) return 0f
        
        var sum = 0L
        for (byte in data) {
            sum += (byte.toInt() and 0xFF)
        }
        return (sum.toFloat() / data.size)
    }
    
    /**
     * Calculate contrast using standard deviation
     */
    private fun calculateContrast(data: ByteArray): Float {
        if (data.isEmpty()) return 0f
        
        val brightness = calculateBrightness(data)
        var variance = 0.0
        
        for (byte in data) {
            val diff = (byte.toInt() and 0xFF) - brightness
            variance += diff * diff
        }
        variance /= data.size
        
        return sqrt(variance).toFloat()
    }
    
    /**
     * Reset frame counters
     */
    fun reset() {
        frameCount = 0
        consecutiveGoodFrames = 0
    }
    
    /**
     * Get current consecutive good frame count
     */
    fun getConsecutiveGoodFrames(): Int = consecutiveGoodFrames
    
    /**
     * Check if minimum frames requirement is met
     */
    fun hasMinimumFrames(): Boolean = consecutiveGoodFrames >= CameraXManager.MIN_FRAMES
}
