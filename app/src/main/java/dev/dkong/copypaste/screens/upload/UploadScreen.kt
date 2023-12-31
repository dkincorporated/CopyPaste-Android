package dev.dkong.copypaste.screens.upload

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
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
import dev.dkong.copypaste.objects.Position
import dev.dkong.copypaste.objects.Sequence
import dev.dkong.copypaste.utils.ActionManager
import dev.dkong.copypaste.utils.ConnectionManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import dev.dkong.copypaste.utils.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
    val context = LocalContext.current

    val rootUrl = "http://${ConnectionManager.serverAddress}:${ConnectionManager.serverPort}"
    var videoUri: Uri? by remember { mutableStateOf(null) }
    var uploadStatus: UploadStatus by remember { mutableStateOf(UploadStatus.NotSelected) }
    var statusUrl: String? by remember { mutableStateOf(null) }
    var processingResult: String? by remember { mutableStateOf(null) }
    var isFailed: Boolean by remember { mutableStateOf(false) }
    var failure: String? by remember { mutableStateOf(null) }
    var actionName: String by remember { mutableStateOf("") }
    var parsedSequence: Sequence? = null

    fun process(statusUrl: String) {
        scope.launch {
            Log.d("STATUS URL", statusUrl)
            var failedFetches = 0
            while (uploadStatus.step < UploadStatus.Complete.step && !isFailed && failedFetches < 10) {
                Fuel.get(statusUrl)
                    .response { _, response, _ ->
                        try {
                            Log.d("Upload", response.data.toString(Charsets.UTF_8))
                            val parsedResponse =
                                Json.decodeFromString<Sequence>(response.data.toString(Charsets.UTF_8))
                            Log.d("PROCESSING", parsedResponse.toString())
                            if (parsedResponse.state?.equals("SUCCESS") == true) {
                                uploadStatus = UploadStatus.Complete
                                parsedSequence = parsedResponse
                            } else if (parsedResponse.state?.equals("FAILURE") == true) {
                                isFailed = true
                                failure = parsedResponse.status
                                uploadStatus = UploadStatus.Selected
                            }
                        } catch (_: IOException) {
                            failedFetches += 1
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
                    isFailed = true
                    failure = "Upload server provided no response."
                    uploadStatus = UploadStatus.Selected
                    Log.d("SERVICE_RESPONSE", "Response Bytes is null.")
                    return@response
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

    var videoWidth by remember { mutableStateOf(0) }
    var videoHeight by remember { mutableStateOf(0) }

    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            videoUri = uri
            uploadStatus = if (uri == null) UploadStatus.NotSelected else UploadStatus.Selected
            isFailed = false
            if (uri == null) return@rememberLauncherForActivityResult
            val mediaRetriever = MediaMetadataRetriever()
            mediaRetriever.setDataSource(context, uri)
            videoWidth = mediaRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
            )?.toInt() ?: 0
            videoHeight = mediaRetriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
            )?.toInt() ?: 0
            mediaRetriever.release()
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
                SectionHeading(heading = "Video selection", includeHorizontalPadding = false)
            }
        }
        item {
            val videoPainter = rememberAsyncImagePainter(
                model = videoUri,
                imageLoader = ImageLoader.Builder(LocalContext.current)
                    .components {
                        add(VideoFrameDecoder.Factory())
                    }
                    .build(),
            )
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .size(256.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(256.dp)
                ) {
                    if (videoUri == null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    launcher.launch(
                                        PickVisualMediaRequest(
                                            mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                                        )
                                    )
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(text = "Choose")
                            }
                        }
                    } else {
                        Image(
                            painter = videoPainter,
                            contentDescription = "Selected video",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(256.dp)
                                .padding(8.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
        item {
            if (isFailed) {
                Text(
                    text = "Video could not be processed. $failure",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            AnimatedVisibility(
                visible = uploadStatus == UploadStatus.Selected,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            launcher.launch(
                                PickVisualMediaRequest(
                                    mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                                )
                            )
                        },
                        enabled = uploadStatus == UploadStatus.Selected
                    ) {
                        Text(text = "Choose again")
                    }
                    Button(
                        onClick = {
                            videoUri?.let {
                                upload(it, context)
                            }
                            uploadStatus = UploadStatus.Uploading
                        },
                        enabled = uploadStatus == UploadStatus.NotSelected || uploadStatus == UploadStatus.Selected
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeading(heading = "Progress", includeHorizontalPadding = false)
                    ProgressItem(
                        name = "Upload video",
                        isVisible = uploadStatus.step >= UploadStatus.Uploading.step,
                        isDone = uploadStatus.step > UploadStatus.Uploading.step
                    )
                    ProgressItem(
                        name = "Processing",
                        isVisible = uploadStatus.step >= UploadStatus.Processing.step,
                        isDone = uploadStatus.step > UploadStatus.Processing.step,
                        isFailed = isFailed
                    )
                    ProgressItem(
                        name = "Complete",
                        isVisible = uploadStatus.step == UploadStatus.Complete.step,
                        isDone = uploadStatus.step == UploadStatus.Complete.step
                    )
                    processingResult?.let {
                        Text(text = it)
                    }
                }
            }
        }
        item {
            AnimatedVisibility(
                visible = uploadStatus.step >= UploadStatus.Selected.step,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SectionHeading(heading = "Customisation", includeHorizontalPadding = false)
                    val isError = uploadStatus == UploadStatus.Complete && actionName == ""
                    OutlinedTextField(
                        value = actionName,
                        onValueChange = { name ->
                            actionName = name
                        },
                        label = { Text("Action name") },
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        isError = isError,
                        supportingText = {
                            AnimatedVisibility(
                                visible = isError,
                                enter = expandVertically(),
                                exit = shrinkVertically()
                            ) {
                                Text("Please name the action before saving.")
                            }
                        }
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        16.dp,
                        Alignment.CenterHorizontally
                    ), modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            isFailed = true
                            failure = "Cancelled by user."
                            uploadStatus = UploadStatus.Selected
                        },
                        enabled = uploadStatus == UploadStatus.Uploading || uploadStatus == UploadStatus.Processing
                    ) {
                        Text(text = "Cancel")
                    }
                    Button(
                        onClick = {
                            if (actionName == "") return@Button
                            parsedSequence?.let { seq ->
                                seq.name = actionName
                                seq.creationTime = System.currentTimeMillis() / 1000
                                seq.id = System.currentTimeMillis() / 1000
                                seq.dimensions =
                                    Position(videoWidth.toFloat(), videoHeight.toFloat())
                                scope.launch {
                                    ActionManager.addSequence(context, seq)
                                    Log.d("SAVE", "Saved action $actionName!")
                                    navHostController.navigateUp()
                                }
                            }
                        },
                        enabled = uploadStatus == UploadStatus.Complete && actionName != ""
                    ) {
                        Text(text = "Save")
                    }
                }
            }
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
            if (!isFailed && isVisible) {
                Box(
                    modifier = Modifier.size(36.dp)
                ) {
                    this@Row.AnimatedVisibility(
                        visible = isDone,
                        enter = scaleIn(),
                        exit = scaleOut()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = Icons.Default.Check.name,
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!isDone) CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
