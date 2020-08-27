package cn.genee.rn.serialport

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.SystemClock
import android.util.Log

import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialProber

import java.io.File
import java.lang.reflect.Method
import java.nio.ByteBuffer
import java.util.concurrent.Executors

import cn.genee.util.toHexString

open class SerialPort(private val context: Context) : Runnable {
    private var listeners = HashSet<SerialPortListener>()

    private var deviceId: String? = null

    // USB specific
    private var usbDriver: UsbSerialDriver? = null
    private var usbVendorId: Int? = null
    private var usbProductId: Int? = null
    // File specific
    private var fileDriver: android.serialport.SerialPort? = null
    private var filePath: String? = null

    private var baudRate: Int = 9600

    private var isUSB: Boolean = false

    private var status = Status.ERROR

    enum class Status {
        READY,
        STOPPING,
        ERROR
    }

    constructor(
        applicationContext: Context,
        deviceId: String,
        usbVendorId: Int,
        usbProductId: Int,
        baudRate: Int
    ) : this(applicationContext) {
        this.isUSB = true
        this.baudRate = baudRate
        this.usbVendorId = usbVendorId
        this.usbProductId = usbProductId
        this.deviceId = deviceId
    }

    constructor(
        applicationContext: Context,
        deviceId: String,
        filePath: String,
        baudRate: Int
    ) : this(applicationContext) {
        this.isUSB = false
        this.filePath = filePath
        this.baudRate = baudRate
        this.deviceId = deviceId
        this.status = Status.READY
    }

    private var parser: String? = null
    fun setParser(name: String?) {
        parser = name
    }

    fun getStatus(): Status {
        return status
    }

    private fun openDriver() {
        try {
            if (isUSB) {
                val manager = getUSBManager()
                for (device in manager.deviceList.values) {
                    if (device.vendorId == usbVendorId && device.productId == usbProductId) {
                        Log.d(LOG_TAG, "found USB device $deviceId")
                        usbDriver = probeUSBDevice(manager, device)!![0]
                        usbDriver?.setParameters(this.baudRate, 8, 1, 0)
                        Log.d(LOG_TAG, "found usbDriver $usbDriver")
                        break
                    }
                }
                usbDriver!!.open()
            } else {
                val file = File(filePath)
                if (file.exists()) {
                    Log.d(LOG_TAG, "found regular device $deviceId")
                    fileDriver = android.serialport.SerialPort(file, baudRate, 0)
                }
            }
        } catch (e: Exception) {
            error(e)
            Log.e("SerialPort openDriver: ${e.message}")
        }
    }

    private fun closeDriver() {
        try {
            if (isUSB) {
                usbDriver?.close()
            } else {
                fileDriver?.close()
            }
        } catch (e: Exception) {
            Log.e("SerialPort closeDriver: ${e.message}")
        } finally {
            if (isUSB) {
                usbDriver = null
            } else {
                fileDriver = null
            }
        }
    }

    fun close() {
        changeStatus(Status.STOPPING)
    }

    fun write(data: ByteArray) {
        try {
            if (isUSB) {
                usbDriver!!.write(data, READ_WAIT_MILLIS)
            } else {
                fileDriver!!.outputStream.write(data)
                fileDriver!!.outputStream.flush()
            }
            Log.d(LOG_TAG, "SerialPort($deviceId).write: ${data.toHexString()}")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "SerialPort($deviceId).write: error=${e.message}")
            closeDriver()
        }
    }

    private val readBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE)!!
    private fun reading() {
        if (isUSB) {
            val length = usbDriver!!.read(readBuffer.array(), READ_WAIT_MILLIS)
            if (0 < length) {
                var data = ByteArray(length)
                readBuffer.get(data, 0, length)
                //usbDriver!!.read(data, READ_WAIT_MILLIS)
                // send it to parser to get parsed data
                if (parser != null) {
                    val method = parserMethod(parser!!)
                    data = method!!.invoke(null, data) as ByteArray
                }
                Log.d(LOG_TAG, "SerialPort($deviceId).read: ${data.toHexString()}")
                for (listener in listeners) {
                    listener.onMessage(data)
                }
                readBuffer.clear()
            }
        } else {
            val stream = fileDriver!!.inputStream
            if (0 < stream!!.available()) {
                val length = stream.read(readBuffer.array())
                var data = ByteArray(length)
                readBuffer.get(data, 0, length)
                Log.d(LOG_TAG, "SerialPort($deviceId).read: ${data.toHexString()}")
                // send it to parser to get parsed data
                if (parser != null) {
                    val method = parserMethod(parser!!)
                    data = method!!.invoke(null, data) as ByteArray
                }
                for (listener in listeners) {
                    listener.onMessage(data)
                }
                readBuffer.clear()
            }

        }
    }

    private fun error(e: Exception) {
        e.printStackTrace()
        if (this.status === Status.READY) {
            changeStatus(Status.ERROR)
        }
    }

    private fun changeStatus(status: Status) {
        if (this.status != status) {
            this.status = status
            for (listener in listeners) {
                try {
                    listener.onStatusChange(this.status)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun run() {
        Log.d(LOG_TAG, "SerialPort $deviceId thread started")
        while (status != Status.STOPPING) {
            var tryCount = 0
            while (status != Status.STOPPING && usbDriver == null && fileDriver == null && status != Status.STOPPING) {
                openDriver()
                if (usbDriver == null && fileDriver == null) {
                    tryCount++
                    if (tryCount == 5) {
                        error(java.lang.Exception("串口打开失败"))
                    }
                    SystemClock.sleep(10)
                }
            }
            try {
                reading()
                if (status == Status.ERROR) {
                    changeStatus(Status.READY)
                }
            } catch (e: Exception) {
                error(e)
                closeDriver()
            }
            SystemClock.sleep(5)
        }
        closeDriver()
        changeStatus(Status.STOPPING)
        Log.d(LOG_TAG, "SerialPort $deviceId thread stopped")
    }

    fun addListener(listener: SerialPortListener) {
        listener.onStatusChange(status)
        if (listeners.isEmpty()) {
            executor.execute(this)
        }
        listeners.add(listener)
    }

    fun removeListener(listener: SerialPortListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            close()
            ports.remove(deviceId)
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

        const val ACTION_USB_PERMISSION = "cn.genee.gpps.action.USB_PERMISSION"

        val StatusText = mapOf(
            Status.READY to "ready",
            Status.STOPPING to "stopping",
            Status.ERROR to "error"
        )

        private val ports = HashMap<String, SerialPort>()
        private val executor = Executors.newCachedThreadPool()

        private const val MAX_BUFFER_SIZE = 16 * 4096
        private const val READ_WAIT_MILLIS = 100 //100
        private const val RECONNECT_MILLIS = 3000L

        init {
            android.serialport.SerialPort.setSuPath("/system/xbin/su");
        }

        fun getPort(context: Context, vid: Int, pid: Int, baudrate: Int): SerialPort? {
            val deviceId = "usb:$vid/$pid"
            if (deviceId !in ports) {
                ports[deviceId] = SerialPort(context, deviceId, vid, pid, baudrate)
            }
            return ports[deviceId]
        }

        fun getPort(context: Context, path: String, baudrate: Int): SerialPort? {
            val file = File(path)
            val deviceId = "com:${file.absolutePath}"
            if (deviceId !in ports) {
                ports[deviceId] =
                    SerialPort(context, deviceId, file.absolutePath, baudrate)
            }
            return ports[deviceId]
        }


        private var parserMethodMap: HashMap<String, Method>? = null
        fun parserMethod(path: String): Method? {
            if (parserMethodMap == null) {
                parserMethodMap = HashMap()
                Constants.modules.forEach {
                    val c = it.value
                    val methods = c.declaredMethods
                    for (method in methods) {
                        val ann = method.getAnnotation(SerialPortParser::class.java)
                        if (ann != null) {
                            val key = ann.value
                            if (!key.isEmpty()) {
                                parserMethodMap!![key] = method
                            }
                        }
                    }
                }
            }
            return parserMethodMap!![path]
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





