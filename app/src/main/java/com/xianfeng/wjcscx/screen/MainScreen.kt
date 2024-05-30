package com.xianfeng.wjcscx.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onSendClick: () -> Unit,
    onReceiveClick: () -> Unit,
    onReceivedFilesClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LAN File Transfer") }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onSendClick, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "Send")
            }
            Button(onClick = onReceiveClick, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(text = "Receive")
            }
            Button(onClick = onReceivedFilesClick) {
                Text(text = "Received Files")
            }
        }
    }
}
