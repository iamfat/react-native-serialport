package cn.genee.rn.serialport

import android.util.Log

object RNLog {
    const val TAG = "RNSerialPort"
    
    fun d(message: String?) {
        Log.d(TAG, message)
    }

    fun e(message: String?) {
        Log.e(TAG, message)
    }
}
