package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalog_items")
data class CatalogItem(
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
    val isIncludedInCadastros: Boolean = false, // Indica se o item já foi importado/incluído nos produtos principais
    val registeredProductId: Long? = null      // ID do produto correspondente na tabela de produtos cadastrados
)

fun Product.toCatalogItem(isIncluded: Boolean = false, registeredId: Long? = null): CatalogItem =
    CatalogItem(
        name = name,
        price = price,
        category = category,
        code = code,
        dimensions = dimensions,
        year = year,
        status = status,
        technique = technique,
        notes = notes,
        artist = artist,
        isIncludedInCadastros = isIncluded,
        registeredProductId = registeredId
    )

fun CatalogItem.toProduct(): Product =
    Product(
        id = registeredProductId ?: 0L,
        name = name,
        price = price,
        category = category,
        code = code,
        dimensions = dimensions,
        year = year,
        status = status,
        technique = technique,
        notes = notes,
        artist = artist,
        catalogItemId = id
    )
