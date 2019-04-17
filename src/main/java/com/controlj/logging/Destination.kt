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

import java.io.File

/**
 * An interface for a logging destination.
 *
 */
interface Destination {

    /**
     * Send a log message to the destination. This is the only mandatory method to be implemented.
     * @param deviceID    Identifies the device and app
     * @param message    The message
     */
    fun sendMessage(deviceID: String, message: String)

    /**
     * Retrieve a list of files for previous logs. Returns null if the destination does not support this.
     * @return A list of Files
     */
    val previousLogfiles: List<File>?
        get() = null

    /**
     * Retrieve log messages previously sent to this destination.
     * @param limit    The maximum number of bytes to return.
     * @return    The messages, separated by newlines. Null if this consumer does not offer a means
     * to retrieve messages.
     */
    fun retrieveMessages(limit: Int): String? {
        return null
    }

    /**
     * Terminate the logger
     */
    fun close() = Unit
}

