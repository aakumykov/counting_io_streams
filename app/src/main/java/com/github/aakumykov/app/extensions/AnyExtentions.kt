package com.github.aakumykov.app.extensions

import android.util.Log

fun Any.LogD(message: String) {
    val tag = tag()
    Log.d(tag, message)
}

fun Any.tag(): String = this.javaClass.simpleName