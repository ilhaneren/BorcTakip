package com.borc.takip.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val normalizedName: String = name.lowercase(Locale("tr", "TR")).trim(),
    val totalBalance: Double = 0.0, // pozitif = bize borçlu, negatif = biz borçluyuz
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
