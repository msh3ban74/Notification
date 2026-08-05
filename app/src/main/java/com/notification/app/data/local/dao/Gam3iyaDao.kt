package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.Gam3iyaEntity
import com.notification.app.data.local.entities.Gam3iyaMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface Gam3iyaDao {
    @Query("SELECT * FROM gam3iyas ORDER BY startDate DESC")
    fun getAllGam3iyas(): Flow<List<Gam3iyaEntity>>

    @Query("SELECT * FROM gam3iyas WHERE id = :id")
    suspend fun getGam3iyaById(id: Long): Gam3iyaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGam3iya(gam3iya: Gam3iyaEntity): Long

    @Delete
    suspend fun deleteGam3iya(gam3iya: Gam3iyaEntity)

    @Query("SELECT * FROM gam3iya_members WHERE gam3iyaId = :gam3iyaId ORDER BY turnMonth ASC")
    fun getMembersForGam3iya(gam3iyaId: Long): Flow<List<Gam3iyaMemberEntity>>

    // Sprint 6 — Executive Dashboard. Read-only query so the dashboard's
    // Gam3iya payment widget can observe upcoming payouts; no logic change.
    @Query("SELECT * FROM gam3iya_members ORDER BY payoutDate ASC")
    fun getAllMembers(): Flow<List<Gam3iyaMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Gam3iyaMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Gam3iyaMemberEntity>)

    @Update
    suspend fun updateMember(member: Gam3iyaMemberEntity)

    @Delete
    suspend fun deleteMember(member: Gam3iyaMemberEntity)
}
