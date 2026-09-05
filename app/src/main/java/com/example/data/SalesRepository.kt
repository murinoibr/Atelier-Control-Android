package com.example.data

import com.example.data.dao.CatalogDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.model.CatalogItem
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double
        get() = product.price * quantity
}

class SalesRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val catalogDao: CatalogDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItems()
    val allCatalogItems: Flow<List<CatalogItem>> = catalogDao.getAllCatalogItems()

    suspend fun insertProduct(product: Product): Long = withContext(Dispatchers.IO) {
        productDao.insertProduct(product)
    }

    suspend fun insertProducts(products: List<Product>) = withContext(Dispatchers.IO) {
        productDao.insertAll(products)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        if (product.catalogItemId != null) {
            catalogDao.updateIncludedStatus(product.catalogItemId, false, null)
        } else {
            val matchingCatalog = catalogDao.getCatalogItemByRegisteredProductId(product.id)
            if (matchingCatalog != null) {
                catalogDao.updateIncludedStatus(matchingCatalog.id, false, null)
            }
        }
        productDao.deleteProduct(product)
    }

    suspend fun deleteAllProducts() = withContext(Dispatchers.IO) {
        productDao.deleteAllProducts()
        catalogDao.resetAllIncludedStatus()
    }

    // ================= CATALOG MANAGEMENT =================

    suspend fun insertCatalogItems(items: List<CatalogItem>) = withContext(Dispatchers.IO) {
        catalogDao.insertAll(items)
    }

    suspend fun insertCatalogItem(item: CatalogItem): Long = withContext(Dispatchers.IO) {
        catalogDao.insertCatalogItem(item)
    }

    suspend fun updateCatalogItem(item: CatalogItem) = withContext(Dispatchers.IO) {
        catalogDao.updateCatalogItem(item)
    }

    suspend fun deleteCatalogItem(item: CatalogItem) = withContext(Dispatchers.IO) {
        catalogDao.deleteCatalogItem(item)
    }

    /**
     * Includes a catalog item into the main registered products ("Cadastros").
     * Inserts into `products` table and updates `catalog_items` tracking fields.
     */
    suspend fun includeCatalogItemInCadastros(catalogItem: CatalogItem): Product = withContext(Dispatchers.IO) {
        // Check if already registered
        if (catalogItem.isIncludedInCadastros && catalogItem.registeredProductId != null) {
            val existing = productDao.getProductById(catalogItem.registeredProductId)
            if (existing != null) return@withContext existing
        }

        // Check if matching by code or catalogItemId
        val existingByCatalogId = productDao.getProductByCatalogItemId(catalogItem.id)
        if (existingByCatalogId != null) {
            catalogDao.updateIncludedStatus(catalogItem.id, true, existingByCatalogId.id)
            return@withContext existingByCatalogId
        }

        val newProduct = Product(
            id = 0,
            name = catalogItem.name,
            price = catalogItem.price,
            category = catalogItem.category,
            code = catalogItem.code,
            dimensions = catalogItem.dimensions,
            year = catalogItem.year,
            status = catalogItem.status,
            technique = catalogItem.technique,
            notes = catalogItem.notes,
            artist = catalogItem.artist,
            catalogItemId = catalogItem.id
        )
        val generatedId = productDao.insertProduct(newProduct)
        val savedProduct = newProduct.copy(id = generatedId)

        catalogDao.updateIncludedStatus(
            id = catalogItem.id,
            isIncluded = true,
            registeredProductId = generatedId
        )

        savedProduct
    }

    /**
     * Removes a catalog item from main registered products ("Cadastros") while keeping it in the Catalog.
     */
    suspend fun removeCatalogItemFromCadastros(catalogItem: CatalogItem) = withContext(Dispatchers.IO) {
        val prodId = catalogItem.registeredProductId
        if (prodId != null) {
            val prod = productDao.getProductById(prodId)
            if (prod != null) {
                productDao.deleteProduct(prod)
            }
        } else {
            val prod = productDao.getProductByCatalogItemId(catalogItem.id)
            if (prod != null) {
                productDao.deleteProduct(prod)
            }
        }
        catalogDao.updateIncludedStatus(catalogItem.id, false, null)
    }

    suspend fun completeSale(
        cartItems: List<CartItem>,
        customerName: String,
        paymentMethod: String,
        discount: Double,
        notes: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("O carrinho está vazio"))
        }

        val subtotal = cartItems.sumOf { it.subtotal }
        val finalAmount = (subtotal - discount).coerceAtLeast(0.0)

        val sale = Sale(
            timestamp = System.currentTimeMillis(),
            customerName = customerName.trim(),
            paymentMethod = paymentMethod,
            discount = discount,
            totalAmount = finalAmount,
            totalCost = 0.0,
            isCancelled = false,
            notes = notes.trim()
        )

        val saleId = saleDao.insertSale(sale)

        val saleItems = cartItems.map { item ->
            SaleItem(
                saleId = saleId,
                productId = item.product.id,
                productName = item.product.name,
                unitPrice = item.product.price,
                unitCost = 0.0,
                quantity = item.quantity,
                subtotal = item.subtotal
            )
        }
        saleDao.insertSaleItems(saleItems)

        Result.success(saleId)
    }

    suspend fun cancelSale(saleId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val saleWithItems = saleDao.getSaleWithItemsById(saleId)
            ?: return@withContext Result.failure(IllegalArgumentException("Venda não encontrada"))

        if (saleWithItems.sale.isCancelled) {
            return@withContext Result.failure(IllegalStateException("Esta venda já está cancelada"))
        }

        saleDao.setSaleCancelled(saleId, true)
        Result.success(Unit)
    }

    suspend fun updateSale(sale: Sale) = withContext(Dispatchers.IO) {
        saleDao.updateSale(sale)
    }

    suspend fun updateSaleWithItems(sale: Sale, items: List<SaleItem>) = withContext(Dispatchers.IO) {
        saleDao.updateSaleWithItems(sale, items)
    }

    suspend fun deleteSale(sale: Sale) = withContext(Dispatchers.IO) {
        saleDao.deleteSale(sale)
    }
}

