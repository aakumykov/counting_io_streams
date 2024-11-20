package com.github.aakumykov.counting_io_streams

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class RandomContentFileCreatorInstrumentedTest {

    private val appContext: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val fileName = "f1.raw"
    private val dirName get() = appContext.cacheDir
    private val file: File get() = File(dirName, fileName)


    @Test
    fun whenCreateRandomContentFileThenFileExists() {
        RandomContentFileCreator.create(file.absolutePath, 11, 2)
        Assert.assertEquals(true, file.exists())
    }

    @Test
    fun when_create_non_zero_file_size_then_size_equals() {
         val fileSize = Random.nextInt(1, 512)
        RandomContentFileCreator.create(file.absolutePath, fileSize, 50)
        Assert.assertEquals(fileSize, (file.length()).toInt())
    }

    @Test
    fun when_create_zero_file_size_then_size_equals() {
        val fileSize = 0
        RandomContentFileCreator.create(file.absolutePath, fileSize, 1)
        Assert.assertEquals(fileSize, (file.length()/8).toInt())
    }
}