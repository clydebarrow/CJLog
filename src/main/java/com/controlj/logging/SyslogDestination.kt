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

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Send log messages to a UDP Syslog destination.
 * @param host The destination host, defaults to localhost
 * @param port The destination port, defaults to 514.
 */
class SyslogDestination(host: String = "localhost", private val port: Int = 514) : NetworkLogger() {

    private val datagramSocket = DatagramSocket()

    // lazy init these so that it occurs on a background thread.
    private val serverAddress: InetAddress by lazy { InetAddress.getByName(host) }
    private val clientName: String by lazy { InetAddress.getLocalHost().hostName }
    private val executor = Executors.newSingleThreadExecutor()

    override fun sendLog(data: Message): Boolean {
        executor.submit {
            try {
                val bytes = data.bytes
                val dgram = DatagramPacket(bytes, bytes.size, serverAddress, port)
                datagramSocket.send(dgram)
            } catch (ex: Exception) {
            }
        }
        return true
    }

    override fun sendMessage(
        deviceId: String,
        priority: CJLog.Priority,
        timestamp: Long,
        location: String,
        message: String
    ) {
        sendMessage(
            deviceId,
            "<${priority.ordinal + 16 * 8}>${timestamp.rfc3164Date} $clientName ${location.replace(':', '.')}: $message"
        )
    }

    private val rfc3164Formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM dd HH:mm:ss").withLocale(Locale.US)

    private val Long.rfc3164Date: String
        get() = rfc3164Formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}
