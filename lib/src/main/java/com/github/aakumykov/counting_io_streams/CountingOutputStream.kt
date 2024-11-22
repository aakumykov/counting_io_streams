package com.github.aakumykov.counting_io_streams

import java.io.IOException
import java.io.OutputStream

class CountingOutputStream(
    private val outputStream: OutputStream,
    private val callbackTriggeringIntervalBytes: Long = 8192,
    private var callback: WritingCallback?,
) : OutputStream() {

    private var writtenBytesCount: Long = 0
    private var callbackTriggeringBytesCounter: Long = 0


    override fun write(b: Int) {
        outputStream.write(b).also {
            summarizeAndCallBack(1)
        }
    }


    @Throws(IOException::class)
    fun write(b: ByteArray, offset: Int, len: Int, callback: WritingCallback? = null) {
        this.callback = callback
        outputStream.write(b, offset, len)
    }


    override fun close() {
        super.close()
        outputStream.close()
    }


    private fun summarizeAndCallBack(count: Int) {
        writtenBytesCount += count
        callbackTriggeringBytesCounter += count

        val isCallbackThresholdExceed = callbackTriggeringBytesCounter >= callbackTriggeringIntervalBytes

        if (isCallbackThresholdExceed || count < 0) {
            callback?.onWriteCountChanged(writtenBytesCount)
            callbackTriggeringBytesCounter = 0
        }
    }

    fun interface WritingCallback {
        fun onWriteCountChanged(count: Long)
    }
}