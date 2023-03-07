/*
 * Copyright (c) 2019-2020 Control-J Pty. Ltd. All rights reserved
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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * A logging [Destination] that saves to the supplied [logFile]. On each class creation if the current logfile
 * is greater in size than a limit, the previous logfiles
 * will be rotated, up to a maximum of [maxFiles]. If the created file is "app.log" then older files will be
 * rotated to "app.log.1" etc.
 * Writing to files is carried out on a dedicated [Thread]
 *
 * @param logFile The file in which logs are to be saved
 */
class FileLogger(val logFile: File) : Destination {
    private val messageQueue: BlockingQueue<String> = LinkedBlockingQueue(100)
    private lateinit var logThread: Thread

    /**
     * The maximum number of rotated log files to keep
     */
    var maxFiles = 2

    /**
     * The maximum length of a log file before it is rotated.
     */
    var maxLength = 1_000_000

    init {
        logThread = Thread {
            try {
                logMsg("Starting file logger, logfile is ${logFile.name}")
                if (!logFile.parentFile.isDirectory)
                    logFile.parentFile.mkdirs()
                if (logFile.exists() && logFile.length() > maxLength) {
                    // rotate old log files so we can recover them
                    val file = previous(maxFiles)
                    if (file.exists())
                        file.delete()
                    for (i: Int in maxFiles - 1 downTo 1) {
                        val f = previous(i)
                        if (f.exists()) f.renameTo(previous(i + 1))
                    }
                    logFile.renameTo(previous(1))
                }
                val logWriter: OutputStreamWriter
                logWriter = FileWriter(logFile, true)
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
        get() = (0 .. maxFiles).map { previous(it) }.filter { it.exists() }.toList()

    override fun close() {
        logThread.interrupt()
    }

    private fun previous(idx: Int): File {
        if (idx == 0) return logFile
        return File(logFile.parentFile, logFile.name + "." + idx)
    }
}
