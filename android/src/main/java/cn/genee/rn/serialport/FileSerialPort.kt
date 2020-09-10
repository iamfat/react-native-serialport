package cn.genee.rn.serialport

import android.content.Context
import android.os.SystemClock
import android.util.Log
import cn.genee.util.toHexString
import java.io.File
import java.nio.ByteBuffer

open class FileSerialPort(
        applicationContext: Context,
        private val filePath: String,
        baudRate: Int
) : SerialPort(applicationContext, baudRate) {
    private var filePort: android.serialport.SerialPort? = null

    init {
        val file = File(filePath)
        if (file.exists()) {
            filePort = android.serialport.SerialPort(file, baudRate)
        } else {
            Log.d(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).init: $filePath not found")
        }
    }

    override fun openPort(): Boolean {
        var tryCount = 0
        while (status != Status.CLOSING && filePort == null) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    Log.d(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).openPort: found regular device")
                    filePort = android.serialport.SerialPort(file, baudRate)
                }
            } catch (e: Exception) {
                onError(e)
                Log.e(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).openPort: ${e.message}")
            }

            if (filePort == null) {
                tryCount++
                if (tryCount == 5) {
                    onError(java.lang.Exception("failure on openPort"))
                }
                SystemClock.sleep(10)
            }
        }
        return filePort !== null
    }

    override fun closePort() {
        try {
            filePort?.close()
        } catch (e: Exception) {
            Log.e(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).closePort: ${e.message}")
        } finally {
            filePort = null
        }
    }

    override fun write(data: ByteArray): Int {
        try {
            filePort?.outputStream?.apply {
                write(data)
                flush()
            }
            Log.d(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).write: ${data.toHexString()}")
            return data.size
        } catch (e: Exception) {
            Log.d(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).write: error=${e.message}")
            return -1
        }
    }

    private val readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE)!!
    override fun reading() {
        val inputStream = filePort?.inputStream
        if (inputStream !== null && inputStream.available() > 0) {
            val length = inputStream.read(readBuffer.array())
            val data = ByteArray(length)
            readBuffer.get(data, 0, length)
            Log.d(RNSerialPort.LOG_TAG, "SerialPort.file($filePath).read: ${data.toHexString()}")
            onData(data)
            readBuffer.clear()
        } else {
            SystemClock.sleep(5)
        }
    }

    companion object {
        private const val MAX_BUFFER_SIZE = 16 * 4096

        init {
            android.serialport.SerialPort.setSuPath("/system/xbin/su")
        }
    }
}
