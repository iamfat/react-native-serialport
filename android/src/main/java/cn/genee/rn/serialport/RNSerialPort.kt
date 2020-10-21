package cn.genee.rn.serialport

import android.app.Activity
import android.content.Intent
import android.util.Log

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.modules.core.RCTNativeAppEventEmitter

import java.net.URI
import java.util.concurrent.Executors

class RNSerialPort(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), ActivityEventListener {

    init {
        reactContext.addActivityEventListener(this)
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
    }

    override fun onNewIntent(intent: Intent?) {
    }

    private fun sendEvent(
            deviceId: String,
            params: WritableMap
    ) {
        try {
            reactApplicationContext
                    .getJSModule(RCTNativeAppEventEmitter::class.java)
                    .emit("SerialPort.event@$deviceId", params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName() = "RNSerialPort"

    @ReactMethod
    fun openPort(deviceId: String, baudRate: Int) {
        Log.d(LOG_TAG, "openPort deviceId=$deviceId baudRate=$baudRate")
        if (!ports.containsKey(deviceId)) {
            val context = reactApplicationContext
            val uri = URI(deviceId)
            when(uri.scheme) {
                "file" -> {
                    Log.d(LOG_TAG, "openFilePort path=${uri.path}")
                    ports[deviceId] = object : FileSerialPort(context, uri.path, baudRate) {
                        override fun onData(data: ByteArray) {
                            val params = WritableNativeMap()
                            params.putString("event", "data")

                            val dataArray = WritableNativeArray()
                            data.map { dataArray.pushInt(it.toInt()) }
                            val eventParams = WritableNativeArray()
                            eventParams.pushArray(dataArray)
                            params.putArray("params", eventParams)

                            sendEvent(deviceId, params)
                        }

                        override fun onClose() {
                            val params = WritableNativeMap()
                            params.putString("event", "close")
                            sendEvent(deviceId, params)
                            ports.remove(deviceId)
                        }
                    }.apply {
                        executor.execute(this)
                    }
                }
                "usb" -> {
                    Log.d(LOG_TAG, "openUSBPort id=${uri.path}")
                    ports[deviceId] = object : USBSerialPort(context, uri.path.toInt(), baudRate) {
                        override fun onData(data: ByteArray) {
                            val params = WritableNativeMap()
                            params.putString("event", "data")

                            val dataArray = WritableNativeArray()
                            data.map { dataArray.pushInt(it.toInt()) }
                            val eventParams = WritableNativeArray()
                            eventParams.pushArray(dataArray)
                            params.putArray("params", eventParams)

                            sendEvent(deviceId, params)
                        }

                        override fun onClose() {
                            val params = WritableNativeMap()
                            params.putString("event", "close")
                            sendEvent(deviceId, params)
                            ports.remove(deviceId)
                        }
                    }.apply {
                        executor.execute(this)
                    }
                }
            }
        } else {
            executor.execute(ports[deviceId])
        }
    }

    @ReactMethod
    fun closePort(deviceId: String) {
        Log.d(LOG_TAG, "closePort deviceId=$deviceId")
        ports[deviceId]?.close()
    }

    @ReactMethod
    fun writePort(deviceId: String, bytesArray: ReadableArray, promise: Promise) {
        val bytes = ByteArray(bytesArray.size()) {
            bytesArray.getInt(it).toByte()
        }
        Log.d(LOG_TAG, "writePort deviceId=$deviceId size=${bytes.size}")
        val port = ports[deviceId]
        if (port !== null) {
            Thread {
                port.write(bytes)
                promise.resolve(bytes.size)
            }.start()
        } else {
            promise.resolve(-1)
        }
    }

    companion object {
        const val LOG_TAG = "serialport"

        private val ports = HashMap<String, SerialPort>()
        private val executor = Executors.newCachedThreadPool()
    }
}
