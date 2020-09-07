package cn.genee.rn.serialport

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock
import android.util.Log
import cn.genee.util.toHexString
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.nio.ByteBuffer
import java.util.concurrent.Executors

open class USBSerialPort(context:Context, private var usbDeviceId: Int, baudRate:Int) : SerialPort(context, baudRate) {

    // USB specific
    private var usbDriver: UsbSerialDriver? = null

    override fun openDriver() {
        var tryCount = 0
        while (status != Status.CLOSING && usbDriver == null) {
            val manager = getUSBManager()
            for (device in manager.deviceList.values) {
                if (device.deviceId == usbDeviceId) {
                    Log.d(LOG_TAG, "found USB device $usbDeviceId")
                    probeUSBDevice(manager, device)?.let {
                        if (it.size > 0) {
                            it[0].apply {
                                Log.d(LOG_TAG, "found usbDriver $this")
                                setParameters(baudRate, 8, 1, 0)
                                open()
                                usbDriver = this
                            }
                        }
                    }

                    break
                }
            }
            if (usbDriver == null) {
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
            usbDriver?.close()
        } catch (e: Exception) {
            Log.e(LOG_TAG, "SerialPort closeDriver: ${e.message}")
        } finally {
            usbDriver = null
        }
    }

    override fun write(data: ByteArray) {
        try {
            usbDriver?.write(data, READ_WAIT_MILLIS)
            Log.d(LOG_TAG, "SerialPort.usb($usbDeviceId).write: ${data.toHexString()}")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "SerialPort.usb($usbDeviceId).write: error=${e.message}")
            closeDriver()
        }
    }

    private val readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE)!!
    override fun reading() {
        val length = usbDriver?.read(readBuffer.array(), READ_WAIT_MILLIS)
        if (length !== null && length > 0) {
            val data = ByteArray(length)
            readBuffer.get(data, 0, length)
            Log.d(LOG_TAG, "SerialPort.usb($usbDeviceId).read: ${data.toHexString()}")
            onData(data)
            readBuffer.clear()
        } else {
            SystemClock.sleep(5)
        }
    }

    fun probeUSBDevice(usbManager: UsbManager, usbDevice: UsbDevice): List<UsbSerialDriver>? {
        if (!usbManager.hasPermission(usbDevice)) {
            Log.d(LOG_TAG, "usbManager.hasNoPermission $usbDevice")
            val intent = PendingIntent.getBroadcast(
                    context, 0,
                    Intent(ACTION_USB_PERMISSION), 0
            )
            usbManager.requestPermission(usbDevice, intent)
            return null
        }
        Log.d(LOG_TAG, "usbManager.hasPermission $usbDevice")
        val drivers = UsbSerialProber.probeSingleDevice(usbManager, usbDevice)
        if (drivers != null && drivers.size > 0) return drivers

        val supportedDevices = mapOf(
                // 0x0403 / 0x6001: FTDI FT232R UART
                // 0x0403 / 0x6015: FTDI FT231X
                0x0403 to listOf(0x6001, 0x6015),
                // 0x2341 / Arduino
                // 0x2341 to listOf(),
                // 0x16C0 / 0x0483: Teensyduino
                0x16C0 to listOf(0x0483),
                // 0x10C4 / 0xEA60: CP210x UART Bridge
                0x10C4 to listOf(0xEA60),
                // 0x067B / 0x2303: Prolific PL2303
                0x067B to listOf(0x2303),
                // 0x1A86 / 0x7523: Qinheng CH340
                0x1A86 to listOf(0x7523),
                //  G-AQ 2.0
                0x0483 to listOf(0x5740)
        )

        if (!testIfUSBSupported(usbDevice, supportedDevices)) {
            return null
        }

        val connection = usbManager.openDevice(usbDevice)
        return listOf<UsbSerialDriver>(CdcAcmSerialDriver(usbDevice, connection))
    }

    fun getUSBManager(): UsbManager {
        return context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    companion object {
        const val LOG_TAG = "usbserial"
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
