package com.github.aakumykov.app.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object Logger {

    private val messageList: MutableList<String> = mutableListOf()
    private val _messageListMutableLiveData: MutableLiveData<List<String>> = MutableLiveData()

    val messages get(): LiveData<List<String>> = _messageListMutableLiveData

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        messageList.add("$tag: $message")
        postNewList()
    }

    fun clear() {
        messageList.clear()
        postNewList()
    }


    private fun postNewList() {
        _messageListMutableLiveData.postValue(messageList)
    }
}