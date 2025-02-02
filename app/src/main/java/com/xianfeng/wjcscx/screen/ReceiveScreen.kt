package com.xianfeng.wjcscx.screen


import android.util.Log
import com.xianfeng.wjcscx.FileViewModel
import com.xianfeng.wjcscx.NetworkService
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ReceiveScreen(
    networkService: NetworkService,
    fileViewModel: FileViewModel = viewModel()
) {
    val receivingStatus by fileViewModel::receivingStatus
    val uniqueServiceName by fileViewModel::uniqueServiceName
    val lifecycleOwner = LocalLifecycleOwner.current
//    LaunchedEffect(Unit) {
//        Log.d("ReceiveScreen", "LaunchedEffect triggered")
//        fileViewModel.startReceiving()
//    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Register the service when the screen is resumed
                    fileViewModel.startReceiving()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Unregister the service when the screen is paused
                    networkService.stopService()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Ensure the service is unregistered when the screen is disposed
            fileViewModel.resetFileTransferStatus()
            networkService.stopService()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uniqueServiceName?.let {
            Text(text = "Your service name: $it", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = receivingStatus, style = MaterialTheme.typography.headlineMedium)
    }
}

