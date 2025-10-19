package com.smsListener.storage


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao


    companion object {
        @Volatile
        private var INSTANCE: ReceiptDatabase? = null


        fun getInstance(context: Context): ReceiptDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReceiptDatabase::class.java,
                    "sms_receipts_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}