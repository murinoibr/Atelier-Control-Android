package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val category: String = "",
    val code: String = "",           // Nº de Registro: ex: "956", "S/N", "1000", "Destaque"
    val dimensions: String = "",     // Dimensões em cm: ex: "42 × 32 cm"
    val year: String = "",           // Ano de produção: ex: "2024", "2025"
    val status: String = "DISPONÍVEL", // "DISPONÍVEL" ou "VENDIDO"
    val technique: String = "",      // Técnica: ex: "Pintura Original", "Gravura Papel Especial", "Gravura com Moldura"
    val notes: String = "",          // Observações: ex: "Obra em Destaque", "Adição manual"
    val artist: String = "Jonas Lemes",
    val createdAt: Long = System.currentTimeMillis(),
    val catalogItemId: Long? = null
)

