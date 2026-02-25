package com.trasparenza.app.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/**
 * Camera controller for managing CameraX
 */
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    
    /**
     * Bind camera to preview view
     */
    fun bindCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            
            // Image capture use case
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(85)
                .build()
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Capture image and return as JPEG bytes
     */
    fun captureImage(onImageCaptured: (ByteArray) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: run {
            onError("Camera not initialized")
            return
        }
        
        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bytes = imageProxyToJpegBytes(image)
                    image.close()
                    onImageCaptured(bytes)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "Capture failed")
                }
            }
        )
    }
    
    private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        // Decode and rotate if needed
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
        
        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        
        return outputStream.toByteArray()
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}

/**
 * Composable camera preview
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onControllerReady: (CameraController) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraController(context, lifecycleOwner) }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                controller.bindCamera(previewView)
                onControllerReady(controller)
            }
        },
        modifier = modifier
    )
}

