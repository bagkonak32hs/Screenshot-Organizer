package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index("screenshotId"), Index("name")]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val name: String
)

@Entity(
    tableName = "reminders",
    indices = [Index("screenshotId")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val title: String,
    val reminderDate: Long, // timestamp
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "analysis_results",
    indices = [Index("screenshotId")]
)
data class AnalysisResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val rawJson: String,
    val detectedFields: String,
    val analyzedAt: Long = System.currentTimeMillis(),
    val modelVersion: String = "gemini-3.5-flash"
)
