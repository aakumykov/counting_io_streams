package com.github.aakumykov.counting_io_streams

import com.github.aakumykov.counting_io_streams.counting_buffered_streams.CountingBufferedInputStream
import java.io.InputStream

class CancelableBufferedInputStream(
    inputStream: InputStream,
    bufferSize: Int = 8192,
    private val cancellationMarker: CancellationMarker,
    readingCallback: ReadingCallback,
)
    : CountingBufferedInputStream(inputStream, bufferSize, readingCallback)
{
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        if (cancellationMarker.isCancelled)
            close()
        return super.read(b, off, len)
    }

    interface CancellationMarker {
        var isCancelled: Boolean
    }
}