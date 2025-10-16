package com.smsreceipts

import com.facebook.react.bridge.*
import android.content.Context

/**
 * SmsModule:
 * - startCapture(contactName: string): Promise<boolean>
 *     Stores capture config in SharedPreferences so SmsReceiver will start capturing matching messages.
 * - stopCapture(): Promise<boolean>
 *     Clears the config (disables capture).
 *
 * Both methods are synchronous wrt SharedPreferences; they resolve with true on success.
 */

class SmsModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val PREFS_NAME = SmsReceiver.PREFS_NAME
        const val KEY_CAPTURE_ENABLED = SmsReceiver.KEY_CAPTURE_ENABLED
        const val KEY_CAPTURE_CONTACT = SmsReceiver.KEY_CAPTURE_CONTACT
    }

    override fun getName(): String {
        return "SmsReceiptModule"
    }

    @ReactMethod
    fun startCapture(contactName: String, promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_CAPTURE_ENABLED, true)
                .putString(KEY_CAPTURE_CONTACT, contactName)
                .apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERR_START_CAPTURE", e)
        }
    }

    @ReactMethod
    fun stopCapture(promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(KEY_CAPTURE_ENABLED, false)
                .remove(KEY_CAPTURE_CONTACT)
                .apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERR_STOP_CAPTURE", e)
        }
    }

    // Optional helper to check status from JS (not required but useful)
    @ReactMethod
    fun isCaptureActive(promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_CAPTURE_ENABLED, false)
            val contact = prefs.getString(KEY_CAPTURE_CONTACT, null)
            val map = Arguments.createMap()
            map.putBoolean("enabled", enabled)
            map.putString("contact", contact)
            promise.resolve(map)
        } catch (e: Exception) {
            promise.reject("ERR_STATUS", e)
        }
    }

    @ReactMethod
    fun getStoredReceipts(promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(SmsReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(SmsReceiver.KEY_CACHE, null)

            val arr = Arguments.createArray()

            if (!jsonString.isNullOrBlank()) {
                val jsonArray = org.json.JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val map = Arguments.createMap()

                    map.putString("id", obj.optString("id"))
                    map.putString("sender", obj.optString("sender"))
                    map.putString("body", obj.optString("body"))
                    map.putDouble("timestamp", obj.optLong("timestamp").toDouble())
                    map.putDouble("capturedAt", obj.optLong("capturedAt").toDouble())
                    map.putString("matchedTarget", obj.optString("matchedTarget"))

                    arr.pushMap(map)
                }
            }

            promise.resolve(arr)
        } catch (e: Exception) {
            promise.reject("ERR_FETCH_RECEIPTS", e)
        }
    }

    @ReactMethod
    fun clearStoredReceipts(promise: Promise) {
        try {
            val prefs = reactContext.getSharedPreferences(SmsReceiver.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(SmsReceiver.KEY_CACHE).apply()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("ERR_CLEAR_RECEIPTS", e)
        }
    }
}