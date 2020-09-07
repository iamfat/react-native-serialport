package cn.genee.rn.serialport

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbRequest
import android.os.Build

import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * USB CDC/ACM serial driver implementation.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @see [Universal Serial Bus Class Definitions for Communication Devices, v1.1](http://www.usb.org/developers/devclass_docs/usbcdc11.pdf)
 */
class CdcAcmSerialDriver(device: UsbDevice, connection: UsbDeviceConnection) : com.hoho.android.usbserial.driver.CdcAcmSerialDriver(device, connection) {
    private var mReadEndpoint: UsbEndpoint? = null

    @Throws(IOException::class)
    override fun open() {
        super.open()
        mReadEndpoint = mDevice.getInterface(1).getEndpoint(1)
    }

    @Throws(IOException::class)
    override fun read(dest: ByteArray, timeoutMillis: Int): Int {
        if (ASYNC_READS_ENABLED) {
            val request = UsbRequest()
            return try {
                request.initialize(mConnection, mReadEndpoint)
                val buf = ByteBuffer.wrap(dest)
                request.queue(buf)
                if (mConnection.requestWait() == null) {
                    throw IOException("Null response")
                }
                buf.position()
            } finally {
                request.close()
            }
        }
        return super.read(dest, timeoutMillis)
    }

    companion object {
        val ASYNC_READS_ENABLED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
    }
}
