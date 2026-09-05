package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Transaction
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesWithItems(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleWithItemsById(saleId: Long): SaleWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    suspend fun deleteItemsBySaleId(saleId: Long)

    @Transaction
    suspend fun updateSaleWithItems(sale: Sale, items: List<SaleItem>) {
        updateSale(sale)
        deleteItemsBySaleId(sale.id)
        insertSaleItems(items.map { it.copy(id = 0, saleId = sale.id) })
    }

    @Delete
    suspend fun deleteSale(sale: Sale)

    @Query("UPDATE sales SET isCancelled = :isCancelled WHERE id = :saleId")
    suspend fun setSaleCancelled(saleId: Long, isCancelled: Boolean)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT COUNT(*) FROM sales")
    suspend fun getCount(): Int
}
