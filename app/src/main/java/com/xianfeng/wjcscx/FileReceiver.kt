package com.xianfeng.wjcscx

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket

class FileReceiver {

    private val bufferSize = 1024 // Use 1 KB buffer

    suspend fun receiveFiles(serverSocket: ServerSocket, folder: File, fileReceivedCallback: (File) -> Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                try {
                    val socket = serverSocket.accept()
                    val input = socket.getInputStream().buffered()
                    val reader = BufferedReader(InputStreamReader(input))
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))

                    while (true) {
                        // Read the file name and checksum
                        val fileName = reader.readLine() ?: break
                        val expectedChecksum = reader.readLine()

                        if (fileName == "END") {
                            break
                        }
                        Log.d("FileReceiver", "Receiving file: $fileName with checksum: $expectedChecksum")

                        val file = File(folder, fileName)
                        file.outputStream().use { output ->
                            val buffer = ByteArray(bufferSize)
                            var totalBytesRead = 0L
                            var bytesRead: Int
                            while (true) {
                                bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                Log.d("FileReceiver", "Received $bytesRead bytes of $fileName")
                            }
                            Log.d("FileReceiver", "Before Output is flushed")
                            output.flush()
                            Log.d("FileReceiver", "Output is flushed")
                        }
                        Log.d("FileReceiver", "Received file data: $fileName")

                        val actualChecksum = ChecksumUtil.calculateChecksum(file)
                        if (expectedChecksum != actualChecksum) {
                            Log.e("FileReceiver", "Checksum mismatch for file: $fileName")
                            writer.write("NACK\n")
                            writer.flush()
                        } else {
                            writer.write("ACK\n")
                            writer.flush()
                            fileReceivedCallback(file)
                        }
                    }

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



