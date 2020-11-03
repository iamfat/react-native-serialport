package cn.genee.rn.serialport

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock

import cn.genee.util.toHexString
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.nio.ByteBuffer

open class USBSerialPort(context: Context, private var usbDeviceId: Int, baudRate: Int) : SerialPort(context, baudRate) {

    // USB specific
    private var usbPort: UsbSerialPort? = null

    override fun openPort(): Boolean {
        var tryCount = 0
        while (status != Status.CLOSING && usbPort == null) {
            val manager = getUSBManager()
            for (device in manager.deviceList.values) {
                if (device.deviceId == usbDeviceId) {
                    RNLog.d("SerialPort.file($usbDeviceId).openPort: found device")
                    usbPort = getUSBPort(manager, device)?.apply {
                        RNLog.d("SerialPort.file($usbDeviceId).openPort: got port")
                        val connection = manager.openDevice(device)
                        open(connection)
                        setParameters(baudRate, 8, 1, 0)
                    }
                    break
                }
            }
            if (usbPort == null) {
                tryCount++
                if (tryCount == 5) {
                    onError(java.lang.Exception("failure on openPort"))
                }
                SystemClock.sleep(10)
            }
        }
        return usbPort !== null
    }

    override fun closePort() {
        try {
            usbPort?.close()
        } catch (e: Exception) {
            RNLog.e("SerialPort.usb($usbDeviceId).closePort: ${e.message}")
        } finally {
            usbPort = null
        }
    }

    override fun write(data: ByteArray): Int {
        try {
            usbPort?.write(data, READ_WAIT_MILLIS)
            RNLog.d("SerialPort.usb($usbDeviceId).write: ${data.toHexString()}")
            return data.size
        } catch (e: Exception) {
            RNLog.e("SerialPort.usb($usbDeviceId).write: error=${e.message}")
            return -1
        }
    }

    private val readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE)!!
    override fun reading() {
        val length = usbPort?.read(readBuffer.array(), READ_WAIT_MILLIS)
        if (length !== null && length > 0) {
            val data = ByteArray(length)
            readBuffer.get(data, 0, length)
            RNLog.d("SerialPort.usb($usbDeviceId).read: ${data.toHexString()}")
            onData(data)
            readBuffer.clear()
        } else {
            SystemClock.sleep(5)
        }
    }

    fun getUSBPort(usbManager: UsbManager, usbDevice: UsbDevice): UsbSerialPort? {
        if (!usbManager.hasPermission(usbDevice)) {
            RNLog.d("usbManager.hasNoPermission $usbDevice")
            val intent = PendingIntent.getBroadcast(
                    context, 0,
                    Intent(ACTION_USB_PERMISSION), 0
            )
            usbManager.requestPermission(usbDevice, intent)
            return null
        }
        RNLog.d("usbManager.hasPermission $usbDevice")

        val driver = UsbSerialProber.getDefaultProber().probeDevice(usbDevice)
        if (driver != null) return driver.ports[0]

        val supportedDevices = mapOf(
                // G-AQ 2.0
                0x0483 to listOf(0x5740),
                // G-Reader
                0x0403 to listOf(0x8288)
        )

        if (!testIfUSBSupported(usbDevice, supportedDevices)) {
            return null
        }

        return CdcAcmSerialDriver(usbDevice).ports[0]
    }

    fun getUSBManager(): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    companion object {
        const val ACTION_USB_PERMISSION = "cn.genee.rn.serialport.action.USB_PERMISSION"

        private const val MAX_BUFFER_SIZE = 16 * 4096
        private const val READ_WAIT_MILLIS = 100 // 100

        init {
            android.serialport.SerialPort.setSuPath("/system/xbin/su")
        }

        private fun testIfUSBSupported(
                usbDevice: UsbDevice,
                supportedDevices: Map<Int, List<Int>>
        ): Boolean {
            val supportedProducts = supportedDevices[usbDevice.vendorId]
            if (supportedProducts != null) {
                val productId = usbDevice.productId
                for (supportedProductId in supportedProducts) {
                    if (productId == supportedProductId) {
                        return true
                    }
                }
            }
            return false
        }
    }
}
