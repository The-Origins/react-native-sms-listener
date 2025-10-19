package com.smsListener

import com.facebook.react.bridge.*
import android.content.Context
import com.smsListener.storage.ReceiptDatabase
import java.util.concurrent.Executors

class SmsModule(private val reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private val executor = Executors.newSingleThreadExecutor()

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
}