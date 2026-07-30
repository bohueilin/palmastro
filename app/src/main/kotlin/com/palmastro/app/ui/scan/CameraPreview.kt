package com.palmastro.app.ui.scan

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.palmastro.app.CrashReporting

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        // Camera init is async; the nav entry can be popped before the provider is ready.
        // Both flags are read/written on the main executor only, so no synchronization.
        var disposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            if (disposed || lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                return@addListener
            }
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                )
            } catch (e: Exception) {
                // Binding raced lifecycle teardown (back pressed during camera init);
                // there is nothing left to show, so record instead of crashing.
                CrashReporting.recordException(e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            val cameraProviderFuture2 = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture2.addListener({
                cameraProviderFuture2.get().unbindAll()
            }, ContextCompat.getMainExecutor(context))
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
