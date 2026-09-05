package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel

@Composable
fun ProductRegistrationScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val scrollState = rememberScrollState()

    var nameInput by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var categoryInput by remember { mutableStateOf("") }

    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val quickCategories = listOf("Ímas", "Obras de Arte")

    val groupedProducts = remember(products) {
        products.groupBy { it.category.ifBlank { "Sem Categoria" } }
    }

    Scaffold(
        containerColor = IosBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar with centered title and red trash icon on top right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Cadastrar Produtos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Red trash icon on top right
                IconButton(
                    onClick = { showDeleteAllDialog = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .testTag("delete_all_products_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Limpar Produtos",
                        tint = ActionRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info banner about Catalog integration
            Surface(
                color = Color(0xFF1E1528),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CollectionsBookmark,
                        contentDescription = null,
                        tint = AccentMagenta,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Obras do Catálogo Oficial não aparecem aqui automaticamente. Para adicioná-las, abra a aba 'Catálogo' e clique em '+ Cadastrar' na obra desejada.",
                        color = Color(0xFFE9D5FF),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= CARD 1: NOVO PRODUTO =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = AccentMagenta,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Novo Produto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Nome do Produto
                Text(
                    text = "Nome do Produto",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = { Text("Ex: Colar Dourado", color = TextMuted, fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Preço (R$)
                Text(
                    text = "Preço (R$)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    placeholder = { Text("Ex: 200,52", color = TextMuted, fontSize = 14.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_price_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Categoria
                Text(
                    text = "Categoria",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = categoryInput,
                    onValueChange = { categoryInput = it },
                    placeholder = { Text("Ex: Joias, Bijuterias...", color = TextMuted, fontSize = 14.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_category_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Category Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickCategories.forEach { cat ->
                        val isSelected = categoryInput == cat
                        Surface(
                            color = if (isSelected) MagentaContainer else Color(0xFF222226),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AccentMagenta else Color(0xFF2E2E32)
                            ),
                            modifier = Modifier
                                .clickable { categoryInput = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MagentaPillText else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // "+ Cadastrar Produto" Button
                Surface(
                    color = Color(0xFF27272A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            val priceVal = priceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                            if (nameInput.isNotBlank() && priceVal > 0) {
                                viewModel.saveProduct(
                                    Product(
                                        id = 0,
                                        name = nameInput.trim(),
                                        category = categoryInput.trim().ifBlank { "Geral" },
                                        price = priceVal
                                    )
                                )
                                nameInput = ""
                                priceInput = ""
                                categoryInput = ""
                            }
                        }
                        .testTag("submit_product_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cadastrar Produto",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= CARD 2: PRODUTOS CADASTRADOS =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Produtos Cadastrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (products.isEmpty()) {
                    Text(
                        text = "Nenhum produto cadastrado ainda.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        groupedProducts.forEach { (categoryName, productList) ->
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Category Header: [Category Name] [N itens badge]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = categoryName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    // Purple Pill Badge: "X itens"
                                    Surface(
                                        color = MagentaContainer,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    ) {
                                        Text(
                                            text = "${productList.size} itens",
                                            color = MagentaPillText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                // Product List under this Category
                                productList.forEach { product ->
                                    Surface(
                                        color = Color(0xFF1C1C1E),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = product.name,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 15.sp,
                                                        color = TextPrimary
                                                    )
                                                    if (product.catalogItemId != null) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = Color(0xFF2E1065),
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = if (product.code.isNotBlank()) "Catálogo Nº ${product.code}" else "Catálogo",
                                                                color = Color(0xFFD8B4FE),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = formatCurrency(product.price),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = NeonGreen
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Category Tag
                                                CategoryBadge(category = product.category)

                                                // Purple Edit Pencil Button
                                                Surface(
                                                    color = MagentaContainer,
                                                    shape = CircleShape,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clickable { productToEdit = product }
                                                        .testTag("edit_product_icon_${product.id}")
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Editar",
                                                            tint = AccentMagenta,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Edit Product Dialog
    // Edit Product Dialog (All Fields)
    if (productToEdit != null) {
        EditProductDialog(
            product = productToEdit!!,
            onDismiss = { productToEdit = null },
            onSave = { updatedProduct ->
                viewModel.saveProduct(updatedProduct)
                productToEdit = null
            }
        )
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = IosSurfaceElevated,
            title = {
                Text(
                    text = "Limpar Todos os Produtos?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Deseja remover todos os produtos cadastrados? Esta operação não poderá ser revertida.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllProducts()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionRed)
                ) {
                    Text("Limpar Tudo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
