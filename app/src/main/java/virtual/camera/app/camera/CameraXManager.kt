package virtual.camera.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX Manager - Modern camera management with quality thresholds
 * Implements SmartPay KYC standards for image capture
 */
class CameraXManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Quality thresholds (matching SmartPay KYC standards)
    companion object {
        const val MIN_FRAMES = 10              // Minimum continuous frames
        const val TIMEOUT_MS = 60000L          // 60 seconds timeout
        const val QUALITY_THRESHOLD = 25.0f    // Brightness threshold
        const val BLUR_THRESHOLD = 10.0f       // Blur tolerance
        const val IMAGE_WIDTH = 512            // Output image width
        const val IMAGE_HEIGHT = 512           // Output image height
        const val MAX_FILE_SIZE = 300000       // 300KB max file size
    }
    
    // Callback interfaces
    interface CameraStateCallback {
        fun onCameraReady()
        fun onCameraError(error: String)
    }
    
    interface ImageCaptureCallback {
        fun onImageCaptured(imageBytes: ByteArray)
        fun onCaptureError(error: String)
    }
    
    private var cameraStateCallback: CameraStateCallback? = null
    private var imageCaptureCallback: ImageCaptureCallback? = null
    
    fun setCameraStateCallback(callback: CameraStateCallback) {
        this.cameraStateCallback = callback
    }
    
    fun setImageCaptureCallback(callback: ImageCaptureCallback) {
        this.imageCaptureCallback = callback
    }
    
    /**
     * Start camera preview with specified settings
     */
    fun startCamera(
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
        qualityAnalyzer: ImageQualityAnalyzer? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Build Preview use case
                preview = Preview.Builder()
                    .setTargetResolution(Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Build ImageCapture use case
                imageCapture = ImageCapture.Builder()
                    .setTargetResolution(Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                // Build ImageAnalysis use case
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        qualityAnalyzer?.let { analyzer ->
                            analysis.setAnalyzer(cameraExecutor, analyzer)
                        }
                    }
                
                // Select camera
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()
                
                // Unbind existing use cases and bind new ones
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                
                cameraStateCallback?.onCameraReady()
                
            } catch (e: Exception) {
                cameraStateCallback?.onCameraError(e.message ?: "Unknown error")
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Capture image and return as ByteArray
     */
    fun captureImage() {
        imageCapture?.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        imageCaptureCallback?.onImageCaptured(bytes)
                    } catch (e: Exception) {
                        imageCaptureCallback?.onCaptureError(e.message ?: "Capture error")
                    } finally {
                        image.close()
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    imageCaptureCallback?.onCaptureError(exception.message ?: "Capture error")
                }
            }
        )
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera(previewView: PreviewView, newLensFacing: Int) {
        startCamera(previewView, newLensFacing)
    }
    
    /**
     * Enable/disable flash
     */
    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }
    
    /**
     * Set zoom level (0.0 to 1.0)
     */
    fun setZoomRatio(zoomRatio: Float) {
        camera?.cameraControl?.setLinearZoom(zoomRatio.coerceIn(0f, 1f))
    }
    
    /**
     * Check if camera has flash
     */
    fun hasFlash(): Boolean {
        return camera?.cameraInfo?.hasFlashUnit() == true
    }
    
    /**
     * Get current zoom state
     */
    fun getZoomState() = camera?.cameraInfo?.zoomState
    
    /**
     * Clean up resources
     */
    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
}
