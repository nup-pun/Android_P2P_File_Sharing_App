package com.xianfeng.wjcscx.screen

import com.xianfeng.wjcscx.FileViewModel
import com.xianfeng.wjcscx.NetworkService

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun SendScreen(
    onFileSelectClick: () -> Unit,
    networkService: NetworkService,
    onDeviceSelected: (NsdServiceInfo) -> Unit,
    fileViewModel: FileViewModel
) {
    val discoveredDevices = remember { mutableStateListOf<NsdServiceInfo>() }
    val isLoading by fileViewModel.isLoading.collectAsState()
    val copyProgress by fileViewModel.copyProgress.collectAsState()
    val fileTransferCompleted by fileViewModel.fileTransferCompleted.collectAsState()

    LaunchedEffect(Unit) {
        networkService.startDiscovery { serviceInfo ->
            if (discoveredDevices.none {
                    it.serviceName == serviceInfo.serviceName
            }) {
                discoveredDevices.add(serviceInfo)
            }
        }
    }

    fun reset() {
        discoveredDevices.clear()
        fileViewModel.resetFileTransferStatus()
    }

    fun searchDevices() {
        reset()
        networkService.startDiscovery { serviceInfo ->
            if (discoveredDevices.none {
                it.serviceName == serviceInfo.serviceName
            }) {
                discoveredDevices.add(serviceInfo)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Maximum file selection: ${FileViewModel.MAX_FILES} files",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Red
        )
        Button(
            onClick = onFileSelectClick,
            modifier = Modifier.padding(end = 8.dp),
            enabled = !isLoading
        ) {
            Text(text = "Select Files")
        }
        Text(text = "Selected files: ${fileViewModel.selectedFiles.size}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { searchDevices() },
            modifier = Modifier.padding(vertical = 8.dp),
            enabled = !isLoading
        ) {
            Text(text = "Search Devices")
        }
        Text(text = "Discovered Devices", style = MaterialTheme.typography.headlineMedium)
        if (discoveredDevices.isEmpty()) {
            Text(
                text = "No device found. Make sure receiving device is in the same local network",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn {
                items(discoveredDevices.filter { it.serviceName.contains("_receiver") }) { device ->
                    Text(
                        text = device.serviceName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { onDeviceSelected(device) }
                            .padding(8.dp)
                    )
                }
            }
        }
        if (isLoading) {
            if (copyProgress in 1..99) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(progress = copyProgress / 100f, modifier = Modifier.fillMaxWidth())
                    Text(text = "Transfer        Progress: $copyProgress%", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
            }
        } else if (fileTransferCompleted) {
            Text(text = "File transfer completed", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
