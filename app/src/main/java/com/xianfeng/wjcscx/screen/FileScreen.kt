package com.xianfeng.wjcscx.screen

import com.xianfeng.wjcscx.FileViewModel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun FilesScreen(
    fileViewModel: FileViewModel,
    onFileClick: (File) -> Unit,
    onBackClick: () -> Unit
) {
    val files by fileViewModel.filesWithTN.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = "Received Files",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 128.dp), modifier = Modifier.weight(1f)) {
            items(files.size) { index ->
                val fileWithThumbnail = files[index]
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable { onFileClick(fileWithThumbnail.file) }
                ) {
                    fileWithThumbnail.thumbnail?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp)
                        )
                    }

                    Text(
                        text = fileWithThumbnail.file.name,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row {
            Button(
                onClick = { onBackClick() },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Back")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { fileViewModel.deleteAllFiles() },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Clear all files")
            }
        }
    }
}