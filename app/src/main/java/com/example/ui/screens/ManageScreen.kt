package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.data.model.SaleWithItems
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel

@Composable
fun ManageScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Produtos, 1 = Vendas
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var saleToEdit by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToDelete by remember { mutableStateOf<SaleWithItems?>(null) }
    var selectedSaleDetails by remember { mutableStateOf<SaleWithItems?>(null) }

    Scaffold(
        containerColor = IosBackground,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // iOS Large Title
            Text(
                text = "Gerenciar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Tab Switcher: [Produtos] [Vendas]
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, IosCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    // Produtos Segment
                    Surface(
                        color = if (selectedTab == 0) AccentMagenta else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedTab = 0 }
                            .testTag("manage_tab_products")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (selectedTab == 0) Color.White else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Produtos",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (selectedTab == 0) Color.White else TextSecondary
                            )
                        }
                    }

                    // Vendas Segment
                    Surface(
                        color = if (selectedTab == 1) AccentMagenta else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedTab = 1 }
                            .testTag("manage_tab_sales")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color.White else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vendas",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = if (selectedTab == 1) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            if (selectedTab == 0) {
                // Products List
                if (products.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Inventory2,
                        title = "Nenhum produto cadastrado",
                        message = "Cadastre produtos na aba 'Cadastros' para gerenciá-los aqui."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(products, key = { it.id }) { product ->
                            ManageProductCard(
                                product = product,
                                onEdit = { productToEdit = product },
                                onDelete = { productToDelete = product }
                            )
                        }
                    }
                }
            } else {
                // Sales List
                if (sales.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.ReceiptLong,
                        title = "Nenhuma venda registrada",
                        message = "As vendas realizadas na aba 'Vendas' aparecerão aqui."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sales, key = { it.sale.id }) { saleWithItems ->
                            ManageSaleCard(
                                saleWithItems = saleWithItems,
                                onEditSale = { saleToEdit = saleWithItems },
                                onViewReceipt = { selectedSaleDetails = saleWithItems },
                                onCancelSale = { viewModel.cancelSale(saleWithItems.sale.id) },
                                onDeleteSale = { saleToDelete = saleWithItems }
                            )
                        }
                    }
                }
            }
        }
    }

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

    // Delete Product Confirmation
    if (productToDelete != null) {
        val prod = productToDelete!!
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            containerColor = IosSurfaceElevated,
            title = {
                Text(
                    text = "Excluir Produto?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Deseja realmente excluir '${prod.name}'? Esta ação não pode ser desfeita.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionRed)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Delete Sale Confirmation
    if (saleToDelete != null) {
        val s = saleToDelete!!
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            containerColor = IosSurfaceElevated,
            title = {
                Text(
                    text = "Excluir Registro de Venda?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Deseja remover permanentemente o registro da venda #${s.sale.id}?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSale(s.sale)
                        saleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActionRed)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // View Sale Receipt Dialog
    if (selectedSaleDetails != null) {
        ReceiptDetailsDialog(
            saleWithItems = selectedSaleDetails!!,
            onDismiss = { selectedSaleDetails = null },
            onEditSale = { saleToEdit = selectedSaleDetails }
        )
    }

    // Edit Sale Dialog (All Fields)
    if (saleToEdit != null) {
        EditSaleDialog(
            saleWithItems = saleToEdit!!,
            onDismiss = { saleToEdit = null },
            onSave = { updatedSale, updatedItems ->
                viewModel.updateSale(updatedSale, updatedItems)
                saleToEdit = null
            }
        )
    }
}

@Composable
fun ManageProductCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = IosSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IosCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Product Name & Category Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
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
                CategoryBadge(category = product.category)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Price in Neon Green (matching video)
            Text(
                text = formatCurrency(product.price),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: [Editar] [Excluir]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Editar Button
                Surface(
                    color = ActionBlueContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ActionBlue.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onEdit() }
                        .testTag("manage_edit_button_${product.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = ActionBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Editar",
                            color = ActionBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Excluir Button
                Surface(
                    color = ActionRedContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ActionRed.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onDelete() }
                        .testTag("manage_delete_button_${product.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ActionRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excluir",
                            color = ActionRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManageSaleCard(
    saleWithItems: SaleWithItems,
    onEditSale: () -> Unit,
    onViewReceipt: () -> Unit,
    onCancelSale: () -> Unit,
    onDeleteSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sale = saleWithItems.sale
    Surface(
        color = IosSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IosCardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Venda #${sale.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                PaymentMethodBadge(method = sale.paymentMethod)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${formatDateTime(sale.timestamp)} • ${saleWithItems.items.size} itens",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCurrency(sale.totalAmount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (sale.discount > 0) {
                        val discountLabel = if (sale.notes.isNotBlank() && sale.notes.contains("%")) {
                            sale.notes
                        } else {
                            "-${formatCurrency(sale.discount)}"
                        }
                        DiscountBadge(discountLabel = discountLabel)
                    }
                    if (sale.notes.isNotBlank() && !sale.notes.contains("%")) {
                        Surface(
                            color = Color(0xFF2E1065),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFC084FC).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = sale.notes,
                                color = Color(0xFFC084FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Editar Venda
                Surface(
                    color = MagentaContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable { onEditSale() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = AccentMagenta,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editar",
                            color = AccentMagenta,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Visualizar Recibo
                Surface(
                    color = Color(0xFF27272A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable { onViewReceipt() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Recibo",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Excluir
                Surface(
                    color = ActionRedContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable { onDeleteSale() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ActionRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Excluir",
                            color = ActionRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
