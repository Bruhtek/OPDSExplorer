package com.bruhtek.opdsexplorer.filesystem

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.bruhtek.opdsexplorer.ui.theme.OPDSExplorerTheme
import kotlinx.coroutines.launch

class OOBEFilesystemActivity : ComponentActivity() {
    private lateinit var documentTreeRepository: DocumentTreeRepository

    private val documentTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { treeUri ->
            handleDocumentTreeResult(treeUri)
        } ?: run {
            // User cancelled the picker
            Log.d("DocumentTree", "User cancelled document tree selection")
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the filesystem or perform any setup needed

        documentTreeRepository = DocumentTreeRepository.getInstance(this)

        setContent {
            OPDSExplorerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(it)
                            .padding(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text("Please select a directory to save your downloaded OPDS books to.")
                            Button(
                                colors = ButtonColors(
                                    containerColor = Color(0xFF000000),
                                    contentColor = Color(0xFFFFFFFF),
                                    disabledContainerColor = Color(0xFFD0D0D0),
                                    disabledContentColor = Color(0xFFA0A0A0)
                                ),
                                onClick = {
                                    openDocumentTree()
                                }
                            ) {
                                Text("Select Directory")
                            }
                        }
                    }
                }
            }

        }

        lifecycleScope.launch {
            documentTreeRepository.treeUriFlow.collect { uriString ->
                if (uriString == null || !documentTreeRepository.hasValidPermissions(
                        uriString.toUri()
                    )
                ) {
                    documentTreeRepository.clearTreeUri()
                } else {
                    useDocumentTree(uriString.toUri())
                }
            }
        }
    }

    private fun openDocumentTree() {
        val initialUri = Environment.getExternalStorageDirectory().toUri()
        documentTreeLauncher.launch(initialUri)
    }

    private fun handleDocumentTreeResult(treeUri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            contentResolver.takePersistableUriPermission(treeUri, flags)

            lifecycleScope.launch {
                documentTreeRepository.saveTreeUri(treeUri)
                useDocumentTree(treeUri)
            }

        } catch (e: SecurityException) {
            Log.e("OOBEFilesystem", "Failed to persist URI permission", e)
        }
    }

    private fun useDocumentTree(treeUri: Uri) {
        val documentFile = DocumentFile.fromTreeUri(this, treeUri)

        documentFile?.let { rootDir ->
            if (rootDir.isDirectory) {
                Log.d("OOBEFilesystem", "Using document tree: $treeUri")
            }

            finish()
        }
    }
}