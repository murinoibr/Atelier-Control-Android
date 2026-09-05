package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel
import com.example.util.PdfReportGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class ReportType(
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val iconColor: Color,
    val containerColor: Color,
    val description: String
) {
    DAY(
        title = "Vendas do Dia",
        shortLabel = "Do Dia",
        icon = Icons.Default.CalendarToday,
        iconColor = Color(0xFF38BDF8),
        containerColor = Color(0xFF0C243C),
        description = "Resumo de todas as vendas do dia selecionado"
    ),
    WEEK(
        title = "Vendas da Semana",
        shortLabel = "Da Semana",
        icon = Icons.Default.DateRange,
        iconColor = Color(0xFFD946EF),
        containerColor = Color(0xFF3B0764),
        description = "Resumo de todas as vendas dos últimos 7 dias"
    ),
    MONTH(
        title = "Vendas do Mês",
        shortLabel = "Do Mês",
        icon = Icons.Default.CalendarMonth,
        iconColor = Color(0xFF22C55E),
        containerColor = Color(0xFF052E16),
        description = "Resumo de todas as vendas do mês selecionado"
    ),
    PAYMENT_METHOD(
        title = "Por Forma de Pagamento",
        shortLabel = "Pagamento",
        icon = Icons.Default.CreditCard,
        iconColor = Color(0xFFF97316),
        containerColor = Color(0xFF451A03),
        description = "Vendas agrupadas ou filtradas por forma de pagamento"
    ),
    CATEGORY(
        title = "Por Categoria",
        shortLabel = "Categoria",
        icon = Icons.Default.LocalOffer,
        iconColor = Color(0xFF06B6D4),
        containerColor = Color(0xFF083344),
        description = "Vendas agrupadas ou filtradas por categoria de produto"
    ),
    FULL(
        title = "Relatório Completo",
        shortLabel = "Completo",
        icon = Icons.Default.Description,
        iconColor = Color(0xFF8B5CF6),
        containerColor = Color(0xFF2E1065),
        description = "Visão geral e detalhada de todo o histórico de vendas"
    )
}

fun getPeriodLabel(type: ReportType): String {
    val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    val sdfMonth = SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
    val now = Date()
    return when (type) {
        ReportType.DAY -> "Hoje (${sdfDate.format(now)})"
        ReportType.WEEK -> "Últimos 7 dias"
        ReportType.MONTH -> sdfMonth.format(now).replaceFirstChar { it.uppercase() }
        ReportType.PAYMENT_METHOD -> "Todas as Formas"
        ReportType.CATEGORY -> "Todas as Categorias"
        ReportType.FULL -> "Todo o Histórico"
    }
}

data class PaymentMethodBreakdown(
    val method: String,
    val count: Int,
    val total: Double,
    val percentage: Double,
    val icon: ImageVector,
    val color: Color
)

data class CategoryBreakdown(
    val category: String,
    val itemsCount: Int,
    val total: Double,
    val percentage: Double
)

data class WeekDayBreakdown(
    val dayName: String,
    val dateStr: String,
    val count: Int,
    val total: Double,
    val isBestDay: Boolean = false
)

data class MonthWeekBreakdown(
    val weekLabel: String,
    val dateRange: String,
    val count: Int,
    val total: Double,
    val percentage: Double
)

data class DayCashSummary(
    val pix: Double,
    val dinheiro: Double,
    val debito: Double,
    val credito: Double
)

@Composable
fun ReportsScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val scrollState = rememberScrollState()

    val products by viewModel.products.collectAsState()
    val productCategoryMap = remember(products) { products.associate { it.id to it.category } }
    val productNameCategoryMap = remember(products) { products.associate { it.name.trim().lowercase() to it.category } }
    val uniqueCategories = remember(products) {
        val list = products.map { it.category.trim().ifBlank { "Sem Categoria" } }.distinct().sorted()
        listOf("Todas") + list
    }

    var selectedReportType by remember { mutableStateOf(ReportType.DAY) }
    var selectedQuickFilter by remember { mutableStateOf<String?>("Do Dia") }
    var selectedDateText by remember { mutableStateOf(getPeriodLabel(ReportType.DAY)) }
    var selectedPaymentFilter by remember { mutableStateOf("Todas") }
    var selectedCategoryFilter by remember { mutableStateOf("Todas") }

    var calendarDisplayMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedCalendarDay by remember { mutableStateOf(Calendar.getInstance()) }

    var showReportViewerDialog by remember { mutableStateOf(false) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var lastGeneratedPdfFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Filtered sales according to the selected report type and sub-filters
    val filteredSales = remember(
        sales,
        selectedReportType,
        selectedQuickFilter,
        selectedPaymentFilter,
        selectedCategoryFilter,
        selectedCalendarDay,
        calendarDisplayMonth,
        productCategoryMap,
        productNameCategoryMap
    ) {
        val nonCancelled = sales.filterNot { it.sale.isCancelled }
        when (selectedReportType) {
            ReportType.DAY -> {
                nonCancelled.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.sale.timestamp }
                    sCal.get(Calendar.DAY_OF_YEAR) == selectedCalendarDay.get(Calendar.DAY_OF_YEAR) &&
                            sCal.get(Calendar.YEAR) == selectedCalendarDay.get(Calendar.YEAR)
                }
            }
            ReportType.WEEK -> {
                val now = System.currentTimeMillis()
                val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                nonCancelled.filter { it.sale.timestamp in weekAgo..now }
            }
            ReportType.MONTH -> {
                nonCancelled.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.sale.timestamp }
                    sCal.get(Calendar.MONTH) == calendarDisplayMonth.get(Calendar.MONTH) &&
                            sCal.get(Calendar.YEAR) == calendarDisplayMonth.get(Calendar.YEAR)
                }
            }
            ReportType.PAYMENT_METHOD -> {
                if (selectedPaymentFilter == "Todas") {
                    nonCancelled
                } else {
                    nonCancelled.filter {
                        it.sale.paymentMethod.contains(selectedPaymentFilter, ignoreCase = true) ||
                                (selectedPaymentFilter == "Débito" && it.sale.paymentMethod.contains("Debito", ignoreCase = true)) ||
                                (selectedPaymentFilter == "Crédito" && it.sale.paymentMethod.contains("Credito", ignoreCase = true))
                    }
                }
            }
            ReportType.CATEGORY -> {
                if (selectedCategoryFilter == "Todas") {
                    nonCancelled
                } else {
                    nonCancelled.filter { saleWithItems ->
                        saleWithItems.items.any { item ->
                            val cat = productCategoryMap[item.productId]?.trim()?.ifBlank { null }
                                ?: productNameCategoryMap[item.productName.trim().lowercase()]?.trim()?.ifBlank { null }
                            cat?.equals(selectedCategoryFilter, ignoreCase = true) == true ||
                                    (selectedCategoryFilter == "Sem Categoria" && cat.isNullOrBlank())
                        }
                    }
                }
            }
            ReportType.FULL -> nonCancelled
        }
    }

    val totalSalesCount = filteredSales.size
    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.sale.totalAmount } }
    val totalProfit = remember(filteredSales) { filteredSales.sumOf { it.sale.totalProfit } }
    val totalDiscount = remember(filteredSales) { filteredSales.sumOf { it.sale.discount } }

    // Tailored breakdowns computed for each report type
    val paymentBreakdown = remember(filteredSales) {
        val totalRev = filteredSales.sumOf { it.sale.totalAmount }
        val normMap = mapOf(
            "PIX" to Triple(Icons.Default.AttachMoney, Color(0xFF22C55E), "PIX"),
            "Cartão de Crédito" to Triple(Icons.Default.CreditCard, Color(0xFFD946EF), "Cartão de Crédito"),
            "Cartão de Débito" to Triple(Icons.Default.CreditCard, Color(0xFF38BDF8), "Cartão de Débito"),
            "Dinheiro" to Triple(Icons.Default.AttachMoney, Color(0xFFF59E0B), "Dinheiro")
        )
        val grouped = filteredSales.groupBy { saleWithItems ->
            val raw = saleWithItems.sale.paymentMethod
            when {
                raw.contains("Crédito", ignoreCase = true) || raw.contains("Credito", ignoreCase = true) -> "Cartão de Crédito"
                raw.contains("Débito", ignoreCase = true) || raw.contains("Debito", ignoreCase = true) -> "Cartão de Débito"
                raw.contains("PIX", ignoreCase = true) -> "PIX"
                raw.contains("Dinheiro", ignoreCase = true) -> "Dinheiro"
                raw.isNotBlank() -> raw
                else -> "Outro"
            }
        }
        val priority = mapOf("PIX" to 1, "Cartão de Débito" to 2, "Cartão de Crédito" to 3, "Dinheiro" to 4)
        val allKeys = (listOf("PIX", "Cartão de Débito", "Cartão de Crédito", "Dinheiro") + grouped.keys).distinct()
        allKeys.map { key ->
            val list = grouped[key] ?: emptyList()
            val count = list.size
            val sum = list.sumOf { it.sale.totalAmount }
            val pct = if (totalRev > 0) (sum / totalRev) * 100.0 else 0.0
            val meta = normMap[key] ?: Triple(Icons.Default.AccountBalanceWallet, Color(0xFF94A3B8), key)
            PaymentMethodBreakdown(key, count, sum, pct, meta.first, meta.second)
        }.sortedWith(compareBy({ priority[it.method] ?: 99 }, { -it.total }))
    }

    val categoryBreakdown = remember(filteredSales, productCategoryMap, productNameCategoryMap) {
        val totalRev = filteredSales.sumOf { it.sale.totalAmount }
        val catBuckets = mutableMapOf<String, Pair<Int, Double>>()
        filteredSales.forEach { saleWithItems ->
            if (saleWithItems.items.isEmpty()) {
                val curr = catBuckets.getOrDefault("Sem Categoria", Pair(0, 0.0))
                catBuckets["Sem Categoria"] = Pair(curr.first + 1, curr.second + saleWithItems.sale.totalAmount)
            } else {
                saleWithItems.items.forEach { item ->
                    val cat = productCategoryMap[item.productId]?.trim()?.ifBlank { null }
                        ?: productNameCategoryMap[item.productName.trim().lowercase()]?.trim()?.ifBlank { null }
                        ?: "Sem Categoria"
                    val qty = item.quantity.coerceAtLeast(1)
                    val subtotal = if (item.subtotal > 0.0) item.subtotal else (item.unitPrice * qty)
                    val curr = catBuckets.getOrDefault(cat, Pair(0, 0.0))
                    catBuckets[cat] = Pair(curr.first + qty, curr.second + subtotal)
                }
            }
        }
        catBuckets.map { (cat, pair) ->
            val pct = if (totalRev > 0) (pair.second / totalRev) * 100.0 else 0.0
            CategoryBreakdown(cat, pair.first, pair.second, pct)
        }.sortedByDescending { it.total }
    }

    val weekDaysBreakdown = remember(filteredSales) {
        val sdfDayName = SimpleDateFormat("EEEE", Locale("pt", "BR"))
        val sdfShortDate = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
        val list = mutableListOf<WeekDayBreakdown>()
        val daySums = mutableListOf<Double>()
        for (offset in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val dYear = cal.get(Calendar.YEAR)
            val dOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val salesOnDay = filteredSales.filter {
                val sCal = Calendar.getInstance().apply { timeInMillis = it.sale.timestamp }
                sCal.get(Calendar.YEAR) == dYear && sCal.get(Calendar.DAY_OF_YEAR) == dOfYear
            }
            val dayTotal = salesOnDay.sumOf { it.sale.totalAmount }
            daySums.add(dayTotal)
            val rawDay = sdfDayName.format(cal.time)
            val dayName = rawDay.substringBefore("-").replaceFirstChar { it.uppercase() }
            val dateStr = sdfShortDate.format(cal.time)
            list.add(WeekDayBreakdown(dayName = dayName, dateStr = dateStr, count = salesOnDay.size, total = dayTotal))
        }
        val maxDayTotal = daySums.maxOrNull() ?: 0.0
        list.map { it.copy(isBestDay = it.total > 0 && it.total == maxDayTotal) }
    }
    val bestWeekDay = remember(weekDaysBreakdown) {
        weekDaysBreakdown.filter { it.total > 0 }.maxByOrNull { it.total }
    }
    val weekDailyAvg = remember(filteredSales) {
        filteredSales.sumOf { it.sale.totalAmount } / 7.0
    }

    val monthWeeksBreakdown = remember(filteredSales, calendarDisplayMonth) {
        val cal = calendarDisplayMonth.clone() as Calendar
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val ranges = listOf(
            Triple("Semana 1", "01 a 07", 1..7),
            Triple("Semana 2", "08 a 14", 8..14),
            Triple("Semana 3", "15 a 21", 15..21),
            Triple("Semana 4+", "22 a $maxDays", 22..maxDays)
        )
        val totalRev = filteredSales.sumOf { it.sale.totalAmount }
        ranges.map { (label, dateRange, range) ->
            val salesInWeek = filteredSales.filter {
                val sCal = Calendar.getInstance().apply { timeInMillis = it.sale.timestamp }
                sCal.get(Calendar.DAY_OF_MONTH) in range
            }
            val count = salesInWeek.size
            val total = salesInWeek.sumOf { it.sale.totalAmount }
            val pct = if (totalRev > 0) (total / totalRev) * 100.0 else 0.0
            MonthWeekBreakdown(label, dateRange, count, total, pct)
        }
    }
    val totalMonthItemsCount = remember(filteredSales) {
        filteredSales.sumOf { it.items.sumOf { item -> item.quantity }.coerceAtLeast(1) }
    }
    val dayTicketMedio = remember(filteredSales) {
        val rev = filteredSales.sumOf { it.sale.totalAmount }
        if (filteredSales.isNotEmpty()) rev / filteredSales.size else 0.0
    }
    val dayCashSummary = remember(filteredSales) {
        val pix = filteredSales.filter { it.sale.paymentMethod.contains("PIX", ignoreCase = true) }.sumOf { it.sale.totalAmount }
        val dinheiro = filteredSales.filter { it.sale.paymentMethod.contains("Dinheiro", ignoreCase = true) }.sumOf { it.sale.totalAmount }
        val debito = filteredSales.filter { it.sale.paymentMethod.contains("Débito", ignoreCase = true) || it.sale.paymentMethod.contains("Debito", ignoreCase = true) }.sumOf { it.sale.totalAmount }
        val credito = filteredSales.filter { it.sale.paymentMethod.contains("Crédito", ignoreCase = true) || it.sale.paymentMethod.contains("Credito", ignoreCase = true) }.sumOf { it.sale.totalAmount }
        DayCashSummary(pix = pix, dinheiro = dinheiro, debito = debito, credito = credito)
    }

    fun generatePdfAndShowDialog() {
        if (isGeneratingPdf) return
        isGeneratingPdf = true
        val title = when (selectedReportType) {
            ReportType.PAYMENT_METHOD -> if (selectedPaymentFilter != "Todas") "Por Forma de Pagamento ($selectedPaymentFilter)" else "Por Forma de Pagamento"
            ReportType.CATEGORY -> if (selectedCategoryFilter != "Todas") "Por Categoria ($selectedCategoryFilter)" else "Por Categoria"
            else -> selectedReportType.title
        }
        val file = PdfReportGenerator.generateSalesReportPdf(
            context = context,
            reportTitle = title,
            periodText = selectedDateText,
            sales = filteredSales,
            totalRevenue = totalRevenue,
            totalProfit = totalProfit,
            totalDiscount = totalDiscount,
            productCategoryMap = productCategoryMap,
            productNameCategoryMap = productNameCategoryMap,
            reportType = selectedReportType.name
        )
        isGeneratingPdf = false
        if (file != null) {
            lastGeneratedPdfFile = file
            showExportSuccessDialog = true
        } else {
            Toast.makeText(context, "Erro ao gerar arquivo PDF.", Toast.LENGTH_SHORT).show()
        }
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
            // iOS Large Title
            Text(
                text = "Relatórios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ================= CARD 1: TIPO DE RELATÓRIO =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tipo de Relatório",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2 columns x 3 rows grid
                val reportTypes = ReportType.values()
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val type = reportTypes[index]
                            val isSelected = selectedReportType == type

                            Surface(
                                color = if (isSelected) Color(0xFF007AFF) else Color(0xFF18181B),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF007AFF) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(104.dp)
                                    .clickable {
                                        selectedReportType = type
                                        selectedQuickFilter = type.shortLabel
                                        selectedDateText = getPeriodLabel(type)
                                    }
                                    .testTag("report_type_${type.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Top Icon Badge
                                    Surface(
                                        color = if (isSelected) Color.White.copy(alpha = 0.22f) else type.containerColor,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = type.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else type.iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = type.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                    if (row < 2) Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Information Helper Banner matching screenshot
                Surface(
                    color = Color(0xFF141416),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selectedReportType.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= CARD 2: PERÍODO =================
            IosCard {
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = Color(0xFFD946EF),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Período",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 17.sp
                        )
                    }

                    // Date Pill
                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable {
                                selectedDateText = getPeriodLabel(selectedReportType)
                            }
                            .testTag("report_date_picker_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = selectedDateText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Month Navigation Row matching screenshot (e.g., "September 2026 >")
                val monthNameYear = remember(calendarDisplayMonth) {
                    val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
                    sdfMonthYear.format(calendarDisplayMonth.time).replaceFirstChar { it.uppercase() }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = monthNameYear,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = Color(0xFF27272A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    val newMonth = calendarDisplayMonth.clone() as Calendar
                                    newMonth.add(Calendar.MONTH, -1)
                                    calendarDisplayMonth = newMonth
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Mês Anterior",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF27272A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    val newMonth = calendarDisplayMonth.clone() as Calendar
                                    newMonth.add(Calendar.MONTH, 1)
                                    calendarDisplayMonth = newMonth
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Próximo Mês",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Days of the week header row
                val weekdayLabels = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF71717A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Days of current month grid
                val firstDayOfWeek = remember(calendarDisplayMonth) {
                    val c = calendarDisplayMonth.clone() as Calendar
                    c.set(Calendar.DAY_OF_MONTH, 1)
                    c.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon...
                }
                val maxDaysInMonth = remember(calendarDisplayMonth) {
                    calendarDisplayMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                }

                val totalCells = (firstDayOfWeek - 1) + maxDaysInMonth
                val totalRows = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (weekRow in 0 until totalRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dayCol in 0 until 7) {
                                val cellIndex = weekRow * 7 + dayCol
                                val dayNum = cellIndex - (firstDayOfWeek - 1) + 1

                                if (dayNum in 1..maxDaysInMonth) {
                                    val isSelectedDay = selectedCalendarDay.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                            selectedCalendarDay.get(Calendar.MONTH) == calendarDisplayMonth.get(Calendar.MONTH) &&
                                            selectedCalendarDay.get(Calendar.YEAR) == calendarDisplayMonth.get(Calendar.YEAR)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            color = if (isSelectedDay) Color(0xFFD946EF) else Color.Transparent,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clickable {
                                                    val newDayCal = calendarDisplayMonth.clone() as Calendar
                                                    newDayCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                                    selectedCalendarDay = newDayCal
                                                    selectedReportType = ReportType.DAY
                                                    val sdfD = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                                                    selectedDateText = "Dia ${sdfD.format(newDayCal.time)}"
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 12.5.sp,
                                                    fontWeight = if (isSelectedDay) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelectedDay) Color.White else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Sub-filter row when "Por Forma de Pagamento" is active
                if (selectedReportType == ReportType.PAYMENT_METHOD) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Forma de Pagamento:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val methods = listOf("Todas", "Pix", "Dinheiro", "Débito", "Crédito")
                        methods.forEach { method ->
                            val isFilterSel = when (method) {
                                "Todas" -> selectedPaymentFilter == "Todas"
                                "Pix" -> selectedPaymentFilter == "Pix"
                                "Dinheiro" -> selectedPaymentFilter == "Dinheiro"
                                "Débito" -> selectedPaymentFilter == "Cartão de Débito" || selectedPaymentFilter == "Débito"
                                "Crédito" -> selectedPaymentFilter == "Cartão de Crédito" || selectedPaymentFilter == "Crédito"
                                else -> selectedPaymentFilter == method
                            }
                            Surface(
                                color = if (isFilterSel) Color(0xFF007AFF) else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFilterSel) Color(0xFF007AFF) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clickable {
                                        selectedPaymentFilter = when (method) {
                                            "Débito" -> "Cartão de Débito"
                                            "Crédito" -> "Cartão de Crédito"
                                            else -> method
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = method,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isFilterSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFilterSel) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Sub-filter row when "Por Categoria" is active
                if (selectedReportType == ReportType.CATEGORY) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFF27272A), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Filtrar Categoria de Produto:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uniqueCategories.forEach { category ->
                            val isCatSel = selectedCategoryFilter == category
                            Surface(
                                color = if (isCatSel) Color(0xFF007AFF) else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCatSel) Color(0xFF007AFF) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .clickable {
                                        selectedCategoryFilter = category
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = category,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCatSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCatSel) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Period Filter Buttons: [Do Dia] [Da Semana] [Do Mês] [Completo]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTypes = listOf(ReportType.DAY, ReportType.WEEK, ReportType.MONTH, ReportType.FULL)
                    quickTypes.forEach { type ->
                        val isFilterSelected = selectedReportType == type
                        Surface(
                            color = if (isFilterSelected) Color(0xFF27272A) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isFilterSelected) NeonGreen else Color(0xFF27272A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clickable {
                                    selectedReportType = type
                                    selectedQuickFilter = type.shortLabel
                                    selectedDateText = getPeriodLabel(type)
                                }
                                .testTag("quick_filter_${type.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = type.shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isFilterSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFilterSelected) NeonGreen else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= CARD 3: RESUMO DO PERÍODO =================
            IosCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Resumo do Período",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left: Vendas
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = NeonGreenContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(text = "Vendas", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = totalSalesCount.toString(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = TextPrimary
                            )
                            Text(text = "encontradas", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    // Right: Total Faturamento
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = NeonGreenContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(46.dp)
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
                            Text(text = "Total", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = formatCurrency(totalRevenue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(text = "faturamento", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ================= CARD 4: DEMONSTRATIVO ESPECÍFICO DO TIPO =================
            ReportSpecificDemonstrativeCard(
                reportType = selectedReportType,
                selectedDateText = selectedDateText,
                filteredSales = filteredSales,
                totalRevenue = totalRevenue,
                totalProfit = totalProfit,
                totalDiscount = totalDiscount,
                totalSalesCount = totalSalesCount,
                paymentBreakdown = paymentBreakdown,
                categoryBreakdown = categoryBreakdown,
                weekDaysBreakdown = weekDaysBreakdown,
                bestWeekDay = bestWeekDay,
                weekDailyAvg = weekDailyAvg,
                monthWeeksBreakdown = monthWeeksBreakdown,
                totalMonthItemsCount = totalMonthItemsCount,
                dayTicketMedio = dayTicketMedio,
                dayCashSummary = dayCashSummary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ================= ACTION BUTTONS =================
            // Primary Button: "Visualizar Relatório" (Neon Green, Eye icon)
            Button(
                onClick = { showReportViewerDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("view_report_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Visualizar Relatório",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Button: "Exportar PDF" (Dark with Green border & text)
            Surface(
                color = NeonGreenContainer,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { generatePdfAndShowDialog() }
                    .testTag("export_pdf_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gerando PDF...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exportar PDF",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ================= SECTION: RELATÓRIOS RÁPIDOS =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Relatórios Rápidos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 17.sp
                )

                Surface(
                    color = NeonGreenContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "6 opções",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Vendas do Dia
                QuickReportItemCard(
                    title = "Vendas do Dia",
                    subtitle = "Vendas e fechamento de hoje",
                    badge = "Hoje",
                    icon = Icons.Default.CalendarToday,
                    iconTint = Color(0xFF38BDF8),
                    containerColor = Color(0xFF0C243C),
                    testTag = "quick_report_day_card",
                    onClick = {
                        selectedReportType = ReportType.DAY
                        selectedQuickFilter = "Do Dia"
                        selectedDateText = getPeriodLabel(ReportType.DAY)
                        showReportViewerDialog = true
                    }
                )

                // 2. Vendas da Semana
                QuickReportItemCard(
                    title = "Vendas da Semana",
                    subtitle = "Consolidado dos últimos 7 dias",
                    badge = "7 dias",
                    icon = Icons.Default.DateRange,
                    iconTint = Color(0xFFD946EF),
                    containerColor = Color(0xFF3B0764),
                    testTag = "quick_report_week_card",
                    onClick = {
                        selectedReportType = ReportType.WEEK
                        selectedQuickFilter = "Da Semana"
                        selectedDateText = getPeriodLabel(ReportType.WEEK)
                        showReportViewerDialog = true
                    }
                )

                // 3. Vendas do Mês
                QuickReportItemCard(
                    title = "Vendas do Mês",
                    subtitle = "Desempenho financeiro do mês atual",
                    badge = "Mês",
                    icon = Icons.Default.CalendarMonth,
                    iconTint = Color(0xFF22C55E),
                    containerColor = Color(0xFF052E16),
                    testTag = "quick_report_month_card",
                    onClick = {
                        selectedReportType = ReportType.MONTH
                        selectedQuickFilter = "Do Mês"
                        selectedDateText = getPeriodLabel(ReportType.MONTH)
                        showReportViewerDialog = true
                    }
                )

                // 4. Por Forma de Pagamento
                QuickReportItemCard(
                    title = "Por Forma de Pagamento",
                    subtitle = "Vendas agrupadas por método de pagamento",
                    badge = "Pagamento",
                    icon = Icons.Default.CreditCard,
                    iconTint = Color(0xFFF97316),
                    containerColor = Color(0xFF451A03),
                    testTag = "quick_report_payment_card",
                    onClick = {
                        selectedReportType = ReportType.PAYMENT_METHOD
                        selectedQuickFilter = "Pagamento"
                        selectedDateText = getPeriodLabel(ReportType.PAYMENT_METHOD)
                        showReportViewerDialog = true
                    }
                )

                // 5. Por Categoria
                QuickReportItemCard(
                    title = "Por Categoria",
                    subtitle = "Vendas agrupadas por categoria de produto",
                    badge = "Categoria",
                    icon = Icons.Default.LocalOffer,
                    iconTint = Color(0xFF06B6D4),
                    containerColor = Color(0xFF083344),
                    testTag = "quick_report_category_card",
                    onClick = {
                        selectedReportType = ReportType.CATEGORY
                        selectedQuickFilter = "Categoria"
                        selectedDateText = getPeriodLabel(ReportType.CATEGORY)
                        showReportViewerDialog = true
                    }
                )

                // 6. Relatório Completo
                QuickReportItemCard(
                    title = "Relatório Completo",
                    subtitle = "Histórico geral de todas as vendas cadastradas",
                    badge = "Geral",
                    icon = Icons.Default.Description,
                    iconTint = Color(0xFF8B5CF6),
                    containerColor = Color(0xFF2E1065),
                    testTag = "quick_report_full_card",
                    onClick = {
                        selectedReportType = ReportType.FULL
                        selectedQuickFilter = "Completo"
                        selectedDateText = getPeriodLabel(ReportType.FULL)
                        showReportViewerDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Full Report Viewer Dialog
    if (showReportViewerDialog) {
        AlertDialog(
            onDismissRequest = { showReportViewerDialog = false },
            containerColor = IosSurfaceElevated,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = selectedReportType.icon,
                        contentDescription = null,
                        tint = selectedReportType.iconColor
                    )
                    Text(
                        text = selectedReportType.title,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            },
            text = {
                ReportViewerDetailContent(
                    reportType = selectedReportType,
                    selectedDateText = selectedDateText,
                    filteredSales = filteredSales,
                    totalRevenue = totalRevenue,
                    totalProfit = totalProfit,
                    totalDiscount = totalDiscount,
                    totalSalesCount = totalSalesCount,
                    paymentBreakdown = paymentBreakdown,
                    categoryBreakdown = categoryBreakdown,
                    weekDaysBreakdown = weekDaysBreakdown,
                    bestWeekDay = bestWeekDay,
                    weekDailyAvg = weekDailyAvg,
                    monthWeeksBreakdown = monthWeeksBreakdown,
                    totalMonthItemsCount = totalMonthItemsCount,
                    dayTicketMedio = dayTicketMedio,
                    dayCashSummary = dayCashSummary
                )
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            showReportViewerDialog = false
                            generatePdfAndShowDialog()
                        },
                        border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { showReportViewerDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Fechar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        )
    }

    // Export PDF Feedback Dialog with Real File Actions
    if (showExportSuccessDialog && lastGeneratedPdfFile != null) {
        val file = lastGeneratedPdfFile!!
        val fileSizeKb = (file.length() / 1024).coerceAtLeast(1)

        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            containerColor = IosSurfaceElevated,
            icon = {
                Surface(
                    color = NeonGreen.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Relatório em PDF Criado!",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "O arquivo PDF foi gerado e salvo com sucesso no armazenamento local.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // File Information Box
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = file.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tamanho: $fileSizeKb KB",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${filteredSales.size} transações",
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Action Buttons: Open & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Open PDF Button
                        Button(
                            onClick = {
                                PdfReportGenerator.openPdf(context, file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Visualizar",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Share PDF Button
                        Button(
                            onClick = {
                                PdfReportGenerator.sharePdf(context, file)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            border = BorderStroke(1.dp, Color(0xFF3A3A3C)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enviar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExportSuccessDialog = false }
                ) {
                    Text("Concluir", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun QuickReportItemCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = IosSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, IosCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Surface(
                            color = iconTint.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badge,
                                color = iconTint,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StatProgressBar(
    progress: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0.0, 1.0).toFloat()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2C2C2E))
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun MiniKpiPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Surface(
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, Color(0xFF2C2C2E)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = label, fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ReportSpecificDemonstrativeCard(
    reportType: ReportType,
    selectedDateText: String,
    filteredSales: List<com.example.data.model.SaleWithItems>,
    totalRevenue: Double,
    totalProfit: Double,
    totalDiscount: Double,
    totalSalesCount: Int,
    paymentBreakdown: List<PaymentMethodBreakdown>,
    categoryBreakdown: List<CategoryBreakdown>,
    weekDaysBreakdown: List<WeekDayBreakdown>,
    bestWeekDay: WeekDayBreakdown?,
    weekDailyAvg: Double,
    monthWeeksBreakdown: List<MonthWeekBreakdown>,
    totalMonthItemsCount: Int,
    dayTicketMedio: Double,
    dayCashSummary: DayCashSummary
) {
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }
    val sdfDate = remember { SimpleDateFormat("dd/MM/yy", Locale("pt", "BR")) }

    IosCard {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = reportType.icon,
                contentDescription = null,
                tint = reportType.iconColor,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (reportType) {
                        ReportType.DAY -> "Demonstrativo do Dia"
                        ReportType.WEEK -> "Desempenho dos Últimos 7 Dias"
                        ReportType.MONTH -> "Evolução Semanal do Mês"
                        ReportType.PAYMENT_METHOD -> "Distribuição por Forma de Pagamento"
                        ReportType.CATEGORY -> "Distribuição por Categoria de Obras"
                        ReportType.FULL -> "Consolidado Geral de Vendas"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Text(
                    text = when (reportType) {
                        ReportType.DAY -> "Fechamento de caixa e timeline de hoje"
                        ReportType.WEEK -> "Evolução diária e média de faturamento"
                        ReportType.MONTH -> "Desempenho por semanas e produtos do mês"
                        ReportType.PAYMENT_METHOD -> "Receita e volume por meio de recebimento"
                        ReportType.CATEGORY -> "Peças vendidas e faturamento por categoria"
                        ReportType.FULL -> "Visão financeira e operacional integrada"
                    },
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Surface(
                color = reportType.iconColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = reportType.shortLabel,
                    color = reportType.iconColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Content strictly corresponding to the selected report type
        when (reportType) {
            ReportType.DAY -> {
                // KPIs for Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        label = "Ticket Médio do Dia",
                        value = formatCurrency(dayTicketMedio),
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKpiPill(
                        label = "Descontos no Dia",
                        value = if (totalDiscount > 0) "-${formatCurrency(totalDiscount)}" else "R$ 0,00",
                        valueColor = Color(0xFFFB923C),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Entradas no Caixa por Meio:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 4 Cash summary pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // PIX
                    Surface(
                        color = Color(0xFF14291F),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF22C55E).copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "PIX", fontSize = 10.sp, color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                            Text(text = formatCurrency(dayCashSummary.pix), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    // Dinheiro
                    Surface(
                        color = Color(0xFF2B1D0C),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Dinheiro", fontSize = 10.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                            Text(text = formatCurrency(dayCashSummary.dinheiro), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Débito
                    Surface(
                        color = Color(0xFF0F2537),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF38BDF8).copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Débito", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            Text(text = formatCurrency(dayCashSummary.debito), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                    // Crédito
                    Surface(
                        color = Color(0xFF2A1535),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFD946EF).copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Crédito", fontSize = 10.sp, color = Color(0xFFE879F9), fontWeight = FontWeight.Bold)
                            Text(text = formatCurrency(dayCashSummary.credito), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Vendas Registradas Hoje (${filteredSales.size}):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (filteredSales.isEmpty()) {
                    Text(
                        text = "Nenhuma venda registrada na data de hoje.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    filteredSales.take(5).forEach { saleWithItems ->
                        val timeStr = sdfTime.format(Date(saleWithItems.sale.timestamp))
                        val title = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${saleWithItems.sale.id}"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = Color(0xFF2C2C2E),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = timeStr,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formatCurrency(saleWithItems.sale.totalAmount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }

            ReportType.WEEK -> {
                // KPIs for Week
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        label = "Média Diária",
                        value = "${formatCurrency(weekDailyAvg)}/dia",
                        valueColor = Color(0xFFD946EF),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKpiPill(
                        label = "Melhor Dia da Semana",
                        value = bestWeekDay?.let { "${it.dayName} (${formatCurrency(it.total)})" } ?: "—",
                        valueColor = Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Desempenho Dia a Dia (Últimos 7 dias):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val maxDaySum = weekDaysBreakdown.maxOfOrNull { it.total } ?: 1.0
                weekDaysBreakdown.forEach { dayStat ->
                    val ratio = if (maxDaySum > 0) dayStat.total / maxDaySum else 0.0
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${dayStat.dayName} (${dayStat.dateStr})",
                                    fontSize = 12.sp,
                                    fontWeight = if (dayStat.isBestDay) FontWeight.Bold else FontWeight.Normal,
                                    color = if (dayStat.isBestDay) Color(0xFFFBBF24) else TextPrimary
                                )
                                if (dayStat.isBestDay && dayStat.total > 0) {
                                    Surface(
                                        color = Color(0xFFFBBF24).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "★ Melhor",
                                            fontSize = 9.sp,
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${dayStat.count} vds • ${formatCurrency(dayStat.total)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayStat.total > 0) NeonGreen else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        StatProgressBar(
                            progress = ratio,
                            color = if (dayStat.isBestDay) Color(0xFFFBBF24) else Color(0xFFD946EF)
                        )
                    }
                }
            }

            ReportType.MONTH -> {
                // KPIs for Month
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        label = "Obras Vendidas no Mês",
                        value = "$totalMonthItemsCount obras",
                        valueColor = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKpiPill(
                        label = "Média Semanal",
                        value = formatCurrency(totalRevenue / 4.0),
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Evolução por Semanas do Mês:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                monthWeeksBreakdown.forEach { weekStat ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${weekStat.weekLabel} (${weekStat.dateRange})",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${weekStat.count} vds • ${formatCurrency(weekStat.total)} (${String.format(Locale.US, "%.0f", weekStat.percentage)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (weekStat.total > 0) NeonGreen else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        StatProgressBar(
                            progress = weekStat.percentage / 100.0,
                            color = Color(0xFF22C55E)
                        )
                    }
                }

                if (categoryBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Top Categorias no Mês:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    categoryBreakdown.take(3).forEach { cat ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "• ${cat.category} (${cat.itemsCount} pcs)", fontSize = 11.sp, color = TextMuted)
                            Text(text = formatCurrency(cat.total), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }

            ReportType.PAYMENT_METHOD -> {
                // KPIs for Payment Method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val leader = paymentBreakdown.maxByOrNull { it.total }
                    MiniKpiPill(
                        label = "Forma Líder em Receita",
                        value = leader?.let { "${it.method} (${String.format(Locale.US, "%.0f", it.percentage)}%)" } ?: "—",
                        valueColor = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKpiPill(
                        label = "Total de Transações",
                        value = "$totalSalesCount vendas",
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Divisão por Meio de Pagamento:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                paymentBreakdown.forEach { stat ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = stat.icon,
                                    contentDescription = null,
                                    tint = stat.color,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stat.method,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${stat.count} vds • ${formatCurrency(stat.total)} (${String.format(Locale.US, "%.0f", stat.percentage)}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = stat.color
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        StatProgressBar(
                            progress = stat.percentage / 100.0,
                            color = stat.color
                        )
                    }
                }
            }

            ReportType.CATEGORY -> {
                // KPIs for Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val leader = categoryBreakdown.firstOrNull()
                    MiniKpiPill(
                        label = "Categoria Líder",
                        value = leader?.let { "${it.category} (${String.format(Locale.US, "%.0f", it.percentage)}%)" } ?: "—",
                        valueColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                    val totalCategoryPieces = categoryBreakdown.sumOf { it.itemsCount }
                    MiniKpiPill(
                        label = "Total de Obras",
                        value = "$totalCategoryPieces vendidas",
                        valueColor = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Divisão por Categoria de Obras:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (categoryBreakdown.isEmpty()) {
                    Text(
                        text = "Nenhuma obra cadastrada nesta seleção.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                } else {
                    categoryBreakdown.forEach { cat ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.category,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${cat.itemsCount} pcs • ${formatCurrency(cat.total)} (${String.format(Locale.US, "%.0f", cat.percentage)}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF06B6D4)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            StatProgressBar(
                                progress = cat.percentage / 100.0,
                                color = Color(0xFF06B6D4)
                            )
                        }
                    }
                }
            }

            ReportType.FULL -> {
                // KPIs for Full
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        label = "Lucro Estimado",
                        value = formatCurrency(totalProfit),
                        valueColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKpiPill(
                        label = "Descontos Totais",
                        value = if (totalDiscount > 0) "-${formatCurrency(totalDiscount)}" else "R$ 0,00",
                        valueColor = Color(0xFFFB923C),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resumo rápido de pagamentos e categorias
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Formas de Pagamento", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            paymentBreakdown.take(3).forEach { p ->
                                Text(
                                    text = "${p.method}: ${formatCurrency(p.total)}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(text = "Categorias Principais", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            categoryBreakdown.take(3).forEach { c ->
                                Text(
                                    text = "${c.category}: ${formatCurrency(c.total)}",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportViewerDetailContent(
    reportType: ReportType,
    selectedDateText: String,
    filteredSales: List<com.example.data.model.SaleWithItems>,
    totalRevenue: Double,
    totalProfit: Double,
    totalDiscount: Double,
    totalSalesCount: Int,
    paymentBreakdown: List<PaymentMethodBreakdown>,
    categoryBreakdown: List<CategoryBreakdown>,
    weekDaysBreakdown: List<WeekDayBreakdown>,
    bestWeekDay: WeekDayBreakdown?,
    weekDailyAvg: Double,
    monthWeeksBreakdown: List<MonthWeekBreakdown>,
    totalMonthItemsCount: Int,
    dayTicketMedio: Double,
    dayCashSummary: DayCashSummary
) {
    val sdfDate = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }
    val sdfTime = remember { SimpleDateFormat("HH:mm", Locale("pt", "BR")) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        // Period Badge
        Surface(
            color = Color(0xFF2C2C2E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Período Analisado:", fontSize = 12.sp, color = TextMuted)
                Text(text = selectedDateText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // Executive Financial Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF14291F),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0xFF22C55E).copy(alpha = 0.4f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Faturamento Total", fontSize = 11.sp, color = Color(0xFF86EFAC))
                    Text(text = formatCurrency(totalRevenue), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                }
            }

            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0xFF3A3A3C)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Total de Vendas", fontSize = 11.sp, color = TextMuted)
                    Text(text = "$totalSalesCount vendas", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0xFF3A3A3C)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Descontos Aplicados", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = if (totalDiscount > 0) "-${formatCurrency(totalDiscount)}" else "R$ 0,00",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFB923C)
                    )
                }
            }

            Surface(
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.5.dp, Color(0xFF3A3A3C)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(text = "Lucro Estimado", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = formatCurrency(totalProfit),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2C2C2E))

        // Specialized Breakdown Section for the Modal
        when (reportType) {
            ReportType.DAY -> {
                Text(
                    text = "Fechamento de Caixa do Dia",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8),
                    fontSize = 14.sp
                )

                Surface(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PIX:", color = TextSecondary, fontSize = 12.sp)
                            Text(formatCurrency(dayCashSummary.pix), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dinheiro:", color = TextSecondary, fontSize = 12.sp)
                            Text(formatCurrency(dayCashSummary.dinheiro), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cartão de Débito:", color = TextSecondary, fontSize = 12.sp)
                            Text(formatCurrency(dayCashSummary.debito), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Cartão de Crédito:", color = TextSecondary, fontSize = 12.sp)
                            Text(formatCurrency(dayCashSummary.credito), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }

                Text(
                    text = "Linha do Tempo das Vendas (${filteredSales.size}):",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )

                if (filteredSales.isEmpty()) {
                    Text("Nenhuma venda registrada na data selecionada.", color = TextMuted, fontSize = 12.sp)
                } else {
                    filteredSales.forEach { saleWithItems ->
                        val timeStr = sdfTime.format(Date(saleWithItems.sale.timestamp))
                        val productTitle = saleWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                            .ifBlank { "Venda #${saleWithItems.sale.id}" }
                        Surface(
                            color = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(text = timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                        Text(text = "• ${saleWithItems.sale.paymentMethod}", fontSize = 11.sp, color = TextMuted)
                                    }
                                    Text(text = productTitle, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                                }
                                Text(
                                    text = formatCurrency(saleWithItems.sale.totalAmount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                    }
                }
            }

            ReportType.WEEK -> {
                Text(
                    text = "Desempenho dos Últimos 7 Dias",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD946EF),
                    fontSize = 14.sp
                )

                weekDaysBreakdown.forEach { dayStat ->
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = dayStat.dayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = dayStat.dateStr, fontSize = 11.sp, color = TextMuted)
                                }
                                Text(text = "${dayStat.count} vendas realizadas", fontSize = 10.sp, color = TextSecondary)
                            }
                            Text(
                                text = formatCurrency(dayStat.total),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayStat.total > 0) NeonGreen else TextMuted
                            )
                        }
                    }
                }
            }

            ReportType.MONTH -> {
                Text(
                    text = "Evolução por Semanas do Mês",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF22C55E),
                    fontSize = 14.sp
                )

                monthWeeksBreakdown.forEach { w ->
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = w.weekLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "Dias ${w.dateRange} • ${w.count} vendas", fontSize = 10.sp, color = TextMuted)
                            }
                            Text(
                                text = "${formatCurrency(w.total)} (${String.format(Locale.US, "%.0f", w.percentage)}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (w.total > 0) NeonGreen else TextMuted
                            )
                        }
                    }
                }

                if (categoryBreakdown.isNotEmpty()) {
                    Text(
                        text = "Categorias Mais Vendidas no Mês:",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    categoryBreakdown.forEach { cat ->
                        Surface(
                            color = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "${cat.category} (${cat.itemsCount} peças)", fontSize = 12.sp, color = TextPrimary)
                                Text(text = formatCurrency(cat.total), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            }
                        }
                    }
                }
            }

            ReportType.PAYMENT_METHOD -> {
                Text(
                    text = "Detalhamento por Meio de Pagamento",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF97316),
                    fontSize = 14.sp
                )

                paymentBreakdown.forEach { p ->
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(imageVector = p.icon, contentDescription = null, tint = p.color, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(text = p.method, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text(text = "${p.count} transações", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = formatCurrency(p.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.color)
                                Text(text = "${String.format(Locale.US, "%.1f", p.percentage)}% do total", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            ReportType.CATEGORY -> {
                Text(
                    text = "Detalhamento por Categoria de Obras",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06B6D4),
                    fontSize = 14.sp
                )

                categoryBreakdown.forEach { c ->
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = c.category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "${c.itemsCount} obras vendidas", fontSize = 10.sp, color = TextMuted)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = formatCurrency(c.total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                                Text(text = "${String.format(Locale.US, "%.1f", c.percentage)}% do total", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            ReportType.FULL -> {
                Text(
                    text = "Relação Completa de Vendas (${filteredSales.size}):",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.sp
                )

                filteredSales.forEach { saleWithItems ->
                    val dateStr = sdfDate.format(Date(saleWithItems.sale.timestamp))
                    val productTitle = saleWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                        .ifBlank { "Venda #${saleWithItems.sale.id}" }
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "$dateStr • ${saleWithItems.sale.paymentMethod}", fontSize = 10.sp, color = TextMuted)
                                Text(text = productTitle, fontSize = 12.sp, color = TextPrimary, maxLines = 1)
                            }
                            Text(
                                text = formatCurrency(saleWithItems.sale.totalAmount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
