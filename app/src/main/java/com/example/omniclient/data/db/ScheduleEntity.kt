package com.example.omniclient.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val week: Int, // относительный номер недели: -1, 0, 1
    val scheduleJson: String,
    val updatedAt: Long,
    val divisionId: Int = 0 // 0 - оба, 458 - колледж, 74 - академия
) 