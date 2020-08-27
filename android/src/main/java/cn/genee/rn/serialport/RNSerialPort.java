package cn.genee.rn.serialport;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.SparseArray;
import android.media.MediaScannerConnection;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@ReactModule(name = RNSerialPort.MODULE_NAME)
public class RNSerialPort extends ReactContextBaseJavaModule {

  static final String MODULE_NAME = "RNSerialPort";

  private ReactApplicationContext reactContext;

  public RNSerialPort(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  private void sendEvent(String eventName,
  @Nullable WritableMap params) {
getReactApplicationContext()
.getJSModule(RCTNativeAppEventEmitter.class)
.emit(eventName, params);
}

private void sendEventWithJson(String eventName,
          JSONObject json) {
try {
WritableMap map = JsonConvert.jsonToReact(json);
sendEvent(eventName, map);
} catch (JSONException ex) {
Log.d(LOG_TAG, "fireNdefEvent fail: " + ex);
}
}


  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @ReactMethod
  public void getFilePort(String path, int baudRate, Promise promise) {
    return 1;
  }

  @ReactMethod
  public void getUSBPort(int vendorId, int productId, int baudRate, Promise promise) {
    return 2;
  }

}
