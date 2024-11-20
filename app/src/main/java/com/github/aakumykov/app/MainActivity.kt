package com.github.aakumykov.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.provider.Settings
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.aakumykov.file_lister_navigator_selector.extensions.listenForFragmentResult
import com.github.aakumykov.file_lister_navigator_selector.file_lister.SimpleSortingMode
import com.github.aakumykov.file_lister_navigator_selector.file_selector.FileSelector
import com.github.aakumykov.app.databinding.ActivityMainBinding
import com.github.aakumykov.app.extensions.showToast
import com.github.aakumykov.app.extensions.tag
import com.github.aakumykov.app.utils.Logger
import com.github.aakumykov.local_file_lister_navigator_selector.local_file_selector.LocalFileSelector
import com.github.aakumykov.storage_access_helper.StorageAccessHelper
import com.gitlab.aakumykov.exception_utils_module.ExceptionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private var cancellationMarker: com.github.aakumykov.counting_io_streams.CancelableBufferedInputStream.CancellationMarker? = null

    private var workingJob: Job? = null
    private var workingInputStream: InputStream? = null
    private var workingOutStream: OutputStream? = null

    private var copyingJob: Job? = null

    private lateinit var binding: ActivityMainBinding

    private val downloadsDir: File get() = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)

    private var selectedFile: File? = null
    private val selectedFileStream: FileInputStream get() = FileInputStream(selectedFile)

    private val targetFileName = "/dev/null"
    private val targetFile = File(targetFileName)
    private val outputStream: OutputStream get() = targetFile.outputStream()

    private val readingBufferSize: Int get() = binding.inputBufferSizeSeekBar.progress
    private val copyDelay: Int get() = binding.copyDelaySeekBar.progress

    private lateinit var storageAccessHelper: StorageAccessHelper
    private lateinit var fileSelector: FileSelector<SimpleSortingMode>

    private var isBusy: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        Logger.messages.observe(this) { messages -> binding.logView.text = messages.joinToString("\n") }
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prepareStorageAccessHelper()
        prepareFileSelector()
        prepareBufferSizeSeekBar()
        prepareDelaySeekBar()
        prepareButtons()

        displayReadingBufferSize(readingBufferSize)
        displayCopyDelay(copyDelay)
    }

    private fun prepareDelaySeekBar() {
        binding.copyDelaySeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) displayCopyDelay(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun displayCopyDelay(progress: Int) {
        binding.copyDelayTextView.text = "Замедление копирования: $progress мс"
    }

    private fun prepareButtons() {
        binding.selectFileButton.setOnClickListener {
            storageAccessHelper.requestFullAccess {
                fileSelector.show(supportFragmentManager, LocalFileSelector.tag())
            }
        }

        binding.copyFileButton.setOnClickListener { copyFileWithUnbufferedStream() }
        binding.copyFileWithBufferButton.setOnClickListener { copyFileWithBufferedStream() }

        binding.stopButton.setOnClickListener { onStopCopyingClicked() }

        binding.copyFileWithBufferWithCoroutineButton.setOnClickListener { startCopyWithCoroutine() }
        binding.stopCopyFileWithBufferWithCoroutineButton.setOnClickListener { stopCopyWithCoroutine() }

        binding.copyWithCancelableStreamButton.setOnClickListener { copyWithCancelableStream() }
        binding.stopCopyWithCancelableStreamButton.setOnClickListener { stopCopyWithCancelableStream() }
    }


    private fun copyWithCancelableStream(){

        cancellationMarker = object: com.github.aakumykov.counting_io_streams.CancelableBufferedInputStream.CancellationMarker {
            private var _isCancelled: Boolean = false
            override var isCancelled: Boolean
                get() = _isCancelled
                set(value) { _isCancelled = value }
        }

        val cancelableInputStream =
            com.github.aakumykov.counting_io_streams.CancelableBufferedInputStream(
                inputStream = selectedFileStream,
                cancellationMarker = cancellationMarker!!
            ) { bytesReaded ->
                displayProgress(selectedFile!!.length(), bytesReaded)
            }

        thread {
            try {
                cancelableInputStream.copyTo(outputStream)
            }
            catch (e: Exception) {
                ExceptionUtils.getErrorMessage(e).also { errorMsg ->
                    Logger.d(TAG, errorMsg)
                    Log.e(TAG, errorMsg, e)
                }
            }
        }
    }

    private fun stopCopyWithCancelableStream() {
        cancellationMarker?.apply {
            isCancelled = true
        } ?: {
            showToast("Копирование не запущено")
        }
    }


    private fun startCopyWithCoroutine() {
        copyingJob = lifecycleScope.launch (Dispatchers.IO) {
            try {
                val countingInputStream =
                    com.github.aakumykov.counting_io_streams.counting_buffered_streams.CoroutineScopedBufferedInputStream(
                        selectedFileStream,
                        this
                    ) { count ->
                        displayProgress(selectedFile!!.length(), count)
//                    Logger.d(TAG, "прочитано: $count / ${selectedFile!!.length()}")
                    }
                countingInputStream.copyTo(outputStream)
            }
            catch (e: Exception) {
                selectedFileStream.close()
                ExceptionUtils.getErrorMessage(e).also { errorMsg ->
                    Logger.d(TAG, errorMsg)
                    Log.e(TAG, errorMsg, e)
                }
            }
            finally {
                copyingJob = null
            }
        }
    }

    private fun stopCopyWithCoroutine() {
        copyingJob?.also {
            lifecycleScope.launch (Dispatchers.IO) {
                it.cancelAndJoin()
            }
        } ?: {
            showToast("Копирование неактивно")
        }
    }


    private fun onStopCopyingClicked() {
        workingJob?.also {
            it.cancel(CancellationException("Прервано пользователем"))
        }
//        workingInputStream?.close()
//        workingOutStream?.close()
    }

    private fun prepareBufferSizeSeekBar() {
        binding.inputBufferSizeSeekBar.setOnSeekBarChangeListener(object: SeekBar. OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    displayReadingBufferSize(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun displayReadingBufferSize(progress: Int) {
        binding.bufferSizeTextView.text = "Размер буфера чтения: ${android.text.format.Formatter.formatFileSize(this@MainActivity, progress.toLong())}"
    }

    private fun prepareStorageAccessHelper() {
        storageAccessHelper = StorageAccessHelper.create(this).apply {
            prepareForFullAccess()
        }
    }

    private fun copyFileWithUnbufferedStream() {

        if (isNotReady()) return

        val countingInputStream =
            com.github.aakumykov.counting_io_streams.CountingInputStream(selectedFileStream) { count ->
                displayProgress(selectedFile!!.length(), count)
                TimeUnit.MILLISECONDS.sleep(copyDelay.toLong())
            }

        copyFromStreamToStream(countingInputStream, outputStream)
    }


    private fun copyFileWithBufferedStream() {

        if (isNotReady()) return

        val countingInputStream =
            com.github.aakumykov.counting_io_streams.counting_buffered_streams.CountingBufferedInputStream(
                inputStream = selectedFileStream,
                bufferSize = readingBufferSize
            ) { count ->
                displayProgress(selectedFile!!.length(), count)
                TimeUnit.MILLISECONDS.sleep(copyDelay.toLong())
            }

        copyFromStreamToStream(countingInputStream, outputStream)
    }


    private fun isNotReady(): Boolean {
        if (null == selectedFile) {
            showToast("Выберите файл")
            return true
        }

        if (isBusy) {
            showToast("Занят")
            return true
        }

        return false
    }


    private fun prepareFileSelector() {
        fileSelector = LocalFileSelector.create(
            fragmentResultKey = KEY_FILE_SELECTION,
            initialPath = downloadsDir.absolutePath,
            isMultipleSelectionMode = false
        )

        listenForFragmentResult(KEY_FILE_SELECTION) { requestKey, result ->
            FileSelector.extractSelectionResult(result)?.also { list ->
                selectedFile = File(list.first().absolutePath)
                selectedFile?.also {
                    binding.selectFileButton.text = "Выбран файл ${it.name}"
                    binding.copyFileButton.text = "Копировать в $targetFileName без буфера"
                    binding.copyFileWithBufferButton.text = "Копировать в $targetFileName c буфером"
                    binding.fileSizeTextView.text = "Размер файла ${fileSize(it.length())}"
                }
            }
        }
    }


    private fun copyFromStreamToStream(inputStream: InputStream, outputStream: OutputStream) {

        workingJob = lifecycleScope.launch(Dispatchers.IO) {

            inMainThread { isBusy = true }

            try {
                workingInputStream = inputStream
                workingOutStream = outputStream

                inputStream.copyTo(outputStream)

            }
            catch (e: CancellationException) {
                workingInputStream?.close()
                workingOutStream?.close()
                ExceptionUtils.getErrorMessage(e).also {
                    log(it)
                    showToast(it)
                }
            }
            catch (e: IOException) {
                log("IOException")
            }
            finally {
                workingInputStream?.close()
                workingOutStream?.close()
            }

            inMainThread {
                isBusy = false
                showToast("Копирование завершено")
            }
        }
    }

    private fun displayProgress(totalBytes: Long, readBytesCount: Long) {
        binding.root.post {
            val fraction = 1f * readBytesCount / totalBytes
            val progress = (fraction.round(2) * 100).toInt()

            inMainThread {
                binding.progressBar.progress = progress
                binding.progressText.text = fraction.round(4).toString()
            }
        }
    }


    private fun inMainThread(block: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.Main) {
            block.invoke()
        }
    }

    private suspend fun inIoThread(block: suspend () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            block.invoke()
        }
    }


    private fun showAppProperties() {
        val uri = Uri.parse("package:$packageName")
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
        if (intent.resolveActivity(packageManager) != null) { startActivity(intent) }
    }

    private fun fileSize(size: Long):String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun log(text: String) {
        Logger.d(TAG, text)
    }

    companion object {
        val TAG: String = MainActivity::class.java.simpleName
        const val KEY_FILE_SELECTION = "KEY_FILE_SELECTION"
        const val COPYING_STEP_DELAY: Long = 500
    }


}

fun Float.round(decimalDigitsAfterComma: Int): Float {
    return if (0 == decimalDigitsAfterComma) { Math.round(this) * 1f }
    else (10f.pow(decimalDigitsAfterComma)).let { n: Float -> Math.round(this * n) / n }
}

// android.text.format.Formatter.formatShortFileSize(this, progress.toLong())