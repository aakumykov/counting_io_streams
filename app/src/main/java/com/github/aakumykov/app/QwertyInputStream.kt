package com.github.aakumykov.app

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class QwertyInputStream(
    private val inputStream: InputStream,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
)
    : InputStream()
{
    init {
        if (inputStream is QwertyInputStream)
            throw IllegalArgumentException("Cannot create $TAG using $TAG")
    }

    private var readBytesCount: Long = 0
    private val _progressMutableSharedFlow: MutableSharedFlow<Long> = MutableSharedFlow()
    val transferredBytesFlow: SharedFlow<Long> = _progressMutableSharedFlow


    override fun read(): Int {
        Log.d(TAG, "read() called")

        readBytesCount += 1

        CoroutineScope(coroutineDispatcher).launch {
            _progressMutableSharedFlow.emit(readBytesCount)
        }

        return inputStream.read()
    }

    companion object {
        val TAG: String = QwertyInputStream::class.java.simpleName
    }
}