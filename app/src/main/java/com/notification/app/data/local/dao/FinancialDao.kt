package com.notification.app.data.local.dao

import androidx.room.*
import com.notification.app.data.local.entities.FinancialItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_items ORDER BY dueDate ASC")
    fun getAll(): Flow<List<FinancialItemEntity>>

    @Query("SELECT * FROM financial_items WHERE id = :id")
    suspend fun getById(id: Long): FinancialItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FinancialItemEntity): Long

    @Update
    suspend fun update(item: FinancialItemEntity)

    @Delete
    suspend fun delete(item: FinancialItemEntity)
}
