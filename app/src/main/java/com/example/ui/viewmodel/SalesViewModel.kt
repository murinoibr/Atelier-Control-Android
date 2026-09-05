package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CartItem
import com.example.data.SalesRepository
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.OfficialCatalog
import com.example.data.model.CatalogItem
import com.example.data.model.toCatalogItem

class SalesViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = SalesRepository(database.productDao(), database.saleDao(), database.catalogDao())

    init {
        viewModelScope.launch {
            try {
                // Ensure Official Catalog items are populated in the catalog table
                if (database.catalogDao().getCount() == 0) {
                    val items = OfficialCatalog.getOfficialCatalog().map { it.toCatalogItem() }
                    repository.insertCatalogItems(items)
                }

                // If previous versions populated catalog items directly into productDao,
                // remove them so they do NOT clutter "produtos principais" (Cadastros / Vendas / Gerenciar)
                val allProds = database.productDao().getAllProductsList()
                val catalogCodes = OfficialCatalog.getOfficialCatalog().mapNotNull { it.code.ifBlank { null } }.toSet()
                for (prod in allProds) {
                    if (prod.name != "Quadro" && prod.name != "Ímã" && prod.catalogItemId == null && catalogCodes.contains(prod.code)) {
                        database.productDao().deleteProduct(prod)
                    }
                }
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val catalogItems: StateFlow<List<CatalogItem>> = repository.allCatalogItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sales: StateFlow<List<SaleWithItems>> = repository.allSales
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Cart in POS / Vendas
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: StateFlow<Double> = _cart
        .combine(_cart) { cartList, _ -> cartList.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = _cart
        .combine(_cart) { cartList, _ -> cartList.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI Events
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    fun addToCart(product: Product) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == product.id }

        if (index >= 0) {
            val currentItem = currentList[index]
            currentList[index] = currentItem.copy(quantity = currentItem.quantity + 1)
        } else {
            currentList.add(CartItem(product = product, quantity = 1))
        }
        _cart.value = currentList
    }

    fun decrementCartQuantity(productId: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val item = currentList[index]
            if (item.quantity > 1) {
                currentList[index] = item.copy(quantity = item.quantity - 1)
            } else {
                currentList.removeAt(index)
            }
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun finalizeSale(
        customerName: String,
        paymentMethod: String,
        discount: Double,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        val items = _cart.value
        if (items.isEmpty()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowSnackbar("O carrinho está vazio"))
            }
            return
        }

        viewModelScope.launch {
            val result = repository.completeSale(
                cartItems = items,
                customerName = customerName,
                paymentMethod = paymentMethod,
                discount = discount,
                notes = notes
            )
            if (result.isSuccess) {
                val saleId = result.getOrThrow()
                clearCart()
                _eventFlow.emit(UiEvent.ShowSnackbar("Venda #$saleId realizada com sucesso!"))
                onSuccess(saleId)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erro ao registrar venda"
                _eventFlow.emit(UiEvent.ShowSnackbar(errorMsg))
            }
        }
    }

    fun completeSale(
        customerName: String,
        paymentMethod: String,
        discount: Double,
        notes: String,
        onSuccess: (Long) -> Unit
    ) {
        finalizeSale(customerName, paymentMethod, discount, notes, onSuccess)
    }

    fun saveProduct(
        product: Product,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (product.id == 0L) {
                    repository.insertProduct(product)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Produto '${product.name}' cadastrado com sucesso!"))
                } else {
                    repository.updateProduct(product)
                    _eventFlow.emit(UiEvent.ShowSnackbar("Produto '${product.name}' atualizado!"))
                }
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao salvar produto: ${e.message}"))
            }
        }
    }



    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _eventFlow.emit(UiEvent.ShowSnackbar("Produto '${product.name}' removido"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Não foi possível excluir o produto: ${e.message}"))
            }
        }
    }

    fun deleteAllProducts(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.deleteAllProducts()
                _eventFlow.emit(UiEvent.ShowSnackbar("Todos os produtos foram removidos"))
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao limpar produtos: ${e.message}"))
            }
        }
    }

    fun restoreOfficialCatalog(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                database.catalogDao().deleteAllCatalogItems()
                val items = OfficialCatalog.getOfficialCatalog().map { it.toCatalogItem() }
                repository.insertCatalogItems(items)
                _eventFlow.emit(UiEvent.ShowSnackbar("Catálogo Oficial Nº 1023 (Jonas Lemes) restaurado com sucesso!"))
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao restaurar catálogo: ${e.message}"))
            }
        }
    }

    /**
     * Inclui um produto do Catálogo nos Produtos Cadastrados (Cadastros / Produtos Principais).
     */
    fun includeCatalogItemInCadastros(catalogItem: CatalogItem, onSuccess: (Product) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val registeredProduct = repository.includeCatalogItemInCadastros(catalogItem)
                _eventFlow.emit(UiEvent.ShowSnackbar("'${catalogItem.name}' incluído nos cadastros com sucesso!"))
                onSuccess(registeredProduct)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao incluir nos cadastros: ${e.message}"))
            }
        }
    }

    /**
     * Remove um produto do catálogo dos produtos cadastrados principais (mantendo-o no catálogo).
     */
    fun removeCatalogItemFromCadastros(catalogItem: CatalogItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.removeCatalogItemFromCadastros(catalogItem)
                _eventFlow.emit(UiEvent.ShowSnackbar("'${catalogItem.name}' removido dos cadastros"))
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao remover dos cadastros: ${e.message}"))
            }
        }
    }

    fun toggleCatalogItemStatus(item: CatalogItem) {
        val newStatus = if (item.status.equals("DISPONÍVEL", ignoreCase = true)) "VENDIDO" else "DISPONÍVEL"
        viewModelScope.launch {
            try {
                repository.updateCatalogItem(item.copy(status = newStatus))
                // If this item was also registered in products, sync status there too
                if (item.registeredProductId != null) {
                    val prod = database.productDao().getProductById(item.registeredProductId)
                    if (prod != null) {
                        repository.updateProduct(prod.copy(status = newStatus))
                    }
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("'${item.name}' marcado como $newStatus"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao atualizar status: ${e.message}"))
            }
        }
    }

    fun saveCatalogItem(item: CatalogItem, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.updateCatalogItem(item)
                // If it's in products, update there too
                if (item.registeredProductId != null) {
                    val prod = database.productDao().getProductById(item.registeredProductId)
                    if (prod != null) {
                        repository.updateProduct(
                            prod.copy(
                                name = item.name,
                                price = item.price,
                                category = item.category,
                                code = item.code,
                                dimensions = item.dimensions,
                                year = item.year,
                                status = item.status,
                                technique = item.technique,
                                notes = item.notes
                            )
                        )
                    }
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Obra '${item.name}' atualizada com sucesso!"))
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao salvar obra: ${e.message}"))
            }
        }
    }

    fun prepareCatalogItemForSale(catalogItem: CatalogItem, onReady: (Product) -> Unit) {
        viewModelScope.launch {
            try {
                val product = if (catalogItem.isIncludedInCadastros && catalogItem.registeredProductId != null) {
                    database.productDao().getProductById(catalogItem.registeredProductId)
                        ?: repository.includeCatalogItemInCadastros(catalogItem)
                } else {
                    repository.includeCatalogItemInCadastros(catalogItem)
                }
                onReady(product)
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao preparar produto para venda: ${e.message}"))
            }
        }
    }

    fun toggleProductStatus(product: Product) {
        val newStatus = if (product.status.equals("DISPONÍVEL", ignoreCase = true)) "VENDIDO" else "DISPONÍVEL"
        viewModelScope.launch {
            try {
                repository.updateProduct(product.copy(status = newStatus))
                _eventFlow.emit(UiEvent.ShowSnackbar("'${product.name}' marcado como $newStatus"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao atualizar status: ${e.message}"))
            }
        }
    }

    fun registerDirectSale(
        product: Product,
        paymentMethod: String,
        discount: Double = 0.0,
        notes: String = "",
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val items = listOf(CartItem(product = product, quantity = 1))
            val result = repository.completeSale(
                cartItems = items,
                customerName = "Cliente Balcão",
                paymentMethod = paymentMethod,
                discount = discount,
                notes = notes
            )
            if (result.isSuccess) {
                val saleId = result.getOrThrow()
                _eventFlow.emit(UiEvent.ShowSnackbar("Venda registrada com sucesso!"))
                onSuccess(saleId)
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Erro ao registrar venda"
                _eventFlow.emit(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    fun cancelSale(saleId: Long) {
        viewModelScope.launch {
            val result = repository.cancelSale(saleId)
            if (result.isSuccess) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Venda #$saleId cancelada e itens devolvidos ao estoque."))
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Erro ao cancelar venda"
                _eventFlow.emit(UiEvent.ShowSnackbar(msg))
            }
        }
    }

    fun updateSale(
        sale: Sale,
        items: List<SaleItem>? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (items != null) {
                    repository.updateSaleWithItems(sale, items)
                } else {
                    repository.updateSale(sale)
                }
                _eventFlow.emit(UiEvent.ShowSnackbar("Venda #${sale.id} atualizada com sucesso!"))
                onSuccess()
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao atualizar venda: ${e.message}"))
            }
        }
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            try {
                repository.deleteSale(sale)
                _eventFlow.emit(UiEvent.ShowSnackbar("Registro da venda #${sale.id} excluído"))
            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Erro ao excluir venda: ${e.message}"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
    }
}
