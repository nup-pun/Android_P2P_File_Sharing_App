package com.xianfeng.wjcscx

import android.util.Log
import com.xianfeng.wjcscx.utility.ChecksumUtil
import com.xianfeng.wjcscx.utility.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.net.Socket

class FileSender {

    private val bufferSize = 4096 //4 KB buffer
    private val maxRetries = 3 // Adjustable number of retries
    suspend fun sendFile(socket: Socket, files: List<File>, progressCallback: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val output = socket.getOutputStream().bufferedWriter()
            val input = socket.getInputStream().bufferedReader()

            val tempDir = File(System.getProperty("java.io.tmpdir") ?: "/tmp")
            val key = EncryptionUtil.generateKey()
            val encodedKey = EncryptionUtil.encodeKey(key)
            output.write(encodedKey)
            output.flush()

            for (file in files) {
                var retryCount = 0
                val encryptedFile = File(tempDir, file.name + ".enc")
                EncryptionUtil.encryptFile(key, file, encryptedFile)
                Log.d("FileSender", "Encrypted file: ${encryptedFile.name}")

                val fileLength = encryptedFile.length()
                var fileSent = false

                while (retryCount < maxRetries && !fileSent) {
                    try {
                        val checksum = ChecksumUtil.calculateChecksum(file)
                        Log.d("FileSender", "Calculated checksum for ${file.name}: $checksum")

                        // Send file name, checksum, file size in a single write
                        output.write("${file.name}\n$checksum\n$fileLength\n")
                        output.flush()
                        Log.d("FileSender", "Sent file name and checksum with size($fileLength): ${file.name}")
                        // Encrypt and send file data with progress update
                        delay(25)
                        encryptedFile.inputStream().use { fileInput ->
                            var totalBytesRead = 0L
                            val buffer = ByteArray(bufferSize)
                            var bytesRead: Int
                            while (fileInput.read(buffer).also { bytesRead = it } != -1) {
                                socket.getOutputStream().write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                val progress = (totalBytesRead / fileLength.toFloat() * 100).toInt()
                                progressCallback(progress)
                                Log.d("FileSender", "Sent $bytesRead bytes of $totalBytesRead ${file.name}")
                                delay(25)
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

                encryptedFile.delete() // Delete temporary encrypted file
            }
            // Send END to indicate all files have been sent
            output.write("END\n")
            output.flush()
            Log.d("FileSender", "All files sent successfully")

            output.close()
            input.close()
        }
    }
}


