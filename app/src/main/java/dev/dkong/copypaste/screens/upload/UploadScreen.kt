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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.beust.klaxon.JsonObject
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
import dev.dkong.copypaste.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class UploadStatus(val step: Int) {
    NotSelected(0),
    Selected(1),
    Uploading(2),
    Processing(3),
    Complete(4)
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

    val rootUrl = "http://192.168.1.188:5000"
    var videoUri: Uri? by remember { mutableStateOf(null) }
    var uploadStatus: UploadStatus by remember { mutableStateOf(UploadStatus.NotSelected) }
    var statusUrl: String? by remember { mutableStateOf(null) }
    var processingResult: String? by remember { mutableStateOf(null) }
    var isFailed: Boolean by remember { mutableStateOf(false) }

    fun process(statusUrl: String) {
        scope.launch {
            while (uploadStatus.step < UploadStatus.Complete.step && !isFailed) {
                Fuel.get(statusUrl)
                    .response { _, _, result ->
                        val (responseBytes, _) = result
                        if (responseBytes == null) {
                            Log.d("SERVICE_RESPONSE", "Response Bytes is null.")
                            return@response
                            // TODO: Make a user-facing error message
                        }
                        val jsonResponse = Utils.convertToJsonObject(String(responseBytes))
                        Log.d("PROCESSING CHECK", jsonResponse.string("state").toString())
                        if (jsonResponse.string("state") == "SUCCESS") {
                            uploadStatus = UploadStatus.Complete
                            processingResult = jsonResponse.array<JsonObject>("result").toString()
                            Log.d("PROCESSING RESULT", processingResult.toString())
                        } else if (jsonResponse.string("state") == "FAILURE") {
                            isFailed = true
                        }
                    }
                delay(3000)
            }
        }
    }

    fun upload(videoUri: Uri, context: Context): String? {
        isFailed = false

        val videoFile = fileFromContentUri(context, videoUri)
        Log.d("UPLOAD", videoFile.toString())
        Fuel.upload(rootUrl, method = Method.POST)
            .add(FileDataPart(videoFile, name = "file"))
            .response { _, _, result ->
                val (responseBytes, _) = result
                if (responseBytes == null) {
                    Log.d("SERVICE_RESPONSE", "Response Bytes is null.")
                    return@response
                    // TODO: Make a user-facing error message
                }
                val jsonResponse = Utils.convertToJsonObject(String(responseBytes))
                if (jsonResponse.int("status") == 202) {
                    Log.d("JSON RESPONSE", "Successful")
                    val location = jsonResponse.string("location")
                    uploadStatus = UploadStatus.Processing
                    statusUrl = rootUrl + location
                    Log.d("JSON RESPONSE", "Status URL: $statusUrl")
                    statusUrl?.let {
                        process(it)
                    }
                }
            }
        return null
    }


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
            AnimatedVisibility(
                visible = uploadStatus.step < UploadStatus.Uploading.step || isFailed,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    SectionHeading(heading = "Video selection", includeHorizontalPadding = false)
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
        item {
            AnimatedVisibility(
                visible = uploadStatus.step >= UploadStatus.Uploading.step,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                SectionHeading(heading = "Progress", includeHorizontalPadding = false)
            }
        }
        item {
            ProgressItem(
                name = "Upload video",
                isVisible = uploadStatus.step >= UploadStatus.Uploading.step,
                isDone = uploadStatus.step > UploadStatus.Uploading.step
            )
        }
        item {
            ProgressItem(
                name = "Processing",
                isVisible = uploadStatus.step >= UploadStatus.Processing.step,
                isDone = uploadStatus.step > UploadStatus.Processing.step,
                isFailed = isFailed
            )
        }
        item {
            ProgressItem(
                name = "Complete",
                isVisible = uploadStatus.step == UploadStatus.Complete.step,
                isDone = uploadStatus.step == UploadStatus.Complete.step
            )
        }
        item {
            processingResult?.let {
                Text(text = it)
            }
        }
        item {
            SectionHeading(heading = "Customisation", includeHorizontalPadding = false)
        }
        item {
            Text(text = "Give your action a name!")
        }
    }
}

@Composable
fun ProgressItem(name: String, isVisible: Boolean, isDone: Boolean, isFailed: Boolean = false) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyLarge)
                if (isFailed)
                    Text(text = "Failed", color = MaterialTheme.colorScheme.error)
            }
            if (!isFailed) {
                Box {
                    if (isDone) {
                        Checkbox(checked = true, onCheckedChange = {})
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
