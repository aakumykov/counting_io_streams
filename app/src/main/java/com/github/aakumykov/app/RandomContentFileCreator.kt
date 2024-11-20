package com.github.aakumykov.app

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

class RandomContentFileCreator {
    companion object {

        private var remainingSize = 0
        private var bufferSize = 1024

        /**
         * Создаёт файл со случайным содержимым, получая его от класса [kotlin.random.Random]
         * методом nextByes([bufferSize]).
         *
         * @see kotlin.random.Random.nextBytes].
         *
         * @param absolutePath
         * @param sizeBytes
         * @param bufferSize
         *
         * @throws IOException
         *
         * @return Созданный файл.
         */
        @Throws(IOException::class)
        fun create(absolutePath: String,
                   sizeBytes: Int,
                   bufferSize: Int = 1024
        ): File {

            remainingSize = sizeBytes
            Companion.bufferSize = bufferSize

            return File(absolutePath).let { file ->
                FileOutputStream(file).use { fileOutputStream ->
                    Random.apply {
                        do {
                            val iterationSize = nextIterationSize()
                            fileOutputStream.write(nextBytes(iterationSize))
                            remainingSize -= bufferSize }
                        while (remainingSize > 0)
                    }
                }
                file
            }
        }

        private fun nextIterationSize(): Int {
            return if (remainingSize < bufferSize) remainingSize
            else bufferSize
        }
    }
}