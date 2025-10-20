package com.smsListener.storage


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy


@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(receipt: ReceiptEntity): Long


    @Query("SELECT body FROM stored_receipts ORDER BY captured_at ASC")
    fun getAllBodies(): List<String>

    @Query("SELECT hash FROM stored_receipts WHERE hash = :hash LIMIT 1")
    fun exists(hash: String): String?


    @Query("DELETE FROM stored_receipts")
    fun clearAll()
}