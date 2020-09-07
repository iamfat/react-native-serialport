package cn.genee.rn.serialport

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class RNSerialPortPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext) = listOf(RNSerialPort(reactContext))
    override fun createViewManagers(reactContext: ReactApplicationContext) = emptyList<ViewManager<*, *>>()
}