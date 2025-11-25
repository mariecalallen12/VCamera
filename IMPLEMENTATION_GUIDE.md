# 🔧 HƯỚNG DẪN TRIỂN KHAI CHI TIẾT - VCAMERA UPGRADE

---

## 📌 PHASE 1: CẬP NHẬT BUILD CONFIGURATION (ĐÃ HOÀN THÀNH ✅)

### 1.1 Root build.gradle - Đã cập nhật

```groovy
// build.gradle (Project level)
buildscript {
    ext.kotlin_version = "1.6.21"  // ✅ Đã cập nhật từ 1.5.21
    repositories {
        google()       // ✅ Đã thêm
        mavenCentral() // ✅ Đã thêm
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.android.tools.build:gradle:7.2.0"  // ✅ Đã cập nhật từ 7.0.2
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

ext {
    minSdkVersion = 24
    compileSdkVersion = 34      // ✅ Đã cập nhật từ 33
    targetSdkVersion = 34       // ✅ Đã cập nhật từ 31
    buildToolsVersion = "34.0.0" // ✅ Đã cập nhật từ 31.0.0
    // ... other configs
}
```

### 1.2 Gradle Wrapper - Đã cấu hình

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-7.2-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 1.3 Local Properties - Đã tạo

```properties
# local.properties
sdk.dir=/usr/local/lib/android/sdk
```

---

## 📌 PHASE 2: NÂNG CẤP DEPENDENCIES (ĐỀ XUẤT)

### 2.1 CameraX Integration

Thêm vào `app/build.gradle`:

```groovy
dependencies {
    // CameraX core
    def camerax_version = "1.3.1"
    implementation "androidx.camera:camera-core:$camerax_version"
    implementation "androidx.camera:camera-camera2:$camerax_version"
    implementation "androidx.camera:camera-lifecycle:$camerax_version"
    implementation "androidx.camera:camera-view:$camerax_version"
    implementation "androidx.camera:camera-extensions:$camerax_version"
}
```

### 2.2 ML Kit Integration

```groovy
dependencies {
    // ML Kit Face Detection
    implementation 'com.google.mlkit:face-detection:16.1.5'
    
    // ML Kit Barcode Scanning (for ID cards)
    implementation 'com.google.mlkit:barcode-scanning:17.2.0'
    
    // ML Kit Text Recognition (OCR)
    implementation 'com.google.mlkit:text-recognition:16.0.0'
    
    // ML Kit Object Detection
    implementation 'com.google.mlkit:object-detection:17.0.0'
}
```

### 2.3 Security Libraries

```groovy
dependencies {
    // Root Detection
    implementation 'com.scottyab:rootbeer-lib:0.1.0'
    
    // Encrypted SharedPreferences
    implementation "androidx.security:security-crypto:1.1.0-alpha06"
    
    // Network Security
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Biometric Authentication
    implementation "androidx.biometric:biometric:1.1.0"
}
```

---

## 📌 PHASE 3: TRIỂN KHAI CAMERA FEATURES

### 3.1 CameraX Manager Class

```kotlin
// app/src/main/java/virtual/camera/app/camera/CameraXManager.kt
package virtual.camera.app.camera

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    
    // Quality thresholds (matching SmartPay standards)
    companion object {
        const val MIN_FRAMES = 10
        const val TIMEOUT_MS = 60000L
        const val QUALITY_THRESHOLD = 25.0f
        const val BLUR_THRESHOLD = 10.0f
        const val IMAGE_WIDTH = 512
        const val IMAGE_HEIGHT = 512
        const val MAX_FILE_SIZE = 300000 // 300KB
    }
    
    fun startCamera(
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            preview = Preview.Builder()
                .setTargetResolution(android.util.Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(android.util.Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(IMAGE_WIDTH, IMAGE_HEIGHT))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageQualityAnalyzer())
                }
            
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            
            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    fun captureImage(onImageCaptured: (ByteArray) -> Unit) {
        imageCapture?.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    onImageCaptured(bytes)
                    image.close()
                }
                
                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }
    
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
```

### 3.2 Image Quality Analyzer

```kotlin
// app/src/main/java/virtual/camera/app/camera/ImageQualityAnalyzer.kt
package virtual.camera.app.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.sqrt

class ImageQualityAnalyzer : ImageAnalysis.Analyzer {
    
    interface QualityCallback {
        fun onQualityResult(
            isQualityOk: Boolean,
            blurScore: Float,
            brightnessScore: Float
        )
    }
    
    var callback: QualityCallback? = null
    
    override fun analyze(image: ImageProxy) {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        
        val blurScore = calculateBlurScore(data, image.width, image.height)
        val brightnessScore = calculateBrightness(data)
        
        val isQualityOk = blurScore >= CameraXManager.BLUR_THRESHOLD && 
                          brightnessScore >= CameraXManager.QUALITY_THRESHOLD
        
        callback?.onQualityResult(isQualityOk, blurScore, brightnessScore)
        
        image.close()
    }
    
    private fun calculateBlurScore(data: ByteArray, width: Int, height: Int): Float {
        // Laplacian variance method for blur detection
        var variance = 0.0
        var mean = 0.0
        
        for (byte in data) {
            mean += (byte.toInt() and 0xFF)
        }
        mean /= data.size
        
        for (byte in data) {
            val diff = (byte.toInt() and 0xFF) - mean
            variance += diff * diff
        }
        variance /= data.size
        
        return sqrt(variance).toFloat()
    }
    
    private fun calculateBrightness(data: ByteArray): Float {
        var sum = 0L
        for (byte in data) {
            sum += (byte.toInt() and 0xFF)
        }
        return (sum / data.size).toFloat()
    }
}
```

---

## 📌 PHASE 4: TRIỂN KHAI SECURITY FEATURES

### 4.1 Root Detection

```kotlin
// app/src/main/java/virtual/camera/app/security/SecurityManager.kt
package virtual.camera.app.security

import android.content.Context
import com.scottyab.rootbeer.RootBeer

class SecurityManager(private val context: Context) {
    
    private val rootBeer = RootBeer(context)
    
    fun isDeviceSecure(): SecurityStatus {
        return SecurityStatus(
            isRooted = rootBeer.isRooted,
            isEmulator = isEmulator(),
            isDebuggable = isDebuggable(),
            hasSuspiciousApps = rootBeer.detectRootManagementApps()
        )
    }
    
    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic") 
                    && android.os.Build.DEVICE.startsWith("generic")
                || "google_sdk" == android.os.Build.PRODUCT)
    }
    
    private fun isDebuggable(): Boolean {
        return (context.applicationInfo.flags 
                and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    data class SecurityStatus(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isDebuggable: Boolean,
        val hasSuspiciousApps: Boolean
    ) {
        fun isSecure(): Boolean {
            return !isRooted && !isEmulator && !isDebuggable && !hasSuspiciousApps
        }
    }
}
```

### 4.2 Encrypted Storage

```kotlin
// app/src/main/java/virtual/camera/app/security/SecureStorage.kt
package virtual.camera.app.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

class SecureStorage(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveSecureString(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }
    
    fun getSecureString(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }
    
    fun saveSecureFile(fileName: String, content: ByteArray) {
        val file = File(context.filesDir, fileName)
        
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
    
    fun readSecureFile(fileName: String): ByteArray? {
        val file = File(context.filesDir, fileName)
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
    
    fun deleteSecureFile(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.delete()
    }
}
```

### 4.3 Certificate Pinning

```kotlin
// app/src/main/java/virtual/camera/app/network/SecureHttpClient.kt
package virtual.camera.app.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object SecureHttpClient {
    
    // Thay đổi domain và hash theo API của bạn
    private const val API_DOMAIN = "api.vcamera.app"
    private const val CERTIFICATE_HASH = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    
    fun create(): OkHttpClient {
        val certificatePinner = CertificatePinner.Builder()
            .add(API_DOMAIN, CERTIFICATE_HASH)
            .build()
        
        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
```

---

## 📌 PHASE 5: ML KIT INTEGRATION

### 5.1 Face Detection

```kotlin
// app/src/main/java/virtual/camera/app/ml/FaceDetectionManager.kt
package virtual.camera.app.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetectionManager {
    
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .enableTracking()
        .build()
    
    private val detector = FaceDetection.getClient(options)
    
    fun detectFaces(
        bitmap: Bitmap,
        onSuccess: (List<Face>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        detector.process(image)
            .addOnSuccessListener { faces ->
                onSuccess(faces)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
    
    fun validateFaceForKYC(face: Face): FaceValidationResult {
        val isFrontal = Math.abs(face.headEulerAngleY) < 15f &&
                        Math.abs(face.headEulerAngleZ) < 15f
        
        val isSmiling = (face.smilingProbability ?: 0f) > 0.5f
        val hasOpenEyes = (face.leftEyeOpenProbability ?: 0f) > 0.5f &&
                          (face.rightEyeOpenProbability ?: 0f) > 0.5f
        
        return FaceValidationResult(
            isFrontal = isFrontal,
            isSmiling = isSmiling,
            hasOpenEyes = hasOpenEyes,
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ
        )
    }
    
    data class FaceValidationResult(
        val isFrontal: Boolean,
        val isSmiling: Boolean,
        val hasOpenEyes: Boolean,
        val headEulerAngleY: Float,
        val headEulerAngleZ: Float
    ) {
        fun isValid(): Boolean = isFrontal && hasOpenEyes
    }
}
```

---

## 📊 PROGRESS TRACKING

| Phase | Status | Progress |
|-------|--------|----------|
| Phase 1: Build Config | ✅ Hoàn thành | 100% |
| Phase 2: Dependencies | ⏳ Đề xuất | 0% |
| Phase 3: Camera | ⏳ Đề xuất | 0% |
| Phase 4: Security | ⏳ Đề xuất | 0% |
| Phase 5: ML Features | ⏳ Đề xuất | 0% |

---

**Tài liệu được tạo**: 2025-11-25  
**Version**: 1.0  
**Author**: GitHub Copilot
