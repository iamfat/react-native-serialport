package cn.genee.gpps.module.serialPort

import android.content.Context
import cn.genee.gpps.module.rpcBus.JSONRPCError
import org.json.JSONObject

open class SerialPortListener(private val context: Context) {

    private var path: String? = null
    private var vendorId: Int? = null
    private var productId: Int? = null
    private var baudRate: Int = 38400
    private var parser: String? = null

    var port: SerialPort? = null

    @Throws(Exception::class)
    constructor(context: Context, params: JSONObject) : this(context) {
        baudRate =
            if (params.has("baudrate")) params.getInt("baudrate") else 38400
        path = if (params.has("path")) params.getString("path") else null
        vendorId = if (params.has("vid")) params.getInt("vid") else null
        productId = if (params.has("pid")) params.getInt("pid") else null
        parser = if (params.has("parser")) params.getString("parser") else null
        initPort() || throw Exception(JSONRPCError.Message.SerialPortNotFound)
    }

    @Throws(Exception::class)
    constructor(
        context: Context,
        path: String,
        baudRate: Int = 38400, parser: String? = null
    ) : this(context) {
        this.path = path
        this.vendorId = null
        this.productId = null
        this.baudRate = baudRate
        this.parser = parser
        initPort() || throw Exception(JSONRPCError.Message.SerialPortNotFound)
    }

    @Throws(Exception::class)
    constructor(
        context: Context,
        vendorId: Int, productId: Int,
        baudRate: Int = 38400, parser: String? = null
    ) : this(context) {
        this.path = null
        this.vendorId = vendorId
        this.productId = productId
        this.baudRate = baudRate
        this.parser = parser
        initPort() || throw Exception(JSONRPCError.Message.SerialPortNotFound)
    }

    fun initPort(): Boolean {
        if (path.isNullOrEmpty()) {
            port = SerialPort.getPort(context, vendorId!!, productId!!, baudRate)
        } else {
            port = SerialPort.getPort(context, path!!, baudRate)
        }

        if (port === null) {
            return false
        }

        port?.setParser(parser)
        onConnect()
        return true
    }

    fun start() {
        port?.addListener(this)
    }

    fun write(data: ByteArray) {
        port?.write(data)
    }

    fun stop() {
        port?.removeListener(this)
    }

    fun reset() {
        initPort()
    }

    open fun onConnect() {}
    open fun onMessage(data: ByteArray) {}
    open fun onStatusChange(status: SerialPort.Status) {}
}


