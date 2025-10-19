package com.smsListener.storage


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface ReceiptDao {
@Insert
fun insert(receipt: ReceiptEntity): Long


@Query("SELECT body FROM stored_receipts ORDER BY captured_at ASC")
fun getAllBodies(): List<String>


@Query("DELETE FROM stored_receipts")
fun clearAll()
}