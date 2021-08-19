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

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

abstract class NetworkLogger : Destination {
    var messageQueue: BlockingQueue<Message> = LinkedBlockingQueue(HttpLogger.MAX_QUEUE)
    val syslogger = Thread {
        try {
            while (true) {
                val message = messageQueue.take()
                if (!sendLog(message)) {
                    CJLog.logMsg("Send http log failed")
                    onFailure(message)
                    break
                }
            }
        } catch (e: InterruptedException) {
        }
    }

    open class Message(val deviceId: String, val message: String) {
        open val bytes: ByteArray
            get() = message.toByteArray()
    }

    val isAlive: Boolean get() = syslogger.isAlive

    init {
        syslogger.start()
    }

    fun sendMessage(message: Message) {
        if (!syslogger.isAlive)
            return
        messageQueue.offer(message)

    }

    override fun sendMessage(deviceID: String, message: String) {
        sendMessage(Message(deviceId = deviceID, message = message))
    }

    override fun close() {
        syslogger.interrupt()
    }

    open fun onFailure(message: Message) {
    }

    /**
     * Actually send the log to the destination [url].
     */
    abstract fun sendLog(data: Message): Boolean
}
