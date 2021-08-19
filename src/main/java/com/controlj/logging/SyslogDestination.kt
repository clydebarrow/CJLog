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

/**
 * Send log messages to a UDP Syslog destination.
 * @param host The destination host
 * @param port The destination port, defaults to 514.
 */
class SyslogDestination(val host: String = "localhost", val port: Int = 514) : NetworkLogger() {

    var serverAddress = InetAddress.getByName(host)
    var datagramSocket = DatagramSocket()
    val hostName = InetAddress.getLocalHost().hostName

    override fun sendLog(data: Message): Boolean {
        try {
            val bytes = data.bytes
            val dgram = DatagramPacket(bytes, bytes.size, serverAddress, port)
            datagramSocket.send(dgram)
            return true
        } catch (ex: Exception) {
            return false
        }
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
            "<${priority.ordinal + 16 * 8}>${timestamp.rfc3164Date} $hostName ${location.replace(':', '.')}: $message"
        )
    }

    val Long.rfc5424Date: String
        get() = String.format("%1\$tFT%1\$tT%1\$tZ", this)

    val rfc3164Formatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss").withLocale(Locale.US)

    val Long.rfc3164Date: String
        get() = rfc3164Formatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
}
