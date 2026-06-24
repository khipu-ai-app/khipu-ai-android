package pe.khipuai.app.ui.components.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.Executor

/**
 * T-05: Composable que muestra el preview de la cámara con CameraX.
 *
 * El [CameraCaptureController] encapsula el ciclo de vida del
 * [ProcessCameraProvider] (bind/unbind) y los use cases (Preview + ImageCapture).
 * El Composable solo se preocupa del layout.
 */
@Composable
fun CameraPreview(
    controller: CameraCaptureController,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                controller.bindPreviewTo(this, lifecycleOwner)
            }
        }
    )
}

/**
 * T-05: controlador del ciclo de vida de CameraX. Maneja:
 *  - bind/unbind del [ProcessCameraProvider]
 *  - selección de lente (frontal/trasera)
 *  - flash (off/on/auto)
 *  - captura de foto a archivo
 *
 * Se construye con [rememberCameraCaptureController]. Es un remember-aware:
 * el Composable se encarga del ciclo de vida del controller.
 */
class CameraCaptureController internal constructor(
    private val context: Context,
    private val mainExecutor: Executor
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var currentPreviewView: PreviewView? = null
    private var currentLifecycleOwner: LifecycleOwner? = null

    private val _cameraState: MutableState<CameraState> =
        mutableStateOf(CameraState.Initializing)
    val cameraState: State<CameraState> = _cameraState

    internal fun bindPreviewTo(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        currentPreviewView = previewView
        currentLifecycleOwner = lifecycleOwner
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()
                startCamera(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    lensFacing = CameraSelector.LENS_FACING_BACK
                )
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e.message ?: "Error iniciando cámara")
            }
        }, mainExecutor)
    }

    fun switchLens() {
        val previewView = currentPreviewView ?: return
        val lifecycleOwner = currentLifecycleOwner ?: return
        val current = _cameraState.value
        if (current !is CameraState.Ready) return
        val newLens = if (current.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera(previewView, lifecycleOwner, newLens)
    }

    fun setFlashMode(flashMode: Int) {
        imageCapture?.flashMode = flashMode
    }

    fun captureToFile(
        outputFile: File,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("ImageCapture no está listo"))
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            options,
            mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    onSuccess(outputFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        currentPreviewView = null
        currentLifecycleOwner = null
    }

    private fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        lensFacing: Int
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val captureUseCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            val cam = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                previewUseCase,
                captureUseCase
            )
            preview = previewUseCase
            imageCapture = captureUseCase
            _cameraState.value = CameraState.Ready(
                lensFacing = lensFacing,
                hasFlash = cam.cameraInfo.hasFlashUnit()
            )
        } catch (e: Exception) {
            _cameraState.value = CameraState.Error(e.message ?: "Error bind cámara")
        }
    }
}

sealed class CameraState {
    data object Initializing : CameraState()
    data class Ready(val lensFacing: Int, val hasFlash: Boolean) : CameraState()
    data class Error(val message: String) : CameraState()
}

@Composable
fun rememberCameraCaptureController(): CameraCaptureController {
    val context = LocalContext.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val controller = remember { CameraCaptureController(context, mainExecutor) }
    DisposableEffect(Unit) {
        onDispose { controller.shutdown() }
    }
    return controller
}
