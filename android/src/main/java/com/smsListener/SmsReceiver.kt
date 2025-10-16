package com.smsListener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.util.UUID

/**
 * SmsReceiver:
 * - Always registered in AndroidManifest so it receives SMS while app is closed.
 * - Reads SharedPreferences keys:
 *     PREFS_NAME = "sms_receipt_prefs"
 *     KEY_CAPTURE_ENABLED = "capture_enabled" (boolean)
 *     KEY_CAPTURE_CONTACT = "capture_contact" (string: contact name or phone fragment)
 *     KEY_CACHE = "sms_cache" (JSON array string)
 *
 * Behavior:
 * - For each incoming SMS PDU, construct SmsMessage safely (with 'format' if provided).
 * - If capture is enabled and the sender or body matches the configured contact identifier,
 *   append the raw receipt object to the native cache (SharedPreferences JSON array).
 *
 * Notes:
 * - This implementation uses SharedPreferences + JSONArray for simplicity. Replace with
 *   Room/SQLite for higher volumes / robust persistence.
 * - Matching logic: default checks sender contains contactName OR message body contains contactName.
 *   You can improve to map contact name -> phone number in a later step.
 */

private const val TAG = "SmsReceiver"

class SmsReceiver : BroadcastReceiver() {

    companion object {
        const val PREFS_NAME = "sms_receipt_prefs"
        const val KEY_CAPTURE_ENABLED = "capture_enabled"
        const val KEY_CAPTURE_CONTACT = "capture_contact"
        const val KEY_CACHE = "sms_cache"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val bundle: Bundle? = intent.extras
            if (bundle == null) {
                Log.d(TAG, "No extras on intent")
                return
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_CAPTURE_ENABLED, false)
            val target = prefs.getString(KEY_CAPTURE_CONTACT, null)

            if (!enabled || target.isNullOrBlank()) {
                // capture not active, skip
                Log.d(TAG, "Capture disabled or no target configured")
                return
            }

            // PDUs may be an Object[] of byte[] or other wrapped forms depending on OEM
            val pdusObj = bundle["pdus"] as? Array<*>
            val format = bundle.getString("format") // may be null on some devices

            if (pdusObj == null) {
                Log.d(TAG, "No pdus in SMS bundle")
                return
            }

            // Iterate through PDUs; build full message(s)
            for (pdu in pdusObj) {
                try {
                    val bytes = when (pdu) {
                        is ByteArray -> pdu
                        is java.io.Serializable -> {
                            // Some OEMs wrap pdus in other containers; try casting to ByteArray
                            try {
                                pdu as ByteArray
                            } catch (e: Exception) {
                                Log.w(TAG, "Unexpected pdu type, skipping: ${pdu::class.java}")
                                continue
                            }
                        }
                        else -> {
                            Log.w(TAG, "Unknown pdu type ${pdu?.javaClass}")
                            continue
                        }
                    }

                    val sms = if (format != null) {
                        SmsMessage.createFromPdu(bytes, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(bytes)
                    }

                    val sender = sms.originatingAddress ?: ""
                    val body = sms.messageBody ?: ""
                    val timestamp = sms.timestampMillis

                    // Simple matching strategy: contact string appears in sender or in message body.
                    // You can later expand this to map a contact name to phone numbers.
                    if (matchesTarget(sender, body, target)) {
                        // Build a JSON object for storage
                        val obj = JSONObject().apply {
                            put("id", UUID.randomUUID().toString())
                            put("sender", sender)
                            put("body", body)
                            put("timestamp", timestamp)
                            put("capturedAt", System.currentTimeMillis())
                            put("matchedTarget", target)
                        }

                        appendToCache(prefs, obj)
                        Log.d(TAG, "Captured SMS from $sender matching target '$target'")
                    } else {
                        Log.v(TAG, "SMS from $sender does not match target '$target'")
                    }

                } catch (inner: Exception) {
                    Log.w(TAG, "Failed to parse PDU or process SMS: ${inner.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "onReceive error: ${e.message}")
        }
    }

    private fun matchesTarget(sender: String, body: String, target: String): Boolean {
        val normTarget = target.trim().lowercase()
        if (normTarget.isEmpty()) return false

        return sender.lowercase().contains(normTarget) || body.lowercase().contains(normTarget)
    }

    /**
     * Append JSONObject to JSONArray stored as string in SharedPreferences.
     * Use synchronization on prefs file to avoid concurrent writes from multiple receiver calls.
     */
    private fun appendToCache(prefs: SharedPreferences, obj: JSONObject) {
        synchronized(prefs) {
            try {
                val existing = prefs.getString(KEY_CACHE, null)
                val arr = if (existing.isNullOrBlank()) JSONArray() else JSONArray(existing)
                arr.put(obj)
                prefs.edit().putString(KEY_CACHE, arr.toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to append to cache: ${e.message}")
            }
        }
    }
}