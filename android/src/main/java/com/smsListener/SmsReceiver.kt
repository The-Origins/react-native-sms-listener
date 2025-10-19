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
import java.security.MessageDigest
import java.util.concurrent.Executors


private const val TAG = "SmsReceiver"

private fun String.toMD5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}


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

            val messages = mutableListOf<SmsMessage>()
            for (pdu in pdusObj) {
                val bytes = pdu as? ByteArray ?: continue
                val sms = if (format != null) {
                    SmsMessage.createFromPdu(bytes, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(bytes)
                }
                messages.add(sms)
            }

            if (messages.isEmpty()) return

            // Combine all parts into one complete message
            val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }
            val sender = messages.first().originatingAddress ?: ""

            // Check both sender/contact and regex on fullBody
            val contactMatch = sender.contains(contactName, ignoreCase = true)
                    || fullBody.contains(contactName, ignoreCase = true)
            val regexMatch = pattern.containsMatchIn(fullBody)

            if (contactMatch && regexMatch) {
                executor.execute {
                    try {
                        val db = ReceiptDatabase.getInstance(context)
                        val hash = fullBody.toMD5()
                        val exists = db.receiptDao().exists(hash)
                        if(exists == null){
                            db.receiptDao().insert(
                                ReceiptEntity(
                                    body = fullBody,
                                    capturedAt = System.currentTimeMillis(),
                                    hash = hash
                                )
                            )
                            Log.d(TAG, "stored full message from $sender matching contact '$contactName'")
                        }else{
                            Log.d(TAG, "duplicate message ignored")
                        }
                    } catch (dbEx: Exception) {
                        Log.e(TAG, "db insert error: ${dbEx.message}")
                    }
                }
            } else {
                Log.v(TAG, "message from $sender did not match contact/regex")
            }
        
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error: ${e.message}")
        }
    }
}
