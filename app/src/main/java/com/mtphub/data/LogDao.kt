package com.mtphub.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mtphub.models.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogEntity)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC LIMIT 1000")
    fun getLogs(): Flow<List<LogEntity>>

    @Query("DELETE FROM logs")
    suspend fun clearLogs()
}
