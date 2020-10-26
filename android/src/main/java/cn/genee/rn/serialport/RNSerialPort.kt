package cn.genee.rn.serialport

import android.util.Log

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

import java.net.URI
import java.util.concurrent.Executors

class RNSerialPort(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

    init {
        reactContext.addLifecycleEventListener(this)
    }

    private fun sendEvent(
            deviceId: String,
            params: WritableMap
    ) {
        try {
            reactApplicationContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("SerialPort.event@$deviceId", params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName() = "RNSerialPort"

    override fun onHostDestroy() {
        Log.d(LOG_TAG, "onHostDestroy: close all opening ports")
        ports.forEach { (_, port) ->
            port.close()
        }
        ports.clear()
    }

    override fun onHostPause() {
        // TODO("Not yet implemented")
    }

    override fun onHostResume() {
        // TODO("Not yet implemented")
    }

    @ReactMethod
    fun openPort(deviceId: String, baudRate: Int) {
        if (!ports.containsKey(deviceId)) {
            Log.d(LOG_TAG, "openPort deviceId=$deviceId baudRate=$baudRate")
            val uri = URI(deviceId)
            when (uri.scheme) {
                "file" -> {
                    Log.d(LOG_TAG, "openFilePort path=${uri.path}")
                    ports[deviceId] = object : FileSerialPort(reactApplicationContext, uri.path, baudRate) {
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
                    ports[deviceId] = object : USBSerialPort(reactApplicationContext, uri.path.toInt(), baudRate) {
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
        }
    }

    @ReactMethod
    fun closePort(deviceId: String) {
        Log.d(LOG_TAG, "closePort deviceId=$deviceId")
        if (ports.containsKey(deviceId)) {
            ports[deviceId]?.close()
            ports.remove(deviceId)
        }
    }

    @ReactMethod
    fun writePort(deviceId: String, baudRate: Int, bytesArray: ReadableArray, promise: Promise) {
        val bytes = ByteArray(bytesArray.size()) {
            bytesArray.getInt(it).toByte()
        }

        openPort(deviceId, baudRate)

        val port = ports[deviceId]
        if (port !== null) {
            executor.execute {
                Log.d(LOG_TAG, "writePort deviceId=$deviceId size=${bytes.size}")
                port.write(bytes)
                promise.resolve(bytes.size)
            }
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
