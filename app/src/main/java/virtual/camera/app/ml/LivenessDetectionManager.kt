package virtual.camera.app.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face

/**
 * Liveness Detection Manager - Anti-spoofing detection
 * Implements 3-angle capture and liveness validation
 */
class LivenessDetectionManager(
    private val faceDetectionManager: FaceDetectionManager
) {
    
    /**
     * Liveness detection state
     */
    enum class LivenessState {
        IDLE,
        DETECTING_FRONT,
        DETECTING_LEFT,
        DETECTING_RIGHT,
        COMPLETED,
        FAILED
    }
    
    /**
     * Liveness detection result
     */
    data class LivenessResult(
        val isLive: Boolean,
        val frontCaptured: Boolean,
        val leftCaptured: Boolean,
        val rightCaptured: Boolean,
        val confidenceScore: Float,
        val errorMessage: String? = null
    ) {
        fun getCompletionPercentage(): Int {
            var count = 0
            if (frontCaptured) count++
            if (leftCaptured) count++
            if (rightCaptured) count++
            return (count * 100) / 3
        }
    }
    
    /**
     * Callback interface for liveness detection
     */
    interface LivenessCallback {
        fun onStateChanged(state: LivenessState, instruction: String)
        fun onAngleCaptured(angle: FaceDetectionManager.FaceAngle)
        fun onLivenessResult(result: LivenessResult)
        fun onError(error: String)
    }
    
    // Detection state
    private var currentState: LivenessState = LivenessState.IDLE
    private var frontCaptured = false
    private var leftCaptured = false
    private var rightCaptured = false
    
    // Captured face data
    private var frontFaceData: ByteArray? = null
    private var leftFaceData: ByteArray? = null
    private var rightFaceData: ByteArray? = null
    
    // Frame counters for stability
    private var consecutiveValidFrames = 0
    
    private var callback: LivenessCallback? = null
    
    companion object {
        private const val REQUIRED_CONSECUTIVE_FRAMES = 5
        private const val MIN_CONFIDENCE_SCORE = 0.7f
    }
    
    /**
     * Set liveness callback
     */
    fun setCallback(callback: LivenessCallback) {
        this.callback = callback
    }
    
    /**
     * Start liveness detection process
     */
    fun startDetection() {
        reset()
        currentState = LivenessState.DETECTING_FRONT
        callback?.onStateChanged(currentState, "Vui lòng nhìn thẳng vào camera")
    }
    
    /**
     * Reset detection state
     */
    fun reset() {
        currentState = LivenessState.IDLE
        frontCaptured = false
        leftCaptured = false
        rightCaptured = false
        frontFaceData = null
        leftFaceData = null
        rightFaceData = null
        consecutiveValidFrames = 0
    }
    
    /**
     * Process frame for liveness detection
     */
    fun processFrame(
        bitmap: Bitmap,
        onFrameProcessed: (Boolean) -> Unit
    ) {
        if (currentState == LivenessState.COMPLETED || currentState == LivenessState.FAILED) {
            onFrameProcessed(false)
            return
        }
        
        faceDetectionManager.detectFacesFast(bitmap, object : FaceDetectionManager.FaceDetectionCallback {
            override fun onFacesDetected(
                faces: List<Face>,
                validationResult: FaceDetectionManager.FaceValidationResult
            ) {
                if (faces.isEmpty()) {
                    consecutiveValidFrames = 0
                    onFrameProcessed(false)
                    return
                }
                
                val face = faces[0]
                val targetAngle = getCurrentTargetAngle()
                
                if (targetAngle != null && faceDetectionManager.validateFaceForAngle(face, targetAngle)) {
                    consecutiveValidFrames++
                    
                    if (consecutiveValidFrames >= REQUIRED_CONSECUTIVE_FRAMES) {
                        captureCurrentAngle(bitmap)
                        moveToNextState()
                        onFrameProcessed(true)
                        return
                    }
                } else {
                    consecutiveValidFrames = 0
                }
                
                onFrameProcessed(false)
            }
            
            override fun onDetectionFailed(error: Exception) {
                consecutiveValidFrames = 0
                callback?.onError(error.message ?: "Lỗi phát hiện khuôn mặt")
                onFrameProcessed(false)
            }
        })
    }
    
    /**
     * Get current target angle based on state
     */
    private fun getCurrentTargetAngle(): FaceDetectionManager.FaceAngle? {
        return when (currentState) {
            LivenessState.DETECTING_FRONT -> FaceDetectionManager.FaceAngle.FRONT
            LivenessState.DETECTING_LEFT -> FaceDetectionManager.FaceAngle.LEFT
            LivenessState.DETECTING_RIGHT -> FaceDetectionManager.FaceAngle.RIGHT
            else -> null
        }
    }
    
    /**
     * Capture current angle image
     */
    private fun captureCurrentAngle(bitmap: Bitmap) {
        val imageBytes = bitmapToByteArray(bitmap)
        
        when (currentState) {
            LivenessState.DETECTING_FRONT -> {
                frontCaptured = true
                frontFaceData = imageBytes
                callback?.onAngleCaptured(FaceDetectionManager.FaceAngle.FRONT)
            }
            LivenessState.DETECTING_LEFT -> {
                leftCaptured = true
                leftFaceData = imageBytes
                callback?.onAngleCaptured(FaceDetectionManager.FaceAngle.LEFT)
            }
            LivenessState.DETECTING_RIGHT -> {
                rightCaptured = true
                rightFaceData = imageBytes
                callback?.onAngleCaptured(FaceDetectionManager.FaceAngle.RIGHT)
            }
            else -> {}
        }
    }
    
    /**
     * Move to next detection state
     */
    private fun moveToNextState() {
        consecutiveValidFrames = 0
        
        currentState = when (currentState) {
            LivenessState.DETECTING_FRONT -> {
                callback?.onStateChanged(LivenessState.DETECTING_LEFT, "Vui lòng quay mặt sang trái")
                LivenessState.DETECTING_LEFT
            }
            LivenessState.DETECTING_LEFT -> {
                callback?.onStateChanged(LivenessState.DETECTING_RIGHT, "Vui lòng quay mặt sang phải")
                LivenessState.DETECTING_RIGHT
            }
            LivenessState.DETECTING_RIGHT -> {
                val result = calculateLivenessResult()
                callback?.onLivenessResult(result)
                if (result.isLive) LivenessState.COMPLETED else LivenessState.FAILED
            }
            else -> currentState
        }
        
        if (currentState == LivenessState.COMPLETED || currentState == LivenessState.FAILED) {
            callback?.onStateChanged(currentState, 
                if (currentState == LivenessState.COMPLETED) "Xác thực thành công!" 
                else "Xác thực thất bại. Vui lòng thử lại.")
        }
    }
    
    /**
     * Calculate final liveness result
     */
    private fun calculateLivenessResult(): LivenessResult {
        val allCaptured = frontCaptured && leftCaptured && rightCaptured
        
        // Calculate confidence based on captured angles
        var confidence = 0f
        if (frontCaptured) confidence += 0.4f
        if (leftCaptured) confidence += 0.3f
        if (rightCaptured) confidence += 0.3f
        
        val isLive = allCaptured && confidence >= MIN_CONFIDENCE_SCORE
        
        return LivenessResult(
            isLive = isLive,
            frontCaptured = frontCaptured,
            leftCaptured = leftCaptured,
            rightCaptured = rightCaptured,
            confidenceScore = confidence,
            errorMessage = if (!isLive) "Không thể xác thực. Vui lòng thử lại." else null
        )
    }
    
    /**
     * Get captured face images
     */
    fun getCapturedImages(): Map<FaceDetectionManager.FaceAngle, ByteArray?> {
        return mapOf(
            FaceDetectionManager.FaceAngle.FRONT to frontFaceData,
            FaceDetectionManager.FaceAngle.LEFT to leftFaceData,
            FaceDetectionManager.FaceAngle.RIGHT to rightFaceData
        )
    }
    
    /**
     * Get current detection state
     */
    fun getCurrentState(): LivenessState = currentState
    
    /**
     * Convert bitmap to byte array
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
