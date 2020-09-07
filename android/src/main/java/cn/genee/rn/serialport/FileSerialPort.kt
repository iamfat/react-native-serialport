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
    private var fileDriver: android.serialport.SerialPort? = null

    init {
        val file = File(filePath)
        if (file.exists()) {
            fileDriver = android.serialport.SerialPort(file, baudRate, 0)
        } else {
            Log.d(LOG_TAG, "$filePath not found")
        }
    }

    override fun openDriver() {
        var tryCount = 0
        while (status != Status.CLOSING && fileDriver == null) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    Log.d(LOG_TAG, "found regular device $filePath")
                    fileDriver = android.serialport.SerialPort(file, baudRate, 0)
                }
            } catch (e: Exception) {
                onError(e)
                Log.e(LOG_TAG, "SerialPort openDriver: ${e.message}")
            }

            if (fileDriver == null) {
                tryCount++
                if (tryCount == 5) {
                    onError(java.lang.Exception("串口打开失败"))
                }
                SystemClock.sleep(10)
            }
        }
    }

    override fun closeDriver() {
        try {
            fileDriver?.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SerialPort.file($filePath) closeDriver: ${e.message}")
        } finally {
            fileDriver = null
        }
    }

    override fun write(data: ByteArray) {
        try {
            fileDriver?.outputStream?.apply {
                write(data)
                flush()
            }
            Log.d(LOG_TAG, "SerialPort.file($filePath).write: ${data.toHexString()}")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "SerialPort.file($filePath).write: error=${e.message}")
            closeDriver()
        }
    }

    private val readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE)!!
    override fun reading() {
        val inputStream = fileDriver?.inputStream
        if (inputStream !== null && inputStream.available() > 0) {
            val length = inputStream.read(readBuffer.array())
            val data = ByteArray(length)
            readBuffer.get(data, 0, length)
            Log.d(LOG_TAG, "SerialPort.file($filePath).read: ${data.toHexString()}")
            onData(data)
            readBuffer.clear()
         } else {
            SystemClock.sleep(5)
         }
    }

    companion object {
        private const val LOG_TAG = "serialport:file"
        private const val MAX_BUFFER_SIZE = 16 * 4096

        init {
            android.serialport.SerialPort.setSuPath("/system/xbin/su")
        }
    }
}
