CJLog
=====

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.control-j.cjlog/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.control-j.cjlog/core)

This library provides a lightweight, system independent logging facility to multiple destinations.
Written in Kotlin, it can also be used in Java projects. A logging destination can be to a file, via HTTP to a local host,
to the system's logging facility or wherever else you like - a new type of destination can be added by implementing
a simple interface.

## Installation

The library is available through Maven Central. Add this to your `build.gradle`

````
dependencies {
    implementation("com.control-j.cjlog:core:2.4")
}
````

Alternatively you could:

1. Download a JAR file from one of the [releases](https://github.com/clydebarrow/CJLog/releases) and add to your project libraries.
1. Clone the repository and add as a gradle sub-project;
1. Clone the repository and run `gradlew build` in the root, this will create `build/libs/cjlog-1.1.jar` - add this file to your project libraries.

## Usage

In your application startup setup some parameters and add destinations. For example
````kotlin
        CJLog.deviceId = this.javaClass.simpleName + "-" + deviceName.replace(' ', '_')
        CJLog.isDebug = isDebugBuild
        CJLog.add(FileLogger(File(tmpdir, "app.log")))
        CJLog.captureErr()
        CJLog.add(FoundationLogger())
        logMsg("device is $model")
        // do http logging only for simulator or ad-hoc builds
        if (isDebugBuild)
            CJLog.add(HttpLogger())
````

In this example `deviceName` should previously have been set to an identifier for the device - on Android this
would be `Build.DEVICE`. The value `isDebugBuild` is a boolean
specifying whether this is a debug build, for example on Android this would be `BuildConfig.DEBUG`. Under RoboVM on iOS
these functions will work:

````kotlin
        val isSimulator: Boolean by lazy {
            NSProcessInfo.getSharedProcessInfo().environment.containsKey("SIMULATOR_DEVICE_NAME")
        }
        val deviceName: String by lazy {
            if (isSimulator) NSProcessInfo.getSharedProcessInfo().environment["SIMULATOR_DEVICE_NAME"].toString()
            else UIDevice.getCurrentDevice().identifierForVendor.asString().takeLast(8)
        }

        val isDebugBuild: Boolean by lazy {
            isSimulator || NSBundle.getMainBundle().findResourcePath("embedded", "mobileprovision") != null
        }
````

In the above example a logfile is created using the call `CJLog.add(FileLogger(File(tmpdir, "app.log")))`. You can retrieve
this file later via the property [`CJLog.logfiles`]
The [`CJLog.captureErr()`] call allows the logger to capture and log anything printed to STDERR.

Another destination is added with `CJLog.add(FoundationLogger())` - in this case `FoundationLogger()` creates an iOS
Foundation logger - the source for `FoundationLogger` is:
````kotlin
class FoundationLogger : Destination {
    override fun sendMessage(deviceID: String, message: String) {
        Foundation.log(message.removeSuffix("\n"))
    }
}
````

The Android equivalent is:
````kotlin
class LogCatLogger : Destination {
    override fun sendMessage(deviceID: String, message: String) {
        Log.d(deviceID, message)
    }
}
````

The [`HttpLogger`] class will log to a suitably provisioned web server. This is typically used only in development to a local
host - it does not implement any security. To set the destination URL set the [`HttpLogger.SYSLOGGER`] property.

## Logging data

Logging a message is as simple as:
````kotlin
        CJLog.logMsg("Something failed $error")
````
or if you want to use `String.format()` style arguments:
````kotlin
      CJLog.logMsg("Got data %d length %d", value, length)
````
The method [`CJLog.debug()`] behaves identically to [`logMsg()`] except it only logs if the [`isDebug`] property is true.
Exceptions can be logged with [`logException()`]

## API Docs

Can be found [here](https://clydebarrow.github.io/CJLog/cjlog/index.html). 

[`CJLog.debug()`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/debug.html]
[`logMsg()`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/log-msg.html]
[`logException()`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/log-exception.html]
[`FileLogger`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/log-exception.html]
[`isDebug`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/is-debug.html]
[`CJLog.captureErr()`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/capture-err.html]
[`CJLog.logFiles`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/log-files.html]
[`FileLogger`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-c-j-log/-file-logger.html]
[`HttpLogger.SYSLOGGER`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-http-logger/-s-y-s-l-o-g-g-e-r.html]
[`HttpLogger`]:[https://clydebarrow.github.io/CJLog/cjlog/com.controlj.logging/-http-logger/index.html]
