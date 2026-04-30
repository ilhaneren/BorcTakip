package com.borc.takip.data.db

import androidx.room.TypeConverter
import com.borc.takip.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
