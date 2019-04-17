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
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * A watchdog timer for a thread. The watched thread will be that associated with the scheduler, typically
 * this will be the platform's main thread scheduler
 * a crash dump of the watched thread will be delivered to the observer.
 * Do NOT subscribe to this on the thread you want to watch :-)
 *
 * @param scheduler The scheduler for the thread of interest.
 * @param timeout The watchdog timeout in timeUnits
 * @param timeUnit The timeUnit
 *
 */
class WatchDog(private val scheduler: Scheduler, private val timeout: Long, private val timeUnit: TimeUnit, private val action: ((String) -> Unit)) {

    private var thread: Thread? = null
    private var disposable = Disposables.disposed()
    private val observable = Observable.interval(timeout / 2, timeUnit, scheduler)
            .timeout(timeout, timeUnit, Schedulers.io())

    // we capture the thread used by the scheduler here
    init {
        scheduler.scheduleDirect { thread = Thread.currentThread() }
    }

    /**
     * Start the watchdog.
     */
    fun start() {
        stop()
        disposable = observable.subscribe(
                {},
                {
                    val result = thread?.stackTrace?.joinToString("\n", "Watchdog timeout:\n") ?: "Watchdog thread did not initialise"
                    logMsg(result)
                    action(result)
                })
    }

    /**
     * Stop the watchdog.
     */
    fun stop() {
        disposable.dispose()
    }
}
