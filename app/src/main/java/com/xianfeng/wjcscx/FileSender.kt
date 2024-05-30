package com.xianfeng.wjcscx

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class FileSender {

    private val bufferSize = 1024 // Use 1 KB buffer
    private val maxRetries = 3 // Adjustable number of retries

    suspend fun sendFile(socket: Socket, files: List<File>, progressCallback: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val output = socket.getOutputStream().bufferedWriter()
            val input = socket.getInputStream().bufferedReader()

            for (file in files) {
                var retryCount = 0
                var fileSent = false

                while (retryCount < maxRetries && !fileSent) {
                    try {
                        val checksum = ChecksumUtil.calculateChecksum(file)
                        Log.d("FileSender", "Calculated checksum for ${file.name}: $checksum")
                        // Send file name, checksum in a single write
                        output.write("${file.name}\n$checksum\n")
                        output.flush()
                        Log.d("FileSender", "Sent file name and checksum: ${file.name}")
                        // Send file data with progress update
                        file.inputStream().use { fileInput ->
                            var totalBytesRead = 0
                            val buffer = ByteArray(bufferSize)
                            var bytesRead: Int
                            while (fileInput.read(buffer).also { bytesRead = it } != -1) {
                                socket.getOutputStream().write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                val progress = (totalBytesRead / file.length().toFloat() * 100).toInt()
                                progressCallback(progress)
                                Log.d("FileSender", "Sent $bytesRead bytes of ${file.name}")
                            }
                            socket.getOutputStream().flush()
                        }
                        Log.d("FileSender", "Sent file data: ${file.name}")
                        // Read acknowledgment
                        val response = input.readLine()
                        if (response == "ACK") {
                            Log.d("FileSender", "File sent successfully: ${file.name}")
                            fileSent = true
                        } else {
                            Log.w("FileSender", "Acknowledgment not received for file: ${file.name}. Retrying (${retryCount + 1}/$maxRetries)")
                            retryCount++
                        }
                    } catch (e: IOException) {
                        Log.e("FileSender", "Error sending file", e)
                        retryCount++
                    }
                }

                if (!fileSent) {
                    Log.e("FileSender", "Failed to send file after $maxRetries retries: ${file.name}")
                    break
                }
            }

            // Send END to indicate all files have been sent
            output.write("END\n")
            output.flush()
            Log.d("FileSender", "All files sent successfully")
        }
    }
}


