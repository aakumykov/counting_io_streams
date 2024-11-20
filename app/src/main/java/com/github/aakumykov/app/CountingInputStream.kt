package com.github.aakumykov.app

import java.io.IOException
import java.io.InputStream
import kotlin.jvm.Throws

class CountingInputStream(
    private val inputStream: InputStream,
    private val callbackTriggeringIntervalBytes: Long = 8192L,
    private val readingCallback: ReadingCallback,
) : InputStream() {

    private var readedBytesCount: Long = 0
    private var callbackTriggeringBytesCounter: Long = 0

    @Throws(IOException::class)
    override fun read(): Int {
        return inputStream.read().let { justReadByte ->
            summarizeAndCallBack(justReadByte)
            justReadByte
        }
    }

    override fun close() {
        super.close()
        inputStream.close()
    }

    private fun summarizeAndCallBack(justReadByte: Int) {

        if (-1 == justReadByte) {
            invokeCallback()
            return
        }

        readedBytesCount += 1
        callbackTriggeringBytesCounter += 1

        val isCallbackThresholdExceed = callbackTriggeringBytesCounter >= callbackTriggeringIntervalBytes

        if (isCallbackThresholdExceed) {
            invokeCallback()
            callbackTriggeringBytesCounter = 0
        }
    }

    private fun invokeCallback() {
        readingCallback.onReadCountChanged(readedBytesCount)
    }


    fun interface ReadingCallback {
        fun onReadCountChanged(count: Long)
    }
}