package com.smsListener

import com.facebook.react.bridge.*
import android.content.Context
import com.smsListener.storage.ReceiptDatabase
import java.util.concurrent.Executors
import com.facebook.react.modules.core.DeviceEventManagerModule

class SmsModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  

  companion object {
    // static holder so SmsReceiver can emit events if JS bridge is active
    @JvmStatic
    var reactContextStatic: ReactApplicationContext? = null

    /**
     * Safely emit an event to JS if the bridge is active.
     * No-op if JS runtime isn't ready (avoids deprecation & crash).
     */
    @JvmStatic
    fun emitEvent(eventName: String, params: WritableMap?) {
      val ctx = reactContextStatic ?: return
      try {
        if (ctx.hasActiveCatalystInstance()) {
          ctx
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
          Log.d(TAG, "Event emitted to JS: $eventName")
        } else {
          Log.v(TAG, "Bridge not active — skipped emit: $eventName")
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to emit event '$eventName': ${e.message}")
      }
    }
  }

  private val executor = Executors.newSingleThreadExecutor()

  init {
    // Save ReactApplicationContext for other native classes (like BroadcastReceiver)
    reactContextStatic = reactContext
  }

  override fun getName(): String = "SmsListenerModule"

  @ReactMethod
  fun startCapture(contactName: String, storeCondition: String, promise: Promise) {
    try {
      val prefs = reactContext.getSharedPreferences(SmsReceiver.PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putBoolean(SmsReceiver.KEY_CAPTURE_ENABLED, true)
        .putString(SmsReceiver.KEY_CAPTURE_CONTACT, contactName)
        .putString(SmsReceiver.KEY_STORE_REGEX, storeCondition)
        .apply()
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("ERR_START_CAPTURE", e)
    }
  }

  @ReactMethod
  fun stopCapture(promise: Promise) {
    try {
      val prefs = reactContext.getSharedPreferences(SmsReceiver.PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putBoolean(SmsReceiver.KEY_CAPTURE_ENABLED, false)
        .remove(SmsReceiver.KEY_CAPTURE_CONTACT)
        .remove(SmsReceiver.KEY_STORE_REGEX)
        .apply()
      promise.resolve(true)
    } catch (e: Exception) {
      promise.reject("ERR_STOP_CAPTURE", e)
    }
  }

  @ReactMethod
  fun isCaptureActive(promise: Promise) {
    try {
      val prefs = reactContext.getSharedPreferences(SmsReceiver.PREFS_NAME, Context.MODE_PRIVATE)
      val active = prefs.getBoolean(SmsReceiver.KEY_CAPTURE_ENABLED, false)
      promise.resolve(active)
    } catch (e: Exception) {
      promise.reject("ERR_IS_CAPTURE_ACTIVE", e)
    }
  }

  @ReactMethod
  fun getStoredReceipts(promise: Promise) {
    executor.execute {
      try {
        val db = ReceiptDatabase.getInstance(reactContext)
        val bodies = db.receiptDao().getAllBodies()
        val arr = Arguments.createArray()
        bodies.forEach { b -> arr.pushString(b) }
        promise.resolve(arr)
      } catch (e: Exception) {
        promise.reject("ERR_GET_RECEIPTS", e)
      }
    }
  }

  @ReactMethod
  fun clearStoredReceipts(promise: Promise) {
    executor.execute {
      try {
        val db = ReceiptDatabase.getInstance(reactContext)
        db.receiptDao().clearAll()
        promise.resolve(true)
      } catch (e: Exception) {
        promise.reject("ERR_CLEAR_RECEIPTS", e)
      }
    }
  }

  /** Delete a single receipt by id, resolves true if deleted (rows > 0) */
  @ReactMethod
  fun deleteReceipt(id: Double, promise: Promise) {
    // RN numbers are doubles; convert to Long
    val longId = id.toLong()
    executor.execute {
      try {
        val db = ReceiptDatabase.getInstance(reactContext)
        val deleted = db.receiptDao().deleteById(longId)
        promise.resolve(deleted > 0)
      } catch (e: Exception) {
        promise.reject("ERR_DELETE_RECEIPT", e)
      }
    }
  }
}