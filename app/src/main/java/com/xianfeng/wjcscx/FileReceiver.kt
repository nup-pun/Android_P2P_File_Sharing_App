package com.xianfeng.wjcscx

import android.util.Log
import com.xianfeng.wjcscx.utility.ChecksumUtil
import com.xianfeng.wjcscx.utility.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket

class FileReceiver {

    private val bufferSize = 4096 //4 KB buffer
    suspend fun receiveFiles(serverSocket: ServerSocket, folder: File, fileReceivedCallback: (File) -> Unit) {
        withContext(Dispatchers.IO) {

            while (true) {
                try {
                    val socket = serverSocket.accept()
                    val input = socket.getInputStream().buffered()
                    val reader = BufferedReader(InputStreamReader(input))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                    val tempDir = File(System.getProperty("java.io.tmpdir") ?: "/tmp")
                    // Receive the encoded key from the sender
                    val encodedKey = reader.readLine()
                    val key = EncryptionUtil.decodeKey(encodedKey)

                    while (true) {
                        // Read the file name and checksum
                        val fileName = reader.readLine() ?: break
                        val expectedChecksum = reader.readLine() ?: break
                        val fileLength = reader.readLine().toLongOrNull() ?: break

                        if (fileName == "END") {
                            Log.d("FileReceiver", "Received END signal. No more files to receive.")
                            break
                        }
                        Log.d("FileReceiver", "Receiving file: $fileName with size: $fileLength checksum: $expectedChecksum")
                        val encryptedFile = File(tempDir,   "$fileName.enc")
                        encryptedFile.outputStream().use { fileOutput ->
                            val buffer = ByteArray(bufferSize)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            while (totalBytesRead < fileLength) {
                                bytesRead = input.read(buffer)
                                if (bytesRead == -1) {
                                    break
                                }
                                fileOutput.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead.toLong()
                                Log.d("FileReceiver", "Received $bytesRead bytes of $fileName with totalBytesRead: $totalBytesRead")
                            }
                            fileOutput.flush()
                            Log.d("FileReceiver", "Output is flushed")
                        }
                        Log.d("FileReceiver", "Received file data: $fileName")

                        // Decrypt the received file
                        val decryptedFile = File(folder, fileName)
                        EncryptionUtil.decryptFile(key, encryptedFile, decryptedFile)
                        Log.d("FileReceiver", "Decrypted file: $fileName")

                        val actualChecksum = ChecksumUtil.calculateChecksum(decryptedFile)
                        if (expectedChecksum != actualChecksum) {
                            Log.e("FileReceiver", "Checksum mismatch for file: $fileName")
                            writer.write("NACK\n")
                            writer.flush()
                        } else {
                            writer.write("ACK\n")
                            writer.flush()
                            fileReceivedCallback(decryptedFile)
                            Log.d("FileReceiver", "Received and acknowledged file: $fileName")
                        }
                        encryptedFile.delete() // Delete temporary encrypted file

                    }
                    writer.close()
                    reader.close()
                    input.close()
                    socket.close()
                    Log.d("FileReceiver", "All files received successfully")
                } catch (e: IOException) {
                    Log.e("FileReceiver", "Error receiving files", e)
                    break // Exit loop on error
                }
            }
        }
    }
}



