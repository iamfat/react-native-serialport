package cn.genee.rn.serialport

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock

import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber

import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

import cn.genee.util.toHexString
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver

open class SerialPort(protected val context: Context, val baudRate: Int) : Runnable {
    protected var status = Status.READY

    enum class Status {
        OPENING,
        READY,
        CLOSING,
        CLOSED
    }

    fun close() {
        if (status !== Status.CLOSED) {
            status = Status.CLOSING
        }
    }

    open fun write(data: ByteArray): Int { return 0 }

    open fun reading() {}
    open fun openPort(): Boolean { return false }
    open fun closePort() {}

    override fun run() {
        this.status = Status.READY
        RNLog.d("SerialPort thread started")
        if (openPort()) {
            onOpen()
            while (status !== Status.CLOSING) {
                try {
                    reading()
                } catch (e: Exception) {
                    onError(e)
                    break
                }
            }
            closePort()
            onClose()
        }
        this.status = Status.CLOSED
        RNLog.d("SerialPort thread stopped")
    }

    open fun onOpen() {}
    open fun onData(data: ByteArray) {}
    open fun onError(error: Exception) {}
    open fun onClose() {}
}
