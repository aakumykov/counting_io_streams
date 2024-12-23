package com.github.aakumykov.counting_io_streams

import com.github.aakumykov.counting_io_streams.counting_buffered_streams.CountingBufferedInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

open class DelayedInputStream(
    inputStream: InputStream,
    private val delayMs: Long = 10L,
    readingCallback: ReadingCallback,
)
    : CountingBufferedInputStream(
        inputStream = inputStream,
        readingCallback = readingCallback
    )
{
    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        return super.read(b, off, len).apply {
            TimeUnit.MILLISECONDS.sleep(delayMs)
        }
    }
}