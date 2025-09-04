package com.example.a3dgs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.a3dgs.ui.theme._3DGSTheme
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import android.net.Uri
import android.content.Intent
import android.provider.DocumentsContract
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import com.example.a3dgs.ply.PlyLoader
import com.example.a3dgs.ply.PointCloud
import com.example.a3dgs.remote.RemoteProcessing
import com.example.a3dgs.viewer.FilamentPointCloudView
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _3DGSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun App(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf("picker") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    var pointCloud by remember { mutableStateOf<PointCloud?>(null) }
    val scope = rememberCoroutineScope()

    val openImages = rememberLauncherForOpenMultiple { uris ->
        selectedUris = uris
        status = "Selected ${uris.size} items"
    }
    val openFolder = rememberLauncherForOpenDirectory { uri ->
        if (uri != null) {
            val images = enumerateImagesInFolder(uri)
            selectedUris = images
            status = "Selected folder with ${images.size} images"
        }
    }
    val openPly = rememberLauncherForOpenPly { uri ->
        if (uri != null) {
            val temp = File(context.cacheDir, "picked_${System.currentTimeMillis()}.ply")
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { out -> input.copyTo(out) }
            }
            pointCloud = PlyLoader.loadAsciiPly(temp)
            screen = "viewer"
        }
    }

    when (screen) {
        "picker" -> PickerScreen(
            modifier = modifier,
            status = status,
            onPickImages = { openImages.launch(arrayOf("image/*")) },
            onPickFolder = { openFolder.launch(null) },
            onPickPly = { openPly.launch(arrayOf("*/*")) },
            onProcess = {
                if (selectedUris.isEmpty()) {
                    status = "Please select images first"
                    return@PickerScreen
                }
                screen = "processing"
                status = "Uploading and processing..."
                scope.launch {
                    val baseUrl = "http://YOUR_GPU_SERVER/" // TODO replace with your server
                    val remote = RemoteProcessing(baseUrl)
                    val dest = File(context.cacheDir, "result_${System.currentTimeMillis()}.ply")
                    val res = remote.runGaussianSplatting(context.contentResolver, selectedUris, dest)
                    if (res.isSuccess) {
                        pointCloud = PlyLoader.loadAsciiPly(res.getOrThrow())
                        screen = "viewer"
                    } else {
                        status = "Failed: ${res.exceptionOrNull()?.message}"
                        screen = "picker"
                    }
                }
            }
        )
        "processing" -> ProcessingScreen(modifier = modifier, status = status)
        "viewer" -> ViewerScreen(modifier = modifier, pointCloud = pointCloud) {
            screen = "picker"
            status = ""
            selectedUris = emptyList()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PickerPreview() {
    _3DGSTheme {
        App()
    }
}

@Composable
private fun PickerScreen(
    modifier: Modifier = Modifier,
    status: String,
    onPickImages: () -> Unit,
    onPickFolder: () -> Unit,
    onPickPly: () -> Unit,
    onProcess: () -> Unit,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("3DGS SDK")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onPickImages) { Text("Pick images") }
            Button(onClick = onPickFolder) { Text("Pick folder") }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onProcess) { Text("Process on server") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPickPly) { Text("Open .ply file") }
        Spacer(Modifier.height(8.dp))
        if (status.isNotBlank()) Text(status)
    }
}

@Composable
private fun ProcessingScreen(modifier: Modifier = Modifier, status: String) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Processing...")
        Spacer(Modifier.height(8.dp))
        Text(status)
    }
}

@Composable
private fun ViewerScreen(modifier: Modifier = Modifier, pointCloud: PointCloud?, onBack: () -> Unit) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Back") }
            Text("Viewer")
        }
        Spacer(Modifier.height(8.dp))
        val pc = pointCloud
        if (pc == null) {
            Text("No point cloud loaded")
        } else {
            androidx.compose.ui.viewinterop.AndroidView(factory = { ctx ->
                FilamentPointCloudView(ctx).apply { setPointCloud(pc) }
            })
        }
    }
}

@Composable
private fun rememberLauncherForOpenMultiple(onResult: (List<Uri>) -> Unit): ActivityResultLauncher<Array<String>> {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    return remember {
        activity.registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
            onResult(uris)
        }
    }
}

@Composable
private fun rememberLauncherForOpenDirectory(onResult: (Uri?) -> Unit): ActivityResultLauncher<Uri?> {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    return remember {
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                try { activity.contentResolver.takePersistableUriPermission(uri, flags) } catch (_: SecurityException) {}
            }
            onResult(uri)
        }
    }
}

@Composable
private fun rememberLauncherForOpenPly(onResult: (Uri?) -> Unit): ActivityResultLauncher<Array<String>> {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    return remember {
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            onResult(uri)
        }
    }
}

private fun enumerateImagesInFolder(treeUri: Uri): List<Uri> {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val doc = DocumentFile.fromTreeUri(ctx, treeUri) ?: return emptyList()
    return doc.listFiles()
        .filter { it.isFile && (it.type?.startsWith("image/") == true) }
        .mapNotNull { it.uri }
}