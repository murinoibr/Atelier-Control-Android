package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.data.model.Product
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleRegistrationScreen(
    viewModel: SalesViewModel,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.products.collectAsState()
    val scrollState = rememberScrollState()

    // Selected product(s) - default to first or selected
    var selectedProductIds by remember { mutableStateOf(setOf<Long>()) }

    // Auto-select "Quadro" (or first product) if available initially to match video
    LaunchedEffect(products) {
        if (selectedProductIds.isEmpty() && products.isNotEmpty()) {
            val quadro = products.find { it.name.contains("Quadro", ignoreCase = true) } ?: products.first()
            selectedProductIds = setOf(quadro.id)
        }
    }

    // Payment Method: "Cartão de Crédito", "Cartão de Débito", "PIX", "Dinheiro"
    var selectedPaymentMethod by remember { mutableStateOf("PIX") }
    val isCreditCard = selectedPaymentMethod == "Cartão de Crédito"
    val isPixOrCash = selectedPaymentMethod == "PIX" || selectedPaymentMethod == "Dinheiro"

    // Installments: "1x", "2x", "3x", etc.
    var selectedInstallments by remember { mutableStateOf("1x") }
    // Discount percentage (0 to 10)
    var discountPercent by remember { mutableIntStateOf(0) }

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var lastSaleSummary by remember { mutableStateOf("") }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    
    // Dedicated Product Search & Filter Picker state
    var showProductPicker by remember { mutableStateOf(false) }
    var pickerSearchQuery by remember { mutableStateOf("") }
    var pickerSelectedCategory by remember { mutableStateOf("Todas") }

    val categories = remember(products) {
        val cats = products.map { it.category.trim().ifBlank { "Geral" } }.distinct().sorted()
        listOf("Todas") + cats
    }

    val pickerFilteredProducts = remember(products, pickerSearchQuery, pickerSelectedCategory) {
        products.filter { product ->
            val matchesQuery = pickerSearchQuery.isBlank() ||
                product.name.contains(pickerSearchQuery, ignoreCase = true) ||
                product.code.contains(pickerSearchQuery, ignoreCase = true) ||
                product.category.contains(pickerSearchQuery, ignoreCase = true) ||
                product.dimensions.contains(pickerSearchQuery, ignoreCase = true)

            val matchesCategory = pickerSelectedCategory == "Todas" ||
                product.category.equals(pickerSelectedCategory, ignoreCase = true)

            matchesQuery && matchesCategory
        }
    }

    val selectedProducts = remember(products, selectedProductIds) {
        products.filter { it.id in selectedProductIds }
    }

    val originalPrice = remember(selectedProducts) {
        selectedProducts.sumOf { it.price }
    }

    val discountAmount = remember(originalPrice, discountPercent) {
        if (discountPercent > 0) (originalPrice * (discountPercent / 100.0)) else 0.0
    }

    val totalToPay = remember(originalPrice, discountAmount) {
        (originalPrice - discountAmount).coerceAtLeast(0.0)
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Top Bar with Center Title + Logo and History icon button on top-right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Centered Title & Monogram
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Atelier Control",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AtelierLogo(modifier = Modifier.padding(bottom = 2.dp))
                }

                // Top Right History Button (Clock in purple circle)
                Surface(
                    color = MagentaContainer,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(38.dp)
                        .clickable { onNavigateToHistory() }
                        .testTag("sales_history_icon_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Histórico",
                            tint = AccentMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ================= CARD 1: PRODUTO SELECIONADO =================
            IosCard {
                // Header with Inventory Icon, Title, and Action Buttons (Quick Add + Change/Search Product)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Produto",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Add Product Button (+)
                        Surface(
                            color = AccentMagenta,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { showQuickAddDialog = true }
                                .testTag("quick_add_product_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar Novo Produto",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (products.isEmpty()) {
                    Text(
                        text = "Nenhum produto cadastrado. Toque no + para adicionar.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    val currentSelectedProduct = selectedProducts.firstOrNull()

                    if (currentSelectedProduct != null) {
                        // Clean card displaying the currently selected product with a prominent "Trocar / Procurar" button
                        Surface(
                            color = Color(0xFF22172A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentMagenta),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProductPicker = true }
                                .testTag("selected_product_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Check Icon in Purple Circle
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(AccentMagenta),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (currentSelectedProduct.code.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFF27272A),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (currentSelectedProduct.code.startsWith("Nº") || currentSelectedProduct.code == "S/N" || currentSelectedProduct.code == "Destaque") currentSelectedProduct.code else "Nº ${currentSelectedProduct.code}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextSecondary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = currentSelectedProduct.name,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (currentSelectedProduct.dimensions.isNotBlank() || currentSelectedProduct.category.isNotBlank()) {
                                            val detail = listOf(currentSelectedProduct.dimensions, currentSelectedProduct.category)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • ")
                                            Text(
                                                text = detail,
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = formatCurrency(currentSelectedProduct.price),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentMagenta
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // "Editar" pill button
                                    Surface(
                                        color = MagentaContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clickable { productToEdit = currentSelectedProduct }
                                            .testTag("edit_selected_product_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar produto",
                                                tint = AccentMagenta,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Editar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentMagenta
                                            )
                                        }
                                    }

                                    // "Alterar / Procurar" pill button on the right
                                    Surface(
                                        color = Color(0xFF2E1F3D),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f)),
                                        modifier = Modifier.clickable { showProductPicker = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                tint = AccentMagenta,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Procurar",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AccentMagenta
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state button asking to choose a product
                        Surface(
                            color = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProductPicker = true }
                                .testTag("select_product_prompt_button")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = AccentMagenta,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Toque para procurar e selecionar um produto...",
                                        fontSize = 14.sp,
                                        color = TextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Helpful hint button to quickly browse / filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProductPicker = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${products.size} produto(s) no acervo",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = AccentMagenta,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Procurar com filtros",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentMagenta
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= CARD 2: FORMA DE PAGAMENTO =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Forma de Pagamento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4 Payment Options in requested order: Pix, Dinheiro, Débito, Crédito
                val paymentMethods = listOf(
                    "PIX" to "Pix",
                    "Dinheiro" to "Dinheiro",
                    "Cartão de Débito" to "Débito",
                    "Cartão de Crédito" to "Crédito"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentMethods.forEach { (fullMethod, displayMethod) ->
                        val isMethodSelected = selectedPaymentMethod == fullMethod
                        Surface(
                            color = if (isMethodSelected) Color(0xFFE4E4E7) else Color(0xFF262628),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clickable {
                                    selectedPaymentMethod = fullMethod
                                    if (fullMethod != "Cartão de Crédito") {
                                        selectedInstallments = "1x"
                                    }
                                    if (fullMethod != "PIX" && fullMethod != "Dinheiro") {
                                        discountPercent = 0
                                    }
                                }
                                .testTag("payment_chip_$displayMethod")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayMethod,
                                    fontSize = 12.sp,
                                    fontWeight = if (isMethodSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isMethodSelected) Color.Black else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Sub-section: "Parcelas" (SOMENTE Cartão de Crédito)
                if (isCreditCard) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val numInstallments = selectedInstallments.replace("x", "").toIntOrNull() ?: 1
                    val installmentValue = if (numInstallments > 0) totalToPay / numInstallments else totalToPay

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Parcelas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Text(
                            text = "$selectedInstallments de ${formatCurrency(installmentValue)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentMagenta
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("1x", "2x", "3x").forEach { installment ->
                            val isInstSelected = selectedInstallments == installment
                            Surface(
                                color = if (isInstSelected) AccentMagenta else Color(0xFF262628),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clickable { selectedInstallments = installment }
                                    .testTag("installment_$installment")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = installment,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isInstSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub-section: "Desconto (0 até 10%)" (SOMENTE PIX e Dinheiro)
                if (isPixOrCash) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Percent,
                                contentDescription = null,
                                tint = if (discountPercent > 0) Color(0xFFFB923C) else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Desconto (0 até 10%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }

                        if (discountPercent > 0) {
                            Surface(
                                color = Color(0xFF3B1A08),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFF97316).copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${discountPercent}% OFF (-${formatCurrency(discountAmount)})",
                                    color = Color(0xFFFB923C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Sem desconto (0%)",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick percentage chips: 0%, 3%, 5%, 7%, 10%
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 3, 5, 7, 10).forEach { pct ->
                            val isSelected = discountPercent == pct
                            Surface(
                                color = if (isSelected) Color(0xFFFB923C) else Color(0xFF262628),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable { discountPercent = pct }
                                    .testTag("discount_chip_$pct")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$pct%",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = discountPercent.toFloat(),
                        onValueChange = { discountPercent = it.roundToInt() },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = if (discountPercent > 0) Color(0xFFFB923C) else Color(0xFFA1A1AA),
                            activeTrackColor = Color(0xFFFB923C),
                            inactiveTrackColor = Color(0xFF27272A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discount_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0% (mín)", fontSize = 11.sp, color = TextMuted)
                        Text(text = "10% (máx)", fontSize = 11.sp, color = TextMuted)
                    }
                }

                // Sub-section: "Cartão de Débito" (À vista)
                if (selectedPaymentMethod == "Cartão de Débito") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF18181B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Pagamento à vista no débito (sem parcelas e sem desconto).",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= CARD 3: RESUMO DA VENDA =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Resumo da Venda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Produto", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = if (selectedProducts.isNotEmpty()) selectedProducts.joinToString { it.name } else "Nenhum selecionado",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preço Original
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Preço Original", color = TextSecondary, fontSize = 14.sp)
                    Text(
                        text = formatCurrency(originalPrice),
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                // Desconto (if applicable)
                if (discountAmount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Desconto (${discountPercent}% off)", color = Color(0xFFFB923C), fontSize = 14.sp)
                        Text(
                            text = "- ${formatCurrency(discountAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB923C),
                            fontSize = 14.sp
                        )
                    }
                }

                // Parcelamento (if applicable)
                if (isCreditCard && selectedInstallments != "1x") {
                    val numInstallments = selectedInstallments.replace("x", "").toIntOrNull() ?: 1
                    val instVal = if (numInstallments > 0) totalToPay / numInstallments else totalToPay
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Parcelamento", color = Color(0xFFC084FC), fontSize = 14.sp)
                        Text(
                            text = "$selectedInstallments de ${formatCurrency(instVal)}",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC084FC),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFF27272A))
                Spacer(modifier = Modifier.height(10.dp))

                // Total a Pagar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total a Pagar",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = formatCurrency(totalToPay),
                        fontWeight = FontWeight.Bold,
                        color = AccentMagenta,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= FINALIZAR VENDA BUTTON =================
            Button(
                onClick = {
                    if (selectedProducts.isNotEmpty()) {
                        val product = selectedProducts.first()
                        val notes = when {
                            isCreditCard && selectedInstallments != "1x" -> "Parcelado em $selectedInstallments"
                            isPixOrCash && discountPercent > 0 -> "$discountPercent% off"
                            else -> ""
                        }
                        viewModel.registerDirectSale(
                            product = product,
                            paymentMethod = selectedPaymentMethod,
                            discount = discountAmount,
                            notes = notes
                        ) { saleId ->
                            val extraDetail = when {
                                isCreditCard && selectedInstallments != "1x" -> " em $selectedInstallments"
                                isPixOrCash && discountPercent > 0 -> " com $discountPercent% de desconto"
                                else -> ""
                            }
                            lastSaleSummary = "Venda #${saleId} de '${product.name}' no valor de ${formatCurrency(totalToPay)}${extraDetail} registrada com sucesso!"
                            showSuccessDialog = true
                        }
                    }
                },
                enabled = selectedProducts.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("finalize_sale_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentMagenta,
                    disabledContainerColor = Color(0xFF27272A)
                )
            ) {
                Text(
                    text = "Finalizar Venda",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Quick Add Product Dialog
    if (showQuickAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newPrice by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Geral") }

        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            containerColor = IosSurfaceElevated,
            title = {
                Text(
                    text = "Adicionar Produto",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome do Produto") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPrice,
                        onValueChange = { newPrice = it },
                        label = { Text("Preço (R$)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Categoria") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceNum = newPrice.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (newName.isNotBlank() && priceNum > 0) {
                            viewModel.saveProduct(
                                Product(
                                    id = 0,
                                    name = newName.trim(),
                                    category = newCategory.trim(),
                                    price = priceNum
                                )
                            )
                            showQuickAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta)
                ) {
                    Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = IosSurfaceElevated,
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Venda Concluída!",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = lastSaleSummary,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta)
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ================= ÁREA DE BUSCA E SELEÇÃO DE PRODUTOS COM FILTROS =================
    if (showProductPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        
        ModalBottomSheet(
            onDismissRequest = { showProductPicker = false },
            sheetState = sheetState,
            containerColor = Color(0xFF141416),
            contentColor = TextPrimary,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFF3F3F46)
                ) {}
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header of Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Selecionar Produto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${pickerFilteredProducts.size} de ${products.size} produto(s)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(
                        onClick = { showProductPicker = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Box with clear button
                OutlinedTextField(
                    value = pickerSearchQuery,
                    onValueChange = { pickerSearchQuery = it },
                    placeholder = { Text("Buscar por nome, número, medidas...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = AccentMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (pickerSearchQuery.isNotBlank()) {
                            IconButton(onClick = { pickerSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Limpar busca",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E1E22),
                        unfocusedContainerColor = Color(0xFF1E1E22),
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = Color(0xFF2E2E33),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_picker_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips (Horizontal Scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { categoryName ->
                        val isCatSelected = pickerSelectedCategory == categoryName
                        FilterChip(
                            selected = isCatSelected,
                            onClick = { pickerSelectedCategory = categoryName },
                            label = {
                                Text(
                                    text = categoryName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E1E22),
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentMagenta,
                                selectedLabelColor = Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCatSelected) AccentMagenta else Color(0xFF2E2E33)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("product_picker_category_$categoryName")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Products List or Empty state
                if (pickerFilteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Nenhum produto encontrado",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            if (pickerSearchQuery.isNotBlank() || pickerSelectedCategory != "Todas") {
                                TextButton(
                                    onClick = {
                                        pickerSearchQuery = ""
                                        pickerSelectedCategory = "Todas"
                                    }
                                ) {
                                    Text("Limpar filtros", color = AccentMagenta, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pickerFilteredProducts, key = { it.id }) { product ->
                            val isSelected = product.id in selectedProductIds
                            Surface(
                                color = if (isSelected) Color(0xFF2A1C35) else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) AccentMagenta else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProductIds = setOf(product.id)
                                        showProductPicker = false
                                    }
                                    .testTag("picker_item_${product.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Circular selection badge
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) AccentMagenta else Color.Transparent)
                                                .border(
                                                    1.5.dp,
                                                    if (isSelected) AccentMagenta else Color(0xFF52525B),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }

                                        // Product Details
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (product.code.isNotBlank()) {
                                                    Surface(
                                                        color = Color(0xFF27272A),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (product.code.startsWith("Nº") || product.code == "S/N" || product.code == "Destaque") product.code else "Nº ${product.code}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = product.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            val detailString = listOf(product.dimensions, product.category)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • ")

                                            if (detailString.isNotBlank()) {
                                                Text(
                                                    text = detailString,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Text(
                                                text = formatCurrency(product.price),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentMagenta
                                            )
                                        }
                                    }

                                    // Right Selection Status or Action Button
                                    if (isSelected) {
                                        Surface(
                                            color = MagentaContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = "Selecionado",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentMagenta,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFF262628),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = "Selecionar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
}
