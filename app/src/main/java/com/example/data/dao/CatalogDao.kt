package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CatalogItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items ORDER BY id ASC")
    fun getAllCatalogItems(): Flow<List<CatalogItem>>

    @Query("SELECT * FROM catalog_items ORDER BY id ASC")
    suspend fun getAllCatalogItemsList(): List<CatalogItem>

    @Query("SELECT * FROM catalog_items WHERE id = :id LIMIT 1")
    suspend fun getCatalogItemById(id: Long): CatalogItem?

    @Query("SELECT * FROM catalog_items WHERE code = :code LIMIT 1")
    suspend fun getCatalogItemByCode(code: String): CatalogItem?

    @Query("SELECT * FROM catalog_items WHERE registeredProductId = :productId LIMIT 1")
    suspend fun getCatalogItemByRegisteredProductId(productId: Long): CatalogItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogItem(item: CatalogItem): Long

    @Update
    suspend fun updateCatalogItem(item: CatalogItem)

    @Delete
    suspend fun deleteCatalogItem(item: CatalogItem)

    @Query("DELETE FROM catalog_items")
    suspend fun deleteAllCatalogItems()

    @Query("SELECT COUNT(*) FROM catalog_items")
    suspend fun getCount(): Int

    @Query("UPDATE catalog_items SET isIncludedInCadastros = :isIncluded, registeredProductId = :registeredProductId WHERE id = :id")
    suspend fun updateIncludedStatus(id: Long, isIncluded: Boolean, registeredProductId: Long?)

    @Query("UPDATE catalog_items SET isIncludedInCadastros = 0, registeredProductId = NULL")
    suspend fun resetAllIncludedStatus()
}
