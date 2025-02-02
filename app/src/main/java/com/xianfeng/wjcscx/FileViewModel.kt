package com.xianfeng.wjcscx

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.Socket

data class FileWithThumbnail(val file: File, val thumbnail: Bitmap?)

class FileViewModel(application: Application, private val networkService: NetworkService) : AndroidViewModel(application) {
    private val _fvmReady = MutableStateFlow(false)
    val fvmReady = _fvmReady.asStateFlow()

    companion object {
        const val MAX_FILES = 30  // Maximum number of files to be selected
    }

    private val folderName = "MyFolder"

    var receivingStatus by mutableStateOf("Waiting for sender...")
        private set
    var selectedFiles by mutableStateOf<List<Uri>>(emptyList())
        private set
    var uniqueServiceName by mutableStateOf<String?>(null)
        private set

    fun updateSelectedFiles(files: List<Uri>) {
        selectedFiles = files
    }

    fun resetFileTransferStatus() {
        _isLoading.value = false
        _fileTransferCompleted.value = false
        receivingStatus = "Waiting for the sender.."
    }

    private val _copyProgress = MutableStateFlow(0)
    val copyProgress: StateFlow<Int> get() = _copyProgress

    private fun setCopyProg(progress: Int) {
        _copyProgress.value = progress
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _fileTransferCompleted = MutableStateFlow(false)
    val fileTransferCompleted: StateFlow<Boolean> get() = _fileTransferCompleted

    private val _filesWithTN = MutableStateFlow<List<FileWithThumbnail>>(emptyList())
    val filesWithTN: StateFlow<List<FileWithThumbnail>> = _filesWithTN

    private val fileSender = FileSender()
    private val fileReceiver = FileReceiver()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            createFolder()
            loadFiles()
            _fvmReady.value = true
        }
    }

    private fun createFolder() {
        val folder = File(getApplication<Application>().getExternalFilesDir(null), folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    private fun getFolder(): File {
        val folder = File(getApplication<Application>().getExternalFilesDir(null), folderName)
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    private fun loadFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = getFolder()
            val filesList = folder.listFiles()?.map { file ->
                val thumbnail = generateThumbnail(file)
                FileWithThumbnail(file, thumbnail)
            } ?: emptyList()
            _filesWithTN.value = filesList
        }
    }

    private fun generateThumbnail(file: File): Bitmap? {
        return try {
            when (file.extension.lowercase()) {
                "jpg", "jpeg", "png", "gif" -> BitmapFactory.decodeFile(file.absolutePath)
                    ?.let { bitmap ->
                        Bitmap.createScaledBitmap(bitmap, 100, 100, false)
                    }

                "mp4", "avi" -> ContextCompat.getDrawable(getApplication(), R.drawable.ic_video)
                    ?.toBitmap()

                "mp3", "m4a" -> ContextCompat.getDrawable(getApplication(), R.drawable.ic_music)
                    ?.toBitmap()

                "txt", "pdf", "doc" -> ContextCompat.getDrawable(
                    getApplication(),
                    R.drawable.ic_document
                )?.toBitmap()

                else -> ContextCompat.getDrawable(getApplication(), R.drawable.ic_file)?.toBitmap()
            }
        } catch (e: Exception) {
            Log.e("FileViewModel", "Error generating thumbnail", e)
            null
        }
    }



    private fun receiveFile() {
        val folder = getFolder()
        viewModelScope.launch(Dispatchers.IO) {
            fileReceiver.receiveFiles(networkService.serverSocket!!, folder) { file ->
                viewModelScope.launch(Dispatchers.Main) {
                    receivingStatus = "File received: ${file.name}"
                    loadFiles() // Update the list of files after receiving a new file
                }
            }
        }

    }

    fun startReceiving() {
        receivingStatus = "Waiting for sender.."
        val name = "receiver_${System.currentTimeMillis()}"
        uniqueServiceName = name
        networkService.registerService(name)
        Log.d("FileViewModel", "Service registered with name: $name")
        receiveFile()
    }

    fun sendFiles(socket: Socket) {
        _isLoading.value = true
        _fileTransferCompleted.value = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = selectedFiles.map { copyFileToCache(it) }
                Log.d("FileViewModel", "Sending files: ${files.map { it.name }}")
                fileSender.sendFile(socket, files) { progress ->
                    setCopyProg(progress)
                }
            } catch (e: IOException) {
                Log.e("FileViewModel", "Error sending files", e)
            } finally {
                withContext(Dispatchers.IO) {
                    _isLoading.value = false
                    _fileTransferCompleted.value = true
                    try {
                        socket.close()
                    } catch (e: IOException) {
                        Log.e("FileViewModel", "Error closing socket", e)
                    }
                }
            }
        }
    }

    private fun copyFileToCache(uri: Uri): File {
        val contentResolver = getApplication<Application>().contentResolver
        val file = File(getApplication<Application>().cacheDir, getFileName(uri, contentResolver))
        contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndexOrThrow("_display_name") ?: -1
        cursor?.moveToFirst()
        val name = if (nameIndex != -1) cursor?.getString(nameIndex) else uri.lastPathSegment
        cursor?.close()
        return name ?: "default_name"
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension
        return when (extension.lowercase()) {
            "jpg", "jpeg", "png", "gif" -> "image/*"
            "mp4", "avi" -> "video/*"
            "mp3", "m4a" -> "audio/*"
            "txt", "pdf", "doc" -> "application/*"
            else -> "*/*"
        }
    }

    fun openFile(file: File, context: Context) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.applicationContext.packageName}.provider",
                file
            )
            val mimeType = context.contentResolver.getType(uri) ?: getMimeType(file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Log.e("FileViewModel", "Error opening file", e)
        }
    }

    fun deleteAllFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val folder = getFolder()
                folder.listFiles()?.forEach { it.delete() }
                loadFiles()
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Log.i("DeleteFile", "Error deleting files: ${e.message}")
                }
            }
        }
    }
}