package com.github.aakumykov.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.InputStream

class FlowableInputStream(
    private val inputStream: InputStream,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : InputStream() {

    init {
        if (inputStream is FlowableInputStream)
            throw IllegalArgumentException("Cannot create FlowableInputStream from FlowableInputStream")
    }

    private var readBytesCount: Long = 0

    private val _progressMutableSharedFlow: MutableSharedFlow<Long> = MutableSharedFlow()
    val transferredBytesFlow: SharedFlow<Long> = _progressMutableSharedFlow


    override fun read(): Int {
        return inputStream.read().let { justReadByte ->
            readBytesCount += 1

            CoroutineScope(coroutineDispatcher).launch {
                _progressMutableSharedFlow.emit(readBytesCount)
            }

            justReadByte
        }
    }
}