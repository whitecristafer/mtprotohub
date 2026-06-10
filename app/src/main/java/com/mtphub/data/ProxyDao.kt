package com.mtphub.data

import androidx.room.*
import com.mtphub.models.ProxyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxies ORDER BY score DESC")
    fun getAllProxies(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies WHERE status = 'WORKING' ORDER BY score DESC LIMIT :limit")
    fun getTopWorkingProxies(limit: Int): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies WHERE url = :url")
    suspend fun getProxyByUrl(url: String): ProxyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProxies(proxies: List<ProxyEntity>)

    @Update
    suspend fun updateProxy(proxy: ProxyEntity)

    @Query("DELETE FROM proxies WHERE status = 'BLACKLISTED' OR lastSeen < :cutoffTime")
    suspend fun deleteOldProxies(cutoffTime: Long)

    @Query("DELETE FROM proxies")
    suspend fun clearAll()
}
