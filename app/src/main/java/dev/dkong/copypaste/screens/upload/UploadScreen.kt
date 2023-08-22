package dev.dkong.copypaste.screens.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.navigation.NavHostController
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.VideoFrameDecoder
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.DataPart
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.core.Method
import dev.dkong.copypaste.composables.LargeTopAppbarScaffold
import dev.dkong.copypaste.composables.SectionHeading
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

enum class UploadStatus {
    NotSelected,
    Selected,
    Uploading,
    Processing,
    Complete
}

fun upload(videoUri: Uri, context: Context) {
    val videoFile = fileFromContentUri(context, videoUri)
    Log.d("UPLOAD", videoFile.toString())
    Fuel.upload("http://192.168.1.188:5000", method = Method.POST)
        .add(FileDataPart(videoFile, name = "file"))
        .response { result ->
            Log.d("UPLOAD FILE", result.toString())
        }

}

// From https://stackoverflow.com/a/64488260

fun fileFromContentUri(context: Context, contentUri: Uri): File {
    // Preparing Temp file name
    val fileExtension = getFileExtension(context, contentUri)
    val fileName = "temp_file" + if (fileExtension != null) ".$fileExtension" else ""

    // Creating Temp file
    val tempFile = File(context.cacheDir, fileName)
    tempFile.createNewFile()

    try {
        val oStream = FileOutputStream(tempFile)
        val inputStream = context.contentResolver.openInputStream(contentUri)

        inputStream?.let {
            copy(inputStream, oStream)
        }

        oStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return tempFile
}

private fun getFileExtension(context: Context, uri: Uri): String? {
    val fileType: String? = context.contentResolver.getType(uri)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(fileType)
}

@Throws(IOException::class)
private fun copy(source: InputStream, target: OutputStream) {
    val buf = ByteArray(8192)
    var length: Int
    while (source.read(buf).also { length = it } > 0) {
        target.write(buf, 0, length)
    }
}

// End from

@Composable
fun UploadScreen(navHostController: NavHostController) {
    val scope = rememberCoroutineScope()

    var videoUri: Uri? by remember { mutableStateOf(null) }
    var uploadStatus: UploadStatus by remember { mutableStateOf(UploadStatus.NotSelected) }

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            videoUri = uri
            uploadStatus = if (uri == null) UploadStatus.NotSelected else UploadStatus.Selected
        }

    LargeTopAppbarScaffold(
        navController = navHostController,
        title = "Upload video",
        horizontalPadding = 16.dp
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                SectionHeading(heading = "Select the video", includeHorizontalPadding = false)
                FilledTonalButton(
                    onClick = {
                        launcher.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        )
                    }
                ) {
                    Text(text = "Choose")
                }
            }
        }
//        item {
//            videoUri?.let {
//                Text(
//                    text = videoUri.toString()
//                )
//            }
//        }
        item {
            val videoPainter = rememberAsyncImagePainter(
                model = videoUri,
                imageLoader = ImageLoader.Builder(LocalContext.current)
                    .components {
                        add(VideoFrameDecoder.Factory())
                    }
                    .build(),
            )
            AnimatedVisibility(
                visible = videoUri != null,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = videoPainter,
                        contentDescription = "Selected video",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(256.dp)
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                }
            }
        }
        item {
            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.Selected,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            videoUri?.let {
                                upload(it, context)
                            }
                            uploadStatus = UploadStatus.Uploading
                        },
                        enabled = uploadStatus == UploadStatus.NotSelected || uploadStatus == UploadStatus.Selected,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Upload")
                    }
                }
            }
        }
        val progress = 0.5f
        item {
            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.Uploading,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    SectionHeading(heading = "Upload video", includeHorizontalPadding = false)
                    LinearProgressIndicator(
                        progress = progress,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.Processing,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    SectionHeading(heading = "Processing", includeHorizontalPadding = false)
                    LinearProgressIndicator(
                        progress = progress,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

