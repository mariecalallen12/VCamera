package virtual.camera.app.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Face Detection Manager - ML Kit integration for face detection
 * Implements face detection and validation for KYC/verification purposes
 */
class FaceDetectionManager {
    
    /**
     * Face validation result data class
     */
    data class FaceValidationResult(
        val isValid: Boolean,
        val faceCount: Int,
        val isFrontal: Boolean,
        val hasOpenEyes: Boolean,
        val isSmiling: Boolean,
        val headEulerAngleY: Float,
        val headEulerAngleZ: Float,
        val leftEyeOpenProbability: Float,
        val rightEyeOpenProbability: Float,
        val smilingProbability: Float,
        val boundingBox: Rect?,
        val errorMessage: String? = null
    ) {
        /**
         * Get validation score (0-100)
         */
        fun getValidationScore(): Int {
            var score = 0
            if (faceCount == 1) score += 30
            if (isFrontal) score += 25
            if (hasOpenEyes) score += 25
            if (isSmiling) score += 10
            if (boundingBox != null) score += 10
            return score.coerceAtMost(100)
        }
        
        /**
         * Get list of issues with the face
         */
        fun getIssues(): List<String> {
            val issues = mutableListOf<String>()
            if (faceCount == 0) issues.add("Không phát hiện khuôn mặt")
            if (faceCount > 1) issues.add("Phát hiện nhiều khuôn mặt")
            if (!isFrontal) issues.add("Khuôn mặt không ở chính diện")
            if (!hasOpenEyes) issues.add("Mắt nhắm")
            return issues
        }
    }
    
    // Face detector with accurate mode
    private val accurateDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }
    
    // Face detector with fast mode for real-time
    private val fastDetector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(options)
    }
    
    /**
     * Callback interface for face detection
     */
    interface FaceDetectionCallback {
        fun onFacesDetected(faces: List<Face>, validationResult: FaceValidationResult)
        fun onDetectionFailed(error: Exception)
    }
    
    /**
     * Detect faces in bitmap using accurate mode
     */
    fun detectFaces(
        bitmap: Bitmap,
        callback: FaceDetectionCallback
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        accurateDetector.process(image)
            .addOnSuccessListener { faces ->
                val validationResult = validateFaces(faces)
                callback.onFacesDetected(faces, validationResult)
            }
            .addOnFailureListener { e ->
                callback.onDetectionFailed(e)
            }
    }
    
    /**
     * Detect faces using fast mode (for real-time preview)
     */
    fun detectFacesFast(
        bitmap: Bitmap,
        callback: FaceDetectionCallback
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        fastDetector.process(image)
            .addOnSuccessListener { faces ->
                val validationResult = validateFaces(faces)
                callback.onFacesDetected(faces, validationResult)
            }
            .addOnFailureListener { e ->
                callback.onDetectionFailed(e)
            }
    }
    
    /**
     * Validate detected faces for KYC requirements
     */
    private fun validateFaces(faces: List<Face>): FaceValidationResult {
        if (faces.isEmpty()) {
            return FaceValidationResult(
                isValid = false,
                faceCount = 0,
                isFrontal = false,
                hasOpenEyes = false,
                isSmiling = false,
                headEulerAngleY = 0f,
                headEulerAngleZ = 0f,
                leftEyeOpenProbability = 0f,
                rightEyeOpenProbability = 0f,
                smilingProbability = 0f,
                boundingBox = null,
                errorMessage = "Không phát hiện khuôn mặt"
            )
        }
        
        if (faces.size > 1) {
            return FaceValidationResult(
                isValid = false,
                faceCount = faces.size,
                isFrontal = false,
                hasOpenEyes = false,
                isSmiling = false,
                headEulerAngleY = 0f,
                headEulerAngleZ = 0f,
                leftEyeOpenProbability = 0f,
                rightEyeOpenProbability = 0f,
                smilingProbability = 0f,
                boundingBox = null,
                errorMessage = "Phát hiện nhiều khuôn mặt. Vui lòng chỉ có một người trong khung hình."
            )
        }
        
        val face = faces[0]
        
        // Check if face is frontal (head rotation within acceptable range)
        val isFrontal = kotlin.math.abs(face.headEulerAngleY) < FRONTAL_ANGLE_THRESHOLD &&
                        kotlin.math.abs(face.headEulerAngleZ) < FRONTAL_ANGLE_THRESHOLD
        
        // Check eye open probability
        val leftEyeOpen = face.leftEyeOpenProbability ?: 0f
        val rightEyeOpen = face.rightEyeOpenProbability ?: 0f
        val hasOpenEyes = leftEyeOpen > EYE_OPEN_THRESHOLD && rightEyeOpen > EYE_OPEN_THRESHOLD
        
        // Check smiling probability
        val smilingProb = face.smilingProbability ?: 0f
        val isSmiling = smilingProb > SMILE_THRESHOLD
        
        // Determine if face is valid for KYC
        val isValid = isFrontal && hasOpenEyes
        
        // Generate error message if not valid
        val errorMessage = when {
            !isFrontal -> "Vui lòng nhìn thẳng vào camera"
            !hasOpenEyes -> "Vui lòng mở mắt"
            else -> null
        }
        
        return FaceValidationResult(
            isValid = isValid,
            faceCount = 1,
            isFrontal = isFrontal,
            hasOpenEyes = hasOpenEyes,
            isSmiling = isSmiling,
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ,
            leftEyeOpenProbability = leftEyeOpen,
            rightEyeOpenProbability = rightEyeOpen,
            smilingProbability = smilingProb,
            boundingBox = face.boundingBox,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Validate face for specific angle (for liveness detection)
     */
    fun validateFaceForAngle(
        face: Face,
        targetAngle: FaceAngle
    ): Boolean {
        val eulerY = face.headEulerAngleY
        
        return when (targetAngle) {
            FaceAngle.FRONT -> kotlin.math.abs(eulerY) < FRONTAL_ANGLE_THRESHOLD
            FaceAngle.LEFT -> eulerY > SIDE_ANGLE_MIN && eulerY < SIDE_ANGLE_MAX
            FaceAngle.RIGHT -> eulerY < -SIDE_ANGLE_MIN && eulerY > -SIDE_ANGLE_MAX
        }
    }
    
    /**
     * Face angle enum for liveness detection
     */
    enum class FaceAngle {
        FRONT,
        LEFT,
        RIGHT
    }
    
    /**
     * Close detectors and release resources
     */
    fun close() {
        accurateDetector.close()
        fastDetector.close()
    }
    
    companion object {
        // Threshold for determining if face is frontal (in degrees)
        private const val FRONTAL_ANGLE_THRESHOLD = 15f
        
        // Threshold for determining if eyes are open
        private const val EYE_OPEN_THRESHOLD = 0.5f
        
        // Threshold for determining if person is smiling
        private const val SMILE_THRESHOLD = 0.5f
        
        // Thresholds for side angle detection (liveness)
        private const val SIDE_ANGLE_MIN = 20f
        private const val SIDE_ANGLE_MAX = 45f
    }
}
