package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String, // e.g. "invoice", "address", "chat", etc.
    val name: String, // e.g. "Faturalar"
    val iconName: String, // Material icon identifier
    val colorHex: String, // Hex color code
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = true,
    val isCustom: Boolean = false,
    val description: String = ""
)
