package com.github.aakumykov.app

import java.io.OutputStream

class CountingOutputStream(
    private val outputStream: OutputStream,
    private val callbackTriggeringIntervalBytes: Long = 8192,
    private val callback: WritingCallback,
) : OutputStream() {

    private var writtenBytesCount: Long = 0
    private var callbackTriggeringBytesCounter: Long = 0

    override fun write(b: Int) {
        outputStream.write(b).also {
            summarizeAndCallBack(1)
        }
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
            callback.onWriteCountChanged(writtenBytesCount)
            callbackTriggeringBytesCounter = 0
        }
    }

    fun interface WritingCallback {
        fun onWriteCountChanged(count: Long)
    }
}