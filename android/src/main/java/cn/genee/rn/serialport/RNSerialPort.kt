package cn.genee.rn.serialport

import android.app.Activity
import android.content.Intent
import android.util.Base64
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
            params: WritableMap? = null
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
        if (!ports.containsKey(deviceId)) {
            val context = reactApplicationContext
            val uri = URI(deviceId)
            if (uri.scheme === "file") {
                ports[deviceId] = object : FileSerialPort(context, uri.path, baudRate) {
                    override fun onData(data: ByteArray) {
                        val map = WritableNativeMap()
                        map.putString("data", "@buf:${
                            Base64.encodeToString(
                                    data,
                                    Base64.NO_WRAP
                            )
                        }")
                        sendEvent(deviceId, map)
                    }

                    override fun onClose() {
                        sendEvent(deviceId)
                    }
                }.apply {
                    executor.execute(this)
                }
            } else if (uri.scheme === "usb") {
                ports[deviceId] = object : USBSerialPort(context, uri.path.toInt(), baudRate) {
                    override fun onData(data: ByteArray) {
                        val map = WritableNativeMap()
                        map.putString("data", "@buf:${
                            Base64.encodeToString(
                                    data,
                                    Base64.NO_WRAP
                            )
                        }")
                        sendEvent(deviceId, map)
                    }

                    override fun onClose() {
                        sendEvent(deviceId)
                    }
                }.apply {
                    executor.execute(this)
                }
            }
        } else {
            executor.execute(ports[deviceId])
        }
    }

    @ReactMethod
    fun closePort(deviceId: String) {
        ports[deviceId]?.close()
        ports.remove(deviceId)
    }

    @ReactMethod
    fun writePort(deviceId: String, bytes: Array<Int>, promise: Promise?) {
        val port = ports[deviceId]
        if (port !== null) {
            Thread {
                port.write(bytes.map { it.toByte() }.toByteArray())
                promise?.resolve(null)
            }.start()
        }
    }

    companion object {
        const val LOG_TAG = "RNSerialPort"

        private val ports = HashMap<String, SerialPort>()
        private val executor = Executors.newCachedThreadPool()
    }
}
