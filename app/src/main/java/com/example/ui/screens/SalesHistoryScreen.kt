package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SaleWithItems
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SalesHistoryScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val sales by viewModel.sales.collectAsState()

    // Current displayed Month & Year
    var currentMonthIndex by remember { mutableIntStateOf(8) } // 8 = September (0-indexed)
    var currentYear by remember { mutableIntStateOf(2026) }

    val monthNames = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    var selectedSaleForReceipt by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToEdit by remember { mutableStateOf<SaleWithItems?>(null) }

    // Filter sales for the selected month and year
    val monthSales = remember(sales, currentMonthIndex, currentYear) {
        sales.filter { saleWithItems ->
            val cal = Calendar.getInstance().apply { timeInMillis = saleWithItems.sale.timestamp }
            cal.get(Calendar.MONTH) == currentMonthIndex && cal.get(Calendar.YEAR) == currentYear
        }
    }

    val totalMonthSalesCount = monthSales.size
    val totalMonthRevenue = remember(monthSales) {
        monthSales.filterNot { it.sale.isCancelled }.sumOf { it.sale.totalAmount }
    }

    // Group sales by day
    val groupedSalesByDay = remember(monthSales) {
        val dateFormat = SimpleDateFormat("dd 'De' MMMM", Locale("pt", "BR"))
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("pt", "BR"))

        monthSales.groupBy { saleWithItems ->
            val date = Date(saleWithItems.sale.timestamp)
            val dayString = dateFormat.format(date).replaceFirstChar { it.uppercase() }
            val weekDay = dayOfWeekFormat.format(date).replaceFirstChar { it.uppercase() }
            dayString to weekDay
        }
    }

    Scaffold(
        containerColor = IosBackground,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // iOS Large Title
            item {
                Text(
                    text = "Histórico de Vendas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 28.sp
                )
            }

            // Month Selector Card
            item {
                IosCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Month Chevron
                        IconButton(
                            onClick = {
                                if (currentMonthIndex == 0) {
                                    currentMonthIndex = 11
                                    currentYear -= 1
                                } else {
                                    currentMonthIndex -= 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mês Anterior",
                                tint = AccentMagenta
                            )
                        }

                        // Month & Year display
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthNames[currentMonthIndex],
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 18.sp
                            )
                            Text(
                                text = currentYear.toString(),
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }

                        // Next Month Chevron
                        IconButton(
                            onClick = {
                                if (currentMonthIndex == 11) {
                                    currentMonthIndex = 0
                                    currentYear += 1
                                } else {
                                    currentMonthIndex += 1
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Próximo Mês",
                                tint = AccentMagenta
                            )
                        }
                    }
                }
            }

            // Resumo do Mês Card (with 2 columns: Vendas & Faturamento)
            item {
                IosCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = AccentMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Resumo do Mês",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Vendas
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MagentaContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = AccentMagenta,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(text = "Vendas", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = totalMonthSalesCount.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = TextPrimary
                                )
                                Text(text = "este mês", color = TextMuted, fontSize = 10.sp)
                            }
                        }

                        // Right Column: Faturamento
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = NeonGreenContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = NeonGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(text = "Faturamento", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(totalMonthRevenue),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = TextPrimary
                                )
                                Text(text = "total", color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Sales list grouped by day
            if (monthSales.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Schedule,
                        title = "Nenhuma venda neste mês",
                        message = "Altere o mês acima ou realize vendas na aba 'Vendas'."
                    )
                }
            } else {
                groupedSalesByDay.forEach { (dateInfo, salesOnDay) ->
                    val (dayTitle, daySubtitle) = dateInfo
                    val dayTotal = salesOnDay.filterNot { it.sale.isCancelled }.sumOf { it.sale.totalAmount }

                    // Date Header
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = dayTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = daySubtitle,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${salesOnDay.size} vendas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentMagenta
                                )
                                Text(
                                    text = formatCurrency(dayTotal),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    // Sales cards on this day
                    items(salesOnDay, key = { it.sale.id }) { saleWithItems ->
                        val sale = saleWithItems.sale
                        val mainProductName = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${sale.id}"
                        val timeStr = formatTimeOnly(sale.timestamp)

                        Surface(
                            color = IosSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, IosCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSaleForReceipt = saleWithItems }
                                .testTag("history_sale_card_${sale.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mainProductName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = timeStr,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Badges row: Payment method & discount
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PaymentMethodBadge(method = sale.paymentMethod)
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = formatCurrency(sale.totalAmount),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )

                                    // Edit / Details Pencil Button
                                    Surface(
                                        color = MagentaContainer,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable { saleToEdit = saleWithItems }
                                            .testTag("history_edit_sale_${sale.id}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar Venda",
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

    // Receipt details dialog
    if (selectedSaleForReceipt != null) {
        ReceiptDetailsDialog(
            saleWithItems = selectedSaleForReceipt!!,
            onDismiss = { selectedSaleForReceipt = null },
            onEditSale = { saleToEdit = selectedSaleForReceipt }
        )
    }

    // Edit Sale Dialog
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
