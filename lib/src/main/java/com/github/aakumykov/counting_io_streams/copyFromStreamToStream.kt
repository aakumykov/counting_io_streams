package com.github.aakumykov.counting_io_streams

import java.io.IOException

@Throws(IOException::class)
fun copyFromStreamToStream(
    countingInputStream: CountingInputStream,
    countingOutputStream: CountingOutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    readingCallback: CountingInputStream.ReadingCallback? = null,
    writingCallback: CountingOutputStream.WritingCallback? = null,
) {
    val dataBuffer = ByteArray(bufferSize)
    var readBytes: Int
    var totalBytes: Long = 0

    readBytes = countingInputStream.read(readingCallback)

    while (readBytes != -1) {
        totalBytes += readBytes
        countingOutputStream.write(dataBuffer, 0, readBytes, writingCallback)
        readBytes = countingInputStream.read(dataBuffer)
    }
}