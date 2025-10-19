package com.smsListener.storage


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "stored_receipts")
data class ReceiptEntity(
@PrimaryKey(autoGenerate = true) val id: Long = 0,
@ColumnInfo(name = "body") val body: String,
@ColumnInfo(name = "captured_at") val capturedAt: Long
)