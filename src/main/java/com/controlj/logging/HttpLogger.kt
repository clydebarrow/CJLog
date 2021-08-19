/*
 * Copyright (c) 2019-2021 Control-J Pty. Ltd. All rights reserved
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
import java.net.HttpURLConnection
import java.net.URL

/**
 * Log data to a web server. Set the property SYSLOGGER to change the destination. Data is logged using HEAD
 * calls with the message and other information in the query string.
 * The transmission of data to the remote server is carried out on a dedicated thread.
 *
 * @param onFail A lambda to be called if connection to the destination fails.
 */
class HttpLogger(val onFail: () -> Unit = {}, val postUrl: String = SYSLOGGER) : NetworkLogger() {

    /**
     * Actually send the log to the destination [url].
     */
    override fun sendLog(data: Message): Boolean {
        try {
            val bytes = data.bytes
            val connection = URL(postUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-type", "text/plain; charset=utf-8")
            connection.setRequestProperty("Content-length", bytes.size.toString())
            connection.setRequestProperty("X-DeviceId", data.deviceId)
            connection.outputStream.use {
                it.write(bytes)
            }
            val responseCode = connection.responseCode
            if(responseCode !in (200..399)) {
                connection.errorStream.close()
                System.err.println("Failed to send data: $responseCode")
                logMsg("Failed to send data: $responseCode")
                return false
            }
            connection.inputStream.close()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun onFailure(message: Message) {
        onFail()
    }

    companion object {
        /**
         * The host to use
         */
        var SYSLOGGER = "http://192.168.1.131/syslog.php"

        /**
         * The maximum number of messages to queue
         */
        var MAX_QUEUE = 100

        /**
         * How long to wait for a response from the server
         */
        var READ_TIMEOUT = 20000

        /**
         * How long to wait for a connection to the server
         */
        var CONNECTION_TIMEOUT = 10000

        /**
         * Parameter keys
         */


        /**
         * Set if a successful connection is made. Used by others to check if we are running in a local debug
         * environment
         */
        var isConnected = false
            private set
    }
}
