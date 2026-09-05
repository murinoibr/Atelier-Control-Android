package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val customerName: String = "",
    val paymentMethod: String = "PIX", // "PIX", "Cartão de Crédito", "Cartão de Débito", "Dinheiro", "Outro"
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val isCancelled: Boolean = false,
    val notes: String = ""
) {
    val totalProfit: Double
        get() = totalAmount - totalCost
}
