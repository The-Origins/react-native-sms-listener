package com.smsListener


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.telephony.SmsMessage
import android.util.Log
import com.smsListener.storage.ReceiptDatabase
import com.smsListener.storage.ReceiptEntity
import java.util.concurrent.Executors


private const val TAG = "SmsReceiver"


class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val PREFS_NAME = "sms_receipt_prefs"
        const val KEY_CAPTURE_ENABLED = "capture_enabled"
        const val KEY_CAPTURE_CONTACT = "capture_contact"
        const val KEY_STORE_REGEX = "store_regex"
    }


    private val executor = Executors.newSingleThreadExecutor()

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val bundle: Bundle? = intent.extras
            if (bundle == null) {
                Log.d(TAG, "no extras in intent")
                return
            }


            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean(KEY_CAPTURE_ENABLED, false)
            val contactName = prefs.getString(KEY_CAPTURE_CONTACT, null)
            val regexString = prefs.getString(KEY_STORE_REGEX, null)


            if (!enabled || contactName.isNullOrBlank() || regexString.isNullOrBlank()) {
                Log.d(TAG, "capture disabled or config missing")
                return
            }

            // compile regex safely
            val pattern = try {
                Regex(regexString)
            } catch (e: Exception) {
                Log.w(TAG, "invalid regex: $regexString")
                return
            }


            val pdusObj = bundle["pdus"] as? Array<*>
            val format = bundle.getString("format")
            if (pdusObj == null) {
                Log.d(TAG, "no pdus")
                return
            }

            for (pdu in pdusObj) {
                try {
                    val bytes = when (pdu) {
                        is ByteArray -> pdu
                        else -> continue
                    }


                    val sms = if (format != null) {
                        SmsMessage.createFromPdu(bytes, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(bytes)
                    }

                    val sender = sms.originatingAddress ?: ""
                    val body = sms.messageBody ?: ""


                    // match: sender contains contactName (case-insensitive) AND regex matches body
                    val contactMatch = sender.lowercase().contains(contactName.lowercase()) || body.lowercase().contains(contactName.lowercase())
                    val regexMatch = pattern.containsMatchIn(body)

                    if (contactMatch && regexMatch) {
                        // persist the entire message body using Room on background thread
                        executor.execute {
                            try {
                                val db = ReceiptDatabase.getInstance(context)
                                db.receiptDao().insert(ReceiptEntity(body = body, capturedAt = System.currentTimeMillis()))
                                Log.d(TAG, "stored message from $sender matching contact '$contactName'")
                            } catch (dbEx: Exception) {
                                Log.e(TAG, "db insert error: ${dbEx.message}")
                            }
                        }
                    } else {
                        Log.v(TAG, "message from $sender did not match contact/regex")
                    }

                } catch (inner: Exception) {
                    Log.w(TAG, "pdu parse error: ${inner.message}")
                }
            }
        
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error: ${e.message}")
        }
    }
}
