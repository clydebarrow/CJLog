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
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Log data to a web server. Set the property SYSLOGGER to change the destination. Data is logged using HEAD
 * calls with the message and other information in the query string.
 *
 * @param onFail A lambda to be called if connection to the destination fails.
 */
class HttpLogger(onFail: () -> Unit = {}) : Destination {
    private var messageQueue: BlockingQueue<URL> = LinkedBlockingQueue(MAX_QUEUE)
    private val syslogger = Thread {
        try {
            while (true) {
                val url = messageQueue.take()
                if(!sendLog(url)) {
                    logMsg("Send http log failed")
                    onFail()
                    break
                }
            }
        } catch (e: InterruptedException) {
        }
    }

    init {
        syslogger.start()
    }

    override fun sendMessage(deviceID: String, message: String) {
        val url: URL
        val args: String
        if (!syslogger.isAlive)
            return
        try {
            args = "?device=" + URLEncoder.encode(deviceID, CHARSETNAME) + "&" + "message=" + URLEncoder.encode(message, CHARSETNAME)
        } catch (ignored: UnsupportedEncodingException) {
            return
        }

        try {
            url = URL(SYSLOGGER + args)
        } catch (ignored: MalformedURLException) {
            return
        }

        messageQueue.offer(url)
    }

    override fun close() {
        syslogger.interrupt()
    }

    /**
     * Actually send the log to the destination [url].
     */
    protected fun sendLog(url: URL): Boolean {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            return 200 <= responseCode && responseCode <= 399
        } catch (e: Exception) {
            return false
        }

    }

    companion object {
        /**
         * The destination to use.
         */
        var SYSLOGGER = "http://192.168.1.131/syslog.php"
        private val MAX_QUEUE = 100
        private val CHARSETNAME = "UTF-8"
        private val READ_TIMEOUT = 20000
        private val CONNECTION_TIMEOUT = 10000
    }

}
