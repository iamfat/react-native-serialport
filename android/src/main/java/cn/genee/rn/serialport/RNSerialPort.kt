package cn.genee.rn.serialport

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

import java.net.URI
import java.util.concurrent.Executors

class RNSerialPort(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private fun sendEvent(
            deviceId: String,
            params: WritableMap
    ) {
        if (reactApplicationContext.hasActiveCatalystInstance()) {
            reactApplicationContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("SerialPort.event@$deviceId", params)
        }
    }

    override fun getName() = "RNSerialPort"

    override fun onCatalystInstanceDestroy() {
        RNLog.d("onCatalystInstanceDestroy: close all opening ports")
        ports.forEach { (_, port) ->
            port.close()
        }
        ports.clear()
        super.onCatalystInstanceDestroy()
    }

    @ReactMethod
    fun openPort(deviceId: String, baudRate: Int) {
        if (!ports.containsKey(deviceId)) {
            RNLog.d("openPort deviceId=$deviceId baudRate=$baudRate")
            val uri = URI(deviceId)
            when (uri.scheme) {
                "file" -> {
                    RNLog.d("openFilePort path=${uri.path}")
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
                    RNLog.d("openUSBPort id=${uri.path}")
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
        RNLog.d("closePort deviceId=$deviceId")
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
                RNLog.d("writePort deviceId=$deviceId size=${bytes.size}")
                port.write(bytes)
                promise.resolve(bytes.size)
            }
        } else {
            promise.resolve(-1)
        }
    }

    companion object {
        private val ports = HashMap<String, SerialPort>()
        private val executor = Executors.newCachedThreadPool()
    }
}
