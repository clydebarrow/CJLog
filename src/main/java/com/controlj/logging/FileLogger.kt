/*
 * Copyright (c) 2019 Control-J Pty. Ltd. All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.controlj.logging

import com.controlj.logging.CJLog.logMsg
import java.io.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A logging [Destination] that saves to the supplied [logFile]
 *
 * @param logFile The file in which logs are to be saved
 */
class FileLogger(val logFile: File) : Destination {
    private val messageQueue: BlockingQueue<String> = LinkedBlockingQueue(MAX_QUEUE)
    private lateinit var logThread: Thread

    init {
        logThread = Thread {
            try {
                logMsg("Starting file logger, logfile is %s", logFile.name)
                if(!logFile.parentFile.isDirectory)
                    logFile.parentFile.mkdirs()
                if (logFile.exists()) {
                    // rotate old log files so we can recover them
                    val file = previous(MAX_FILES)
                    if (file.exists())
                        file.delete()
                    for (i: Int in MAX_FILES - 1 downTo 1) {
                        val f = previous(i)
                        if (f.exists()) {
                            val r = previous(i + 1)
                            f.renameTo(r)
                        }
                    }
                    logFile.renameTo(previous(1))
                }
                val logWriter: OutputStreamWriter
                logWriter = FileWriter(logFile, false)
                while (!logThread.isInterrupted) {
                    val message = messageQueue.take()
                    logWriter.write(message)
                    if (!message.endsWith("\n"))
                        logWriter.write("\n")
                    if (messageQueue.isEmpty())
                        logWriter.flush()
                }
                logWriter.close()
            } catch (ignored: Exception) {
            }
        }
        logThread.start()
    }

    override fun sendMessage(deviceID: String, message: String) {
        if (logThread.isAlive)
            messageQueue.offer(message)
    }

    override fun retrieveMessages(limit: Int): String {
        try {
            val fr = FileReader(logFile)
            if (logFile.length() > limit)
                fr.skip(logFile.length() - limit)
            val bufferedReader = BufferedReader(fr)
            val sb = StringBuilder()
            var s: String
            while (bufferedReader.ready()) {
                s = bufferedReader.readLine()
                if (s == null)
                    break
                sb.append(s)
                sb.append('\n')
            }
            fr.close()
            return sb.toString()
        } catch (e: IOException) {
            return e.toString()
        }
    }

    override val previousLogfiles: List<File>
            get() = (0 until MAX_FILES).map { previous(it) }.filter { it.exists() }.toList()

    override fun close() {
        logThread.interrupt()
    }

    private fun previous(idx: Int): File {
        if (idx == 0) return logFile
        return File(logFile.parentFile, logFile.name + "." + idx)
    }

    companion object {
        private const val MAX_QUEUE = 100
        /**
         * The maximum number of logfiles to be rotated and preserved.
         */
        var MAX_FILES = 4     // maximum number of files to keep
    }
}
