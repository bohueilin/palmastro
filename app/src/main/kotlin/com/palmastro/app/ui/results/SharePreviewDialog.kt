package com.palmastro.app.ui.results

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.palmastro.app.R
import com.palmastro.app.share.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Preview → chooser handoff, shared by Results and Domain Detail. The PNG encode and the
 * cache write are blocking work, so they run off the main thread, and the preview stays
 * on screen until the file exists rather than blinking out mid-encode.
 */
@Composable
fun ShareFlowDialog(
    bitmap: Bitmap,
    shareText: String,
    chooserTitle: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SharePreviewDialog(
        bitmap = bitmap,
        onDismiss = onDismiss,
        onConfirm = {
            scope.launch {
                val uri = withContext(Dispatchers.IO) { ShareHelper.writeShareFile(context, bitmap) }
                // Chooser first, dismiss second: dismissing removes this dialog — and the
                // scope this coroutine runs in — from composition.
                ShareHelper.startChooser(context, uri, shareText, chooserTitle)
                onDismiss()
            }
        },
    )
}

/**
 * Preview-first share flow (PRD 13.7): the rendered card is shown to the user before
 * anything reaches the system share sheet.
 */
@Composable
fun SharePreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_preview_title), fontWeight = FontWeight.SemiBold) },
        text = {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.share_preview_image_desc),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                contentScale = ContentScale.FillWidth,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.share_preview_share), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
