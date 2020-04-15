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

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.jvm.jvmName

/**
 * This class enables logging to a file and optionally a remote server.
 */
object CJLog {

    /**
     * The package version number
     */

    var versionString: String = "?.?"

    /**
     * The package build number
     */

    var buildNumber: String = "-1"

    /**
     * The deviceID. Set by the Application on startup
     */
    var deviceId: String = "?????"

    /**
     * The package name for the app
     */

    var packageName: String = "????"

    /**
     * If this is a debug build. Must be set by external logic, as it is system dependent.
     */
    var isDebug = false
    // The list of logging destinations.
    private val destinations = ConcurrentLinkedQueue<Destination>()

    /**
     * Delivers a stream of log messages to anyone interested. Useful for in-app display of the log stream.
     * See also [logFiles]
     */
    val loggerObservable: Observable<String> by lazy {

        Observable.create { emitter: ObservableEmitter<String> ->
            val s = logString
            if (s != null)
                emitter.onNext(s)
            val obsDest = object : Destination {
                override fun sendMessage(deviceID: String, message: String) {
                    emitter.onNext(message)
                }

                override fun close() {
                    emitter.onComplete()
                }
            }
            destinations.add(obsDest)
            emitter.setCancellable { destinations.remove(obsDest) }
        }.share()
    }

    /**
     * Retrieve a string with the most recent log messages. All destinations will be queried for this, the first
     * one that can deliver a result will be used. How much is retained will be destination dependent.
     */
    private val logString: String?
        get() {
            for (consumer in destinations) {
                val s = consumer.retrieveMessages(LOGMAX)
                if (s != null)
                    return s
            }
            return null
        }

    private fun addMessage(message: String, vararg args: Any) {
        var msgString = message
        val tag = tag
        if (args.size != 0)
            msgString = String.format(message, *args)
        msgString = String.format(Locale.US, "%1\$tF %1\$tT %2\$s: %3\$s\n", System.currentTimeMillis(), tag, msgString)
        destinations.forEach { it.sendMessage(deviceId, msgString) }
    }

    /**
     * Close all destinations and remove them from the list.
     */
    fun close() {
        destinations.forEach { it.close() }
        destinations.clear()
    }

    /**
     * Call this function if you want the logger to capture the STDERR output stream.
     */
    fun captureErr() {
        System.setErr(PrintStream(object : OutputStream() {
            val buffer = StringBuffer()
            override fun write(p0: Int) {
                if (p0 == '\n'.toInt()) {
                    logMsg("%s", buffer.toString())
                    buffer.setLength(0)
                } else
                    buffer.append(p0.toChar())
            }
        }))
    }

    private val ANONYMOUS_CLASS = Regex("(\\$\\d+)+$")

    private val LOGMAX = 8192

    private fun isLoggerName(s: String): Boolean {
        return s == CJLog::class.jvmName || s.contains("Logger")
    }

    // Get the source file and line number from whence the message came. Change this code at your peril.
    // lines from within logger code are skipped.
    private val tag: String
        get() {
            val stackTrace = Throwable().stackTrace
            for (element in stackTrace) {
                if (!isLoggerName(element.className)) {
                    val tag = element.className.substringAfterLast('.').replace(ANONYMOUS_CLASS, "")
                    return if (element.lineNumber > 0) tag + ":" + element.lineNumber else tag
                }
            }
            return "??"
        }

    /**
     * Add a log message. If additional arguments are supplied the [message] string will be formatted using
     * String.format() otherwise it will be used bare.
     * A timestamp and the device ID will be prepended to the message, after which it is sent to all destinations.
     */
    @JvmStatic
    fun logMsg(message: String, vararg args: Any) {
        addMessage(message, *args)
    }

    /**
     * Add a log message only if this is a debug build. See [addMessage]
     */
    @JvmStatic
    fun debug(message: String, vararg args: Any) {
        if (isDebug)
            addMessage(message, *args)
    }

    /**
     * Log an exception.
     */
    @JvmStatic
    fun logException(e: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            addMessage("Exception: $e\n$sw")
            e.cause?.let { cause ->
                addMessage("Caused by:")
                logException(cause)
            }
        } catch (ignored: Exception) {
        }

    }

    /**
     * Retrieve a string with the most recent log messages. All destinations will be queried for this, the first
     * one that can deliver a result will be used. How much is retained will be destination dependent.
     */
    @JvmStatic
    val log: String?
        get() = logString

    /**
     * Query all destinations for preserved log files. Return the first one found.
     */
    val logFiles: List<File>
        get() = destinations.mapNotNull { it.previousLogfiles }.firstOrNull() ?: listOf()

    /**
     * Add a new [Destination] to the list.
     */
    @JvmStatic
    fun add(destination: Destination) {
        destinations.add(destination)
    }


    /**
     * Remove [destination] from the current list of destinations.
     */
    @JvmStatic
    fun remove(destination: Destination) {
        destinations.remove(destination)
    }

    /**
     * Useful logging lambda to poke into various places.
     */
    val logger = { message: String -> logMsg(message) }
}
