package com.example.omniclient.data.db

import androidx.room.Entity

@Entity(tableName = "schedule", primaryKeys = ["username", "week"])
data class ScheduleEntity(
    val username: String,
    val week: Int, // относительный номер недели: -1, 0, 1
    val scheduleJson: String,
    val updatedAt: Long,
    val divisionId: Int = 0 // 0 - оба, 458 - колледж, 74 - академия
)