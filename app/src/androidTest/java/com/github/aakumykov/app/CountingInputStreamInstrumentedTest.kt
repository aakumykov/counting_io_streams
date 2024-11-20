package com.github.aakumykov.app

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CountingInputStreamInstrumentedTest {

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val randomFileName = "randomFile.bin"
    private val randomFilePath: String get() = File(context.cacheDir, randomFileName).absolutePath
    private val randomFile: File get() = File(randomFilePath)
    private val isRandomFilePrepared: Boolean get() = randomFile.let { it.exists() && it.length() > 0 }

    private val inputStream: InputStream get() = randomFile.inputStream()
    private val outputStream: OutputStream = File("/dev/null").outputStream()


    //
    // Тесты вспомогательных функций
    //
    @Test
    fun when_prepare_random_file_then_file_exists() {
        prepareRandomFile()
        assertTrue(randomFile.exists())
    }


    @Test
    fun when_prepare_random_file_then_its_size_greats_than_zero() {
        prepareRandomFile()
        randomFile.length().also {
            Log.d(TAG, "random file size: $it")
            assertTrue(it > 0)
        }
    }


    @Test
    fun when_prepare_random_file_then_size_equals_requested() {
        arrayOf(
            0,
            1,
            2,
            3,
            4,
            5,
        ).forEach { size ->
            prepareRandomFile(size)
            assertEquals(size, randomFile.length().toInt())
        }
    }


    //
    // Тесты главных функций
    //

    @Test
    fun when_copy_zero_size_file_then_callback_calls_only_ones_with_minus_one_argument() {
        prepareRandomFile(0)
        val callback = Mockito.mock(com.github.aakumykov.app.CountingInputStream.ReadingCallback::class.java)
        val countingInputStream =
            com.github.aakumykov.app.CountingInputStream(inputStream, 8192L, callback)
        countingInputStream.copyTo(outputStream)
        verify(callback, Mockito.only()).onReadCountChanged(0)
    }


    @Test
    fun when_copy_file_with_size_equals_buffer_size_then_callback_triggers_once_and_returns_that_size() {
        val size = 8192L
        prepareRandomFile(size.toInt())
        val callback = Mockito.mock(com.github.aakumykov.app.CountingInputStream.ReadingCallback::class.java)
        val countingInputStream =
            com.github.aakumykov.app.CountingInputStream(inputStream, size, callback)
        countingInputStream.copyTo(outputStream)
        verify(callback, Mockito.atLeast(1)).onReadCountChanged(size)
    }


    @Test
    fun when_copy_file_with_double_buffer_size_then_callback_triggers_once_and_returns_that_size() {
        val size = 8192L * 2
        prepareRandomFile(size.toInt())
        val callback = Mockito.mock(com.github.aakumykov.app.CountingInputStream.ReadingCallback::class.java)
        val countingInputStream =
            com.github.aakumykov.app.CountingInputStream(inputStream, size, callback)
        countingInputStream.copyTo(outputStream)
        verify(callback, Mockito.atLeast(2)).onReadCountChanged(size)
    }


    @Test
    fun dev_null() {

    }




    private fun prepareRandomFile(size: Int = Random.nextInt(1,1563)) {
        com.github.aakumykov.app.RandomContentFileCreator.create(randomFilePath, size)
    }


    companion object {
        val TAG: String = CountingInputStreamInstrumentedTest::class.java.simpleName
    }
}