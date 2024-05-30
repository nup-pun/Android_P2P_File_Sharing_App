package com.xianfeng.wjcscx

import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.xianfeng.wjcscx.screen.AppNavigationScreen
import com.xianfeng.wjcscx.ui.theme.WjcscxTheme

class MainActivity : ComponentActivity() {
    private val networkService by lazy { NetworkService(this) }
    private val fileViewModel: FileViewModel by viewModels {
        FileViewModelFactory(application, networkService)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                !fileViewModel.fvmReady.value
            }
        }
        setContent {
            WjcscxTheme {
                AppNavigationScreen(
                    fileViewModel = fileViewModel,
                    networkService = networkService,
                    onDeviceSelected = ::onDeviceSelected,
                    selectFilesLauncher = { selectFilesLauncher.launch(arrayOf("*/*")) }
                )
            }
        }
    }

    private val selectFilesLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.size > FileViewModel.MAX_FILES) {
            Toast.makeText(this, "You can select up to ${FileViewModel.MAX_FILES} files only.", Toast.LENGTH_SHORT).show()
        } else {
            fileViewModel.updateSelectedFiles(uris)
        }
    }

    private fun onDeviceSelected(serviceInfo: NsdServiceInfo) {
        if (fileViewModel.selectedFiles.isEmpty()) {
            Toast.makeText(this, "You have to select a file first", Toast.LENGTH_SHORT).show()
        } else {
            try {
                // Initiate connection and transfer files
                networkService.connectToService(serviceInfo) { socket ->
                    if (socket != null) {
                        fileViewModel.sendFiles(socket)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "Failed to connect to service", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkService.stopService()
    }
}


