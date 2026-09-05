package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.SaleWithItems
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    private fun formatCurrency(amount: Double): String {
        val ptBr = Locale("pt", "BR")
        return NumberFormat.getCurrencyInstance(ptBr).format(amount)
    }

    private fun drawCartIcon(canvas: Canvas, cx: Float, cy: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            moveTo(cx - 7f, cy - 4.5f)
            lineTo(cx - 4f, cy - 4.5f)
            lineTo(cx - 2f, cy + 3.5f)
            lineTo(cx + 6f, cy + 3.5f)
            lineTo(cx + 7.5f, cy - 2f)
            lineTo(cx - 1f, cy - 2f)
        }
        canvas.drawPath(path, paint)
        val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx - 1.5f, cy + 6.5f, 1.3f, wheelPaint)
        canvas.drawCircle(cx + 4.5f, cy + 6.5f, 1.3f, wheelPaint)
    }

    private fun normalizePaymentMethod(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("Cartão de Crédito", ignoreCase = true) || trimmed.startsWith("Crédito", ignoreCase = true) -> "Cartão de Crédito"
            trimmed.startsWith("Cartão de Débito", ignoreCase = true) || trimmed.startsWith("Débito", ignoreCase = true) -> "Cartão de Débito"
            trimmed.equals("PIX", ignoreCase = true) -> "PIX"
            trimmed.equals("Dinheiro", ignoreCase = true) -> "Dinheiro"
            trimmed.isNotBlank() -> trimmed
            else -> "Outro"
        }
    }

    private data class PaymentRow(
        val method: String,
        val count: Int,
        val total: Double,
        val percentage: Double
    )

    private data class CategoryRow(
        val category: String,
        val count: Int,
        val total: Double,
        val percentage: Double
    )

    private data class WeekDayRow(
        val dayName: String,
        val dateStr: String,
        val count: Int,
        val total: Double,
        val percentage: Double,
        val isBestDay: Boolean
    )

    private data class MonthWeekRow(
        val weekLabel: String,
        val dateRange: String,
        val count: Int,
        val total: Double,
        val percentage: Double
    )

    /**
     * Generates a tailored PDF file matching each specific report type
     */
    fun generateSalesReportPdf(
        context: Context,
        reportTitle: String,
        periodText: String,
        sales: List<SaleWithItems>,
        totalRevenue: Double,
        totalProfit: Double,
        totalDiscount: Double = sales.sumOf { it.sale.discount },
        productCategoryMap: Map<Long, String> = emptyMap(),
        productNameCategoryMap: Map<String, String> = emptyMap(),
        reportType: String = "FULL"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()

            // Standard A4 dimensions: 595 x 842 pt
            val pageWidth = 595
            val pageHeight = 842
            val marginX = 36f
            val contentWidth = pageWidth - (marginX * 2) // 523f
            val rightMargin = marginX + contentWidth

            val ptBr = Locale("pt", "BR")
            val shortDateFmt = SimpleDateFormat("dd/MM/yyyy", ptBr)
            val dateFmtWithTime = SimpleDateFormat("dd/MM/yyyy HH:mm", ptBr)
            val tableRowDateFmt = SimpleDateFormat("dd/MM/yy HH:mm", ptBr)
            val hourOnlyFmt = SimpleDateFormat("HH:mm", ptBr)

            val now = Date()
            val nowTimeStr = dateFmtWithTime.format(now)

            val cal = Calendar.getInstance()
            val currentYear = cal.get(Calendar.YEAR)
            val monthName = SimpleDateFormat("MMMM", ptBr).format(now).replaceFirstChar { it.uppercase() }

            // Determine effective report type
            val effectiveType = when {
                reportType.equals("DAY", ignoreCase = true) || reportTitle.contains("Dia", ignoreCase = true) -> "DAY"
                reportType.equals("WEEK", ignoreCase = true) || reportTitle.contains("Semana", ignoreCase = true) -> "WEEK"
                reportType.equals("MONTH", ignoreCase = true) || reportTitle.contains("Mês", ignoreCase = true) -> "MONTH"
                reportType.equals("PAYMENT_METHOD", ignoreCase = true) || reportTitle.contains("Pagamento", ignoreCase = true) -> "PAYMENT_METHOD"
                reportType.equals("CATEGORY", ignoreCase = true) || reportTitle.contains("Categoria", ignoreCase = true) -> "CATEGORY"
                else -> "FULL"
            }

            // Theme colors per report type
            val (primaryColor, bannerBgColor, badgeText, bannerTitle) = when (effectiveType) {
                "DAY" -> Quad(
                    Color.rgb(2, 132, 199),   // Sky #0284C7
                    Color.rgb(240, 249, 255), // Sky 50 #F0F9FF
                    "FECHAMENTO DE CAIXA DIÁRIO",
                    "Relatório Diário — Fechamento de Caixa"
                )
                "WEEK" -> Quad(
                    Color.rgb(192, 38, 211),  // Fuchsia #C026D3
                    Color.rgb(253, 244, 255), // Fuchsia 50 #FDF4FF
                    "EVOLUÇÃO SEMANAL — 7 DIAS",
                    "Relatório Semanal de Vendas"
                )
                "MONTH" -> Quad(
                    Color.rgb(22, 163, 74),   // Green #16A34A
                    Color.rgb(240, 253, 244), // Green 50 #F0FDF4
                    "CONSOLIDADO MENSAL DE GESTÃO",
                    "Relatório Mensal — $monthName de $currentYear"
                )
                "PAYMENT_METHOD" -> Quad(
                    Color.rgb(234, 88, 12),   // Orange #EA580C
                    Color.rgb(255, 247, 237), // Orange 50 #FFF7ED
                    "ANÁLISE DE FORMAS DE PAGAMENTO",
                    "Relatório Analítico — Formas de Pagamento"
                )
                "CATEGORY" -> Quad(
                    Color.rgb(8, 145, 178),   // Cyan #0891B2
                    Color.rgb(236, 254, 255), // Cyan 50 #ECFEFF
                    "DESEMPENHO POR CATEGORIA",
                    "Relatório Analítico — Vendas por Categoria"
                )
                else -> Quad(
                    Color.rgb(124, 58, 237),  // Purple #7C3AED
                    Color.rgb(245, 243, 255), // Purple 50 #F5F3FF
                    "DOSSIÊ EXECUTIVO GERAL",
                    "Relatório Geral Completo"
                )
            }

            // Period subtitle
            val periodSubtitle = if (periodText.isNotBlank()) {
                periodText
            } else when (effectiveType) {
                "DAY" -> shortDateFmt.format(now)
                "WEEK" -> {
                    val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
                    "${shortDateFmt.format(startCal.time)} — ${shortDateFmt.format(now)}"
                }
                "MONTH", "FULL" -> {
                    val firstDay = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                    val lastDay = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    }
                    "${shortDateFmt.format(firstDay.time)} — ${shortDateFmt.format(lastDay.time)}"
                }
                else -> {
                    if (sales.isNotEmpty()) {
                        val minTime = sales.minOf { it.sale.timestamp }
                        val maxTime = sales.maxOf { it.sale.timestamp }
                        "${shortDateFmt.format(Date(minTime))} — ${shortDateFmt.format(Date(maxTime))}"
                    } else "Todo o Histórico"
                }
            }

            // --- Precompute Data: Formas de Pagamento ---
            val paymentPriority = mapOf(
                "PIX" to 1,
                "Cartão de Débito" to 2,
                "Cartão de Crédito" to 3,
                "Dinheiro" to 4
            )
            val paymentRows = sales
                .groupBy { normalizePaymentMethod(it.sale.paymentMethod) }
                .map { (method, salesList) ->
                    val count = salesList.size
                    val sum = salesList.sumOf { it.sale.totalAmount }
                    val pct = if (totalRevenue > 0.0) (sum / totalRevenue) * 100.0 else 0.0
                    PaymentRow(method, count, sum, pct)
                }
                .sortedWith(compareBy({ paymentPriority[it.method] ?: 99 }, { -it.total }))

            val topPaymentMethod = paymentRows.firstOrNull()?.method ?: "Nenhum"
            val topPaymentPct = paymentRows.firstOrNull()?.let { String.format(Locale.US, "%.0f%%", it.percentage) } ?: "0%"

            // --- Precompute Data: Categorias ---
            val categoryBuckets = mutableMapOf<String, MutableList<Pair<Int, Double>>>()
            sales.forEach { saleWithItems ->
                if (saleWithItems.items.isEmpty()) {
                    val cat = "Sem Categoria"
                    categoryBuckets.getOrPut(cat) { mutableListOf() }.add(1 to saleWithItems.sale.totalAmount)
                } else {
                    saleWithItems.items.forEach { item ->
                        val cat = productCategoryMap[item.productId]?.trim()?.ifBlank { null }
                            ?: productNameCategoryMap[item.productName.trim().lowercase()]?.trim()?.ifBlank { null }
                            ?: "Sem Categoria"
                        val qty = item.quantity.coerceAtLeast(1)
                        val subtotal = if (item.subtotal > 0.0) item.subtotal else (item.unitPrice * qty)
                        categoryBuckets.getOrPut(cat) { mutableListOf() }.add(qty to subtotal)
                    }
                }
            }
            val categoryRows = categoryBuckets.map { (cat, list) ->
                val count = list.sumOf { it.first }
                val total = list.sumOf { it.second }
                val pct = if (totalRevenue > 0.0) (total / totalRevenue) * 100.0 else 0.0
                CategoryRow(cat, count, total, pct)
            }.sortedWith(compareByDescending<CategoryRow> { it.total }.thenBy { it.category })

            val totalCategoryItems = categoryRows.sumOf { it.count }
            val topCategory = categoryRows.firstOrNull()?.category ?: "Nenhuma"
            val topCategoryPct = categoryRows.firstOrNull()?.let { String.format(Locale.US, "%.0f%%", it.percentage) } ?: "0%"

            // --- Precompute Data: Dias da Semana (WEEK) ---
            val weekDayRows = mutableListOf<WeekDayRow>()
            if (effectiveType == "WEEK") {
                val sdfDayName = SimpleDateFormat("EEEE", ptBr)
                val sdfShortDate = SimpleDateFormat("dd/MM", ptBr)
                val daySums = mutableListOf<Double>()
                val tempCal = Calendar.getInstance()

                for (i in 6 downTo 0) {
                    val dCal = Calendar.getInstance().apply {
                        time = tempCal.time
                        add(Calendar.DAY_OF_YEAR, -i)
                    }
                    val dayStart = Calendar.getInstance().apply {
                        time = dCal.time
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val dayEnd = Calendar.getInstance().apply {
                        time = dCal.time
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val daySales = sales.filter { it.sale.timestamp in dayStart..dayEnd }
                    val dTotal = daySales.sumOf { it.sale.totalAmount }
                    daySums.add(dTotal)
                    val rawDay = sdfDayName.format(dCal.time)
                    val cleanDayName = rawDay.substringBefore("-").replaceFirstChar { it.uppercase() }
                    val dateStr = sdfShortDate.format(dCal.time)
                    val pct = if (totalRevenue > 0.0) (dTotal / totalRevenue) * 100.0 else 0.0
                    weekDayRows.add(WeekDayRow(cleanDayName, dateStr, daySales.size, dTotal, pct, false))
                }
                val maxDaySum = daySums.maxOrNull() ?: 0.0
                if (maxDaySum > 0.0) {
                    val bestIdx = daySums.indexOfFirst { it == maxDaySum }
                    if (bestIdx in weekDayRows.indices) {
                        weekDayRows[bestIdx] = weekDayRows[bestIdx].copy(isBestDay = true)
                    }
                }
            }

            // --- Precompute Data: Semanas do Mês (MONTH) ---
            val monthWeekRows = mutableListOf<MonthWeekRow>()
            if (effectiveType == "MONTH") {
                val calRef = Calendar.getInstance()
                val year = calRef.get(Calendar.YEAR)
                val month = calRef.get(Calendar.MONTH)
                val maxDays = calRef.getActualMaximum(Calendar.DAY_OF_MONTH)

                val ranges = listOf(
                    1 to minOf(7, maxDays),
                    8 to minOf(14, maxDays),
                    15 to minOf(21, maxDays),
                    22 to minOf(28, maxDays),
                    29 to maxDays
                ).filter { it.first <= maxDays }

                ranges.forEachIndexed { index, (startDay, endDay) ->
                    val startCal = Calendar.getInstance().apply {
                        set(year, month, startDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val endCal = Calendar.getInstance().apply {
                        set(year, month, endDay, 23, 59, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    val wSales = sales.filter { it.sale.timestamp in startCal.timeInMillis..endCal.timeInMillis }
                    val wSum = wSales.sumOf { it.sale.totalAmount }
                    val pct = if (totalRevenue > 0.0) (wSum / totalRevenue) * 100.0 else 0.0
                    val rangeText = String.format(Locale.getDefault(), "%02d a %02d/%02d", startDay, endDay, month + 1)
                    monthWeekRows.add(MonthWeekRow("Semana ${index + 1}", rangeText, wSales.size, wSum, pct))
                }
            }

            // Paint configurations
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 0.6f
                color = Color.rgb(229, 231, 235) // Slate 200
            }

            val footerY = pageHeight - 34f
            val maxPageContentY = footerY - 14f

            var currentPageIndex = 0
            val activePages = mutableListOf<PdfDocument.Page>()

            fun startNewPage(): Canvas {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                activePages.add(page)
                val c = page.canvas

                // Background
                c.drawColor(Color.WHITE)

                // 1. Top Bar
                bgPaint.color = primaryColor
                c.drawRect(marginX, 24f, rightMargin, 26.5f, bgPaint)

                // 2. Header: ATELIER CONTROL & Página
                textPaint.color = primaryColor
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.textAlign = Paint.Align.LEFT
                c.drawText("ATELIER CONTROL", marginX, 42f, textPaint)

                textPaint.color = Color.rgb(156, 163, 175)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textAlign = Paint.Align.RIGHT
                c.drawText("Página ${currentPageIndex + 1}", rightMargin, 42f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT

                return c
            }

            fun drawFooterOnCanvas(c: Canvas, pIndex: Int) {
                c.drawLine(marginX, footerY - 8f, rightMargin, footerY - 8f, linePaint)

                textPaint.color = Color.rgb(156, 163, 175)
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textAlign = Paint.Align.LEFT
                c.drawText("Atelier Control — Sistema de Gestão", marginX, footerY + 6f, textPaint)

                textPaint.textAlign = Paint.Align.CENTER
                c.drawText("Página ${pIndex + 1}", pageWidth / 2f, footerY + 6f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                c.drawText("Gerado em $nowTimeStr", rightMargin, footerY + 6f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
            }

            var canvas = startNewPage()
            var currentY = 54f

            // Helper to handle page break
            fun ensureSpace(neededHeight: Float) {
                if (currentY + neededHeight > maxPageContentY) {
                    drawFooterOnCanvas(canvas, currentPageIndex)
                    pdfDocument.finishPage(activePages.last())
                    currentPageIndex++
                    canvas = startNewPage()
                    currentY = 54f
                }
            }

            // ================= 1. BANNER DO RELATÓRIO =================
            val bannerHeight = 72f
            val bannerRect = RectF(marginX, currentY, rightMargin, currentY + bannerHeight)
            bgPaint.color = bannerBgColor
            canvas.drawRoundRect(bannerRect, 8f, 8f, bgPaint)

            // Accent Left Bar
            bgPaint.color = primaryColor
            val bannerBar = RectF(marginX, currentY, marginX + 4.5f, currentY + bannerHeight)
            canvas.drawRoundRect(bannerBar, 3f, 3f, bgPaint)

            // Badge Pill on Banner Top-Right
            val badgeWidth = textPaint.measureText(badgeText) + 16f
            val badgeRect = RectF(rightMargin - badgeWidth - 14f, currentY + 10f, rightMargin - 14f, currentY + 26f)
            bgPaint.color = Color.WHITE
            canvas.drawRoundRect(badgeRect, 8f, 8f, bgPaint)

            textPaint.color = primaryColor
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + 2.8f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            // Banner Title
            textPaint.color = Color.rgb(17, 24, 39)
            textPaint.textSize = 13.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(bannerTitle, marginX + 16f, currentY + 24f, textPaint)

            // Banner Period
            textPaint.color = Color.rgb(107, 114, 128)
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Período: $periodSubtitle", marginX + 16f, currentY + 41f, textPaint)

            // Banner Subtext
            textPaint.color = Color.rgb(156, 163, 175)
            textPaint.textSize = 8f
            canvas.drawText("Documento oficial emitido em $nowTimeStr", marginX + 16f, currentY + 58f, textPaint)

            currentY += bannerHeight + 14f

            // ================= 2. CARDS DE INDICADORES (2x2) =================
            val cardWidth = (contentWidth - 14f) / 2f
            val cardHeight = 48f

            fun drawKpiCard(
                left: Float,
                top: Float,
                width: Float,
                title: String,
                value: String,
                accentColor: Int,
                iconType: String
            ) {
                val cardRect = RectF(left, top, left + width, top + cardHeight)
                bgPaint.color = Color.rgb(249, 250, 252)
                canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

                bgPaint.color = accentColor
                val barRect = RectF(left, top, left + 4f, top + cardHeight)
                canvas.drawRoundRect(barRect, 2f, 2f, bgPaint)

                textPaint.color = Color.rgb(107, 114, 128)
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(title, left + 14f, top + 17f, textPaint)

                textPaint.color = Color.rgb(17, 24, 39)
                textPaint.textSize = 14f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val truncatedVal = if (value.length > 22) value.take(19) + "..." else value
                canvas.drawText(truncatedVal, left + 14f, top + 38f, textPaint)

                // Icon / Pill Indicator
                val iconRight = left + width - 10f
                when (iconType) {
                    "CART" -> {
                        val iRect = RectF(iconRight - 24f, top + 11f, iconRight, top + 35f)
                        bgPaint.color = Color.rgb(243, 232, 255)
                        canvas.drawRoundRect(iRect, 6f, 6f, bgPaint)
                        drawCartIcon(canvas, iRect.centerX(), iRect.centerY(), accentColor)
                    }
                    "CASH" -> {
                        bgPaint.color = Color.rgb(20, 83, 45)
                        canvas.drawCircle(iconRight - 12f, top + 23f, 11f, bgPaint)
                        textPaint.color = Color.WHITE
                        textPaint.textSize = 9f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("R$", iconRight - 12f, top + 26.5f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT
                    }
                    "DISCOUNT" -> {
                        val iRect = RectF(iconRight - 24f, top + 11f, iconRight, top + 35f)
                        bgPaint.color = Color.rgb(254, 243, 199)
                        canvas.drawRoundRect(iRect, 6f, 6f, bgPaint)
                        textPaint.color = Color.rgb(180, 83, 9)
                        textPaint.textSize = 12f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("%", iRect.centerX(), iRect.centerY() + 4f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT
                    }
                    else -> {
                        val iRect = RectF(iconRight - 24f, top + 11f, iconRight, top + 35f)
                        bgPaint.color = Color.rgb(224, 242, 254)
                        canvas.drawRoundRect(iRect, 6f, 6f, bgPaint)
                        textPaint.color = accentColor
                        textPaint.textSize = 10.5f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textPaint.textAlign = Paint.Align.CENTER
                        canvas.drawText("★", iRect.centerX(), iRect.centerY() + 3.5f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT
                    }
                }
            }

            val row1Top = currentY
            val row2Top = row1Top + cardHeight + 10f

            when (effectiveType) {
                "DAY" -> {
                    val dayAvg = if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
                    drawKpiCard(marginX, row1Top, cardWidth, "Vendas Hoje", "${sales.size} vendas", primaryColor, "CART")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento do Dia", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Descontos Concedidos", if (totalDiscount > 0.0) "-${formatCurrency(totalDiscount)}" else "-R$ 0,00", Color.rgb(245, 158, 11), "DISCOUNT")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Ticket Médio do Dia", formatCurrency(dayAvg), Color.rgb(14, 165, 233), "STAR")
                }
                "WEEK" -> {
                    val dailyAvg = totalRevenue / 7.0
                    val bestDayName = weekDayRows.firstOrNull { it.isBestDay }?.dayName ?: "N/D"
                    drawKpiCard(marginX, row1Top, cardWidth, "Vendas na Semana", "${sales.size} vendas", primaryColor, "CART")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento Semanal", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Média Diária (7 dias)", "${formatCurrency(dailyAvg)}/dia", Color.rgb(217, 70, 239), "STAR")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Melhor Dia", bestDayName, Color.rgb(168, 85, 247), "STAR")
                }
                "MONTH" -> {
                    val monthAvg = if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
                    drawKpiCard(marginX, row1Top, cardWidth, "Vendas no Mês", "${sales.size} vendas", primaryColor, "CART")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento Mensal", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Peças Comercializadas", "$totalCategoryItems peças", Color.rgb(16, 185, 129), "STAR")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Ticket Médio", formatCurrency(monthAvg), Color.rgb(14, 165, 233), "CASH")
                }
                "PAYMENT_METHOD" -> {
                    val ticketGeral = if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
                    drawKpiCard(marginX, row1Top, cardWidth, "Volume de Transações", "${sales.size} vendas", primaryColor, "CART")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento Total", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Forma Líder", "$topPaymentMethod ($topPaymentPct)", Color.rgb(234, 88, 12), "STAR")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Ticket Médio Geral", formatCurrency(ticketGeral), Color.rgb(14, 165, 233), "CASH")
                }
                "CATEGORY" -> {
                    val ticketPeca = if (totalCategoryItems > 0) totalRevenue / totalCategoryItems else 0.0
                    drawKpiCard(marginX, row1Top, cardWidth, "Obras Vendidas", "$totalCategoryItems peças", primaryColor, "STAR")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento Total", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Categoria Líder", "$topCategory ($topCategoryPct)", Color.rgb(8, 145, 178), "STAR")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Ticket Médio / Peça", formatCurrency(ticketPeca), Color.rgb(14, 165, 233), "CASH")
                }
                else -> { // FULL
                    drawKpiCard(marginX, row1Top, cardWidth, "Total de Vendas", "${sales.size} vendas", primaryColor, "CART")
                    drawKpiCard(marginX + cardWidth + 14f, row1Top, cardWidth, "Faturamento Bruto", formatCurrency(totalRevenue), Color.rgb(34, 197, 94), "CASH")
                    drawKpiCard(marginX, row2Top, cardWidth, "Descontos Aplicados", if (totalDiscount > 0.0) "-${formatCurrency(totalDiscount)}" else "-R$ 0,00", Color.rgb(245, 158, 11), "DISCOUNT")
                    drawKpiCard(marginX + cardWidth + 14f, row2Top, cardWidth, "Lucro Estimado", formatCurrency(totalProfit), Color.rgb(14, 165, 233), "CASH")
                }
            }

            currentY = row2Top + cardHeight + 20f

            // ================= 3. SEÇÕES ESPECÍFICAS DE CADA RELATÓRIO =================

            fun drawSectionHeader(title: String, color: Int) {
                ensureSpace(40f)
                textPaint.color = color
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(title, marginX, currentY + 10f, textPaint)
                currentY += 18f
            }

            // --- CASO 1: RELATÓRIO DO DIA (FECHAMENTO DE CAIXA) ---
            if (effectiveType == "DAY") {
                drawSectionHeader("DEMONSTRATIVO DE CAIXA POR FORMA DE RECEBIMENTO", primaryColor)

                // Header da Tabela de Caixa
                val payHeaderRect = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(payHeaderRect, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FORMA DE RECEBIMENTO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("QTD TRANSAÇÕES", marginX + 220f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL RECEBIDO", marginX + 340f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO CAIXA", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                if (paymentRows.isEmpty()) {
                    ensureSpace(24f)
                    textPaint.color = Color.rgb(148, 163, 184)
                    textPaint.textSize = 8.5f
                    canvas.drawText("Nenhuma entrada de caixa registrada hoje.", marginX + 8f, currentY + 15f, textPaint)
                    currentY += 22f
                } else {
                    paymentRows.forEachIndexed { idx, row ->
                        ensureSpace(22f)
                        val rowY = currentY
                        if (idx % 2 == 0) {
                            bgPaint.color = Color.rgb(248, 250, 252)
                            canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                        }
                        canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                        textPaint.color = Color.rgb(31, 41, 55)
                        textPaint.textSize = 8f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(row.method, marginX + 8f, rowY + 14.5f, textPaint)

                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText("${row.count} vendas", marginX + 220f, rowY + 14.5f, textPaint)
                        canvas.drawText(formatCurrency(row.total), marginX + 340f, rowY + 14.5f, textPaint)

                        textPaint.textAlign = Paint.Align.RIGHT
                        val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                        canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT
                        currentY += 22f
                    }

                    // Total do Caixa
                    ensureSpace(22f)
                    val totRowY = currentY
                    bgPaint.color = Color.rgb(224, 242, 254) // Sky 100
                    canvas.drawRect(marginX, totRowY, rightMargin, totRowY + 22f, bgPaint)

                    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(2, 132, 199)
                        strokeWidth = 1f
                    }
                    canvas.drawLine(marginX, totRowY, rightMargin, totRowY, borderPaint)
                    canvas.drawLine(marginX, totRowY + 22f, rightMargin, totRowY + 22f, borderPaint)

                    textPaint.color = Color.rgb(17, 24, 39)
                    textPaint.textSize = 8.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TOTAL FECHAMENTO DO DIA", marginX + 8f, totRowY + 14.5f, textPaint)
                    canvas.drawText("${sales.size} vendas", marginX + 220f, totRowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(totalRevenue), marginX + 340f, totRowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("100%", rightMargin - 10f, totRowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 28f
                }

                // Seção 2: Linha do Tempo das Vendas do Dia
                drawSectionHeader("RELAÇÃO DETALHADA DAS VENDAS DO DIA", primaryColor)

                fun drawDaySalesHeader() {
                    val headerRect = RectF(marginX, currentY, rightMargin, currentY + 22f)
                    bgPaint.color = Color.rgb(51, 65, 85) // Slate 700
                    canvas.drawRoundRect(headerRect, 5f, 5f, bgPaint)

                    textPaint.color = Color.WHITE
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("HORA", marginX + 8f, currentY + 14.5f, textPaint)
                    canvas.drawText("OBRA / PRODUTO", marginX + 60f, currentY + 14.5f, textPaint)
                    canvas.drawText("PAGAMENTO", marginX + 260f, currentY + 14.5f, textPaint)
                    canvas.drawText("DESCONTO", marginX + 380f, currentY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("VALOR LÍQUIDO", rightMargin - 10f, currentY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }

                drawDaySalesHeader()

                if (sales.isEmpty()) {
                    ensureSpace(24f)
                    textPaint.color = Color.rgb(148, 163, 184)
                    textPaint.textSize = 8.5f
                    canvas.drawText("Nenhuma transação efetuada hoje.", marginX + 8f, currentY + 16f, textPaint)
                    currentY += 24f
                } else {
                    sales.forEachIndexed { rowIndex, saleWithItems ->
                        ensureSpace(22f)
                        val rowY = currentY
                        if (rowIndex % 2 == 0) {
                            bgPaint.color = Color.rgb(248, 250, 252)
                            canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                        }
                        canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                        textPaint.color = Color.rgb(31, 41, 55)
                        textPaint.textSize = 8f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        canvas.drawText(hourOnlyFmt.format(Date(saleWithItems.sale.timestamp)), marginX + 8f, rowY + 14.5f, textPaint)

                        val pName = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${saleWithItems.sale.id}"
                        val truncProduct = if (pName.length > 28) pName.take(25) + "..." else pName
                        canvas.drawText(truncProduct, marginX + 60f, rowY + 14.5f, textPaint)

                        val payStr = saleWithItems.sale.paymentMethod.ifBlank { "PIX" }
                        val truncPay = if (payStr.length > 18) payStr.take(16) + "..." else payStr
                        canvas.drawText(truncPay, marginX + 260f, rowY + 14.5f, textPaint)

                        val discStr = if (saleWithItems.sale.discount > 0.0) "-${formatCurrency(saleWithItems.sale.discount)}" else "R$ 0,00"
                        canvas.drawText(discStr, marginX + 380f, rowY + 14.5f, textPaint)

                        textPaint.textAlign = Paint.Align.RIGHT
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText(formatCurrency(saleWithItems.sale.totalAmount), rightMargin - 10f, rowY + 14.5f, textPaint)
                        textPaint.textAlign = Paint.Align.LEFT

                        currentY += 22f
                    }
                }

                // Conferência e Assinatura de Fechamento de Caixa
                ensureSpace(70f)
                currentY += 14f
                val signRect = RectF(marginX, currentY, rightMargin, currentY + 54f)
                bgPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(signRect, 6f, 6f, bgPaint)

                val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(203, 213, 225)
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }
                canvas.drawRoundRect(signRect, 6f, 6f, boxBorderPaint)

                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TERMO DE CONFERÊNCIA DE FECHAMENTO DE CAIXA", marginX + 12f, currentY + 16f, textPaint)

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.textSize = 7.5f
                canvas.drawText("[   ] Caixa conferido e conciliado integralmente           [   ] Divergência apontada", marginX + 12f, currentY + 32f, textPaint)
                canvas.drawText("Operador/Responsável: _____________________________________   Visto/Gerente: _____________________________________", marginX + 12f, currentY + 46f, textPaint)
                currentY += 64f
            }

            // --- CASO 2: RELATÓRIO DA SEMANA (EVOLUÇÃO DOS 7 DIAS) ---
            else if (effectiveType == "WEEK") {
                drawSectionHeader("DESEMPENHO DIÁRIO NA SEMANA (ÚLTIMOS 7 DIAS)", primaryColor)

                // Header da Tabela Semanal
                val weekHeaderRect = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(weekHeaderRect, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DIA DA SEMANA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("DATA", marginX + 130f, currentY + 14.5f, textPaint)
                canvas.drawText("VENDAS", marginX + 210f, currentY + 14.5f, textPaint)
                canvas.drawText("FATURAMENTO (R$)", marginX + 290f, currentY + 14.5f, textPaint)
                canvas.drawText("% SEMANA", marginX + 410f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("STATUS", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                weekDayRows.forEachIndexed { idx, day ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (day.isBestDay) {
                        bgPaint.color = Color.rgb(253, 244, 255) // Fuchsia highlight
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    } else if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = if (day.isBestDay) primaryColor else Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (day.isBestDay) Typeface.BOLD else Typeface.NORMAL)
                    canvas.drawText(day.dayName, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.color = Color.rgb(75, 85, 99)
                    canvas.drawText(day.dateStr, marginX + 130f, rowY + 14.5f, textPaint)
                    canvas.drawText("${day.count}", marginX + 210f, rowY + 14.5f, textPaint)

                    textPaint.color = Color.rgb(17, 24, 39)
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(formatCurrency(day.total), marginX + 290f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    val pctStr = if (day.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", day.percentage)
                    canvas.drawText(pctStr, marginX + 410f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    if (day.isBestDay) {
                        textPaint.color = primaryColor
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        canvas.drawText("★ MELHOR DIA", rightMargin - 10f, rowY + 14.5f, textPaint)
                    } else {
                        textPaint.color = Color.rgb(156, 163, 175)
                        canvas.drawText("—", rightMargin - 10f, rowY + 14.5f, textPaint)
                    }
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }

                // Linha Total da Semana
                ensureSpace(22f)
                val totRowY = currentY
                bgPaint.color = Color.rgb(250, 232, 255)
                canvas.drawRect(marginX, totRowY, rightMargin, totRowY + 22f, bgPaint)

                textPaint.color = Color.rgb(17, 24, 39)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAL DA SEMANA", marginX + 8f, totRowY + 14.5f, textPaint)
                canvas.drawText("${sales.size} vendas", marginX + 210f, totRowY + 14.5f, textPaint)
                canvas.drawText(formatCurrency(totalRevenue), marginX + 290f, totRowY + 14.5f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("100%", rightMargin - 10f, totRowY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 28f

                // Resumo por Forma de Pagamento na Semana
                drawSectionHeader("PAGAMENTOS NA SEMANA", primaryColor)
                val payHeaderRect = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(126, 34, 206) // Purple 700
                canvas.drawRoundRect(payHeaderRect, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FORMA DE PAGAMENTO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("QTD", marginX + 230f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL", marginX + 330f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                paymentRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(250, 245, 255)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(row.method, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count}", marginX + 230f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 330f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
                currentY += 10f

                // Relação de Vendas da Semana
                drawSectionHeader("HISTÓRICO DE VENDAS DA SEMANA", primaryColor)
                val salesHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(51, 65, 85)
                canvas.drawRoundRect(salesHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DATA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("PRODUTO / OBRA", marginX + 88f, currentY + 14.5f, textPaint)
                canvas.drawText("PAGAMENTO", marginX + 260f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("VALOR TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                sales.forEachIndexed { rowIndex, saleWithItems ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (rowIndex % 2 == 0) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(tableRowDateFmt.format(Date(saleWithItems.sale.timestamp)), marginX + 8f, rowY + 14.5f, textPaint)

                    val pName = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${saleWithItems.sale.id}"
                    val truncP = if (pName.length > 28) pName.take(25) + "..." else pName
                    canvas.drawText(truncP, marginX + 88f, rowY + 14.5f, textPaint)

                    val pay = saleWithItems.sale.paymentMethod.ifBlank { "PIX" }
                    val truncPay = if (pay.length > 20) pay.take(18) + "..." else pay
                    canvas.drawText(truncPay, marginX + 260f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(formatCurrency(saleWithItems.sale.totalAmount), rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
            }

            // --- CASO 3: RELATÓRIO DO MÊS (SEMANAS E CATEGORIAS) ---
            else if (effectiveType == "MONTH") {
                drawSectionHeader("EVOLUÇÃO POR SEMANAS DO MÊS", primaryColor)

                // Header da Tabela por Semanas
                val mHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(mHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("BLOCO SEMANAL", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("PERÍODO (DIAS)", marginX + 140f, currentY + 14.5f, textPaint)
                canvas.drawText("VENDAS", marginX + 250f, currentY + 14.5f, textPaint)
                canvas.drawText("FATURAMENTO", marginX + 340f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO MÊS", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                monthWeekRows.forEachIndexed { idx, mw ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(mw.weekLabel, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(mw.dateRange, marginX + 140f, rowY + 14.5f, textPaint)
                    canvas.drawText("${mw.count} vendas", marginX + 250f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(mw.total), marginX + 340f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (mw.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", mw.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }

                // Total Mês
                ensureSpace(22f)
                val totRowY = currentY
                bgPaint.color = Color.rgb(220, 252, 231)
                canvas.drawRect(marginX, totRowY, rightMargin, totRowY + 22f, bgPaint)

                textPaint.color = Color.rgb(17, 24, 39)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAL MENSAL CONSOLIDADO", marginX + 8f, totRowY + 14.5f, textPaint)
                canvas.drawText("${sales.size} vendas", marginX + 250f, totRowY + 14.5f, textPaint)
                canvas.drawText(formatCurrency(totalRevenue), marginX + 340f, totRowY + 14.5f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("100%", rightMargin - 10f, totRowY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 28f

                // Categorias de Obras Vendidas no Mês
                drawSectionHeader("DESEMPENHO POR CATEGORIA DE OBRAS NO MÊS", Color.rgb(13, 148, 136))
                val catHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(13, 148, 136)
                canvas.drawRoundRect(catHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("CATEGORIA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("PEÇAS VENDIDAS", marginX + 220f, currentY + 14.5f, textPaint)
                canvas.drawText("FATURAMENTO", marginX + 340f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO MÊS", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                categoryRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(240, 253, 250)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val catTrunc = if (row.category.length > 28) row.category.take(25) + "..." else row.category
                    canvas.drawText(catTrunc, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count} peças", marginX + 220f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 340f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
                currentY += 10f

                // Formas de Pagamento do Mês
                drawSectionHeader("RECEBIMENTOS DO MÊS", Color.rgb(234, 88, 12))
                val payHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(234, 88, 12)
                canvas.drawRoundRect(payHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FORMA DE PAGAMENTO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("TRANSAÇÕES", marginX + 230f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL", marginX + 330f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% PARTICIPAÇÃO", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                paymentRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(255, 251, 235)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(row.method, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count}", marginX + 230f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 330f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
            }

            // --- CASO 4: RELATÓRIO POR FORMA DE PAGAMENTO ---
            else if (effectiveType == "PAYMENT_METHOD") {
                drawSectionHeader("MATRIZ COMPARATIVA DE FORMAS DE PAGAMENTO", primaryColor)

                val payHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(payHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FORMA DE PAGAMENTO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("TRANSAÇÕES", marginX + 180f, currentY + 14.5f, textPaint)
                canvas.drawText("TICKET MÉDIO", marginX + 280f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL RECEBIDO", marginX + 390f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% PART.", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                paymentRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(255, 251, 235)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(row.method, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count} vendas", marginX + 180f, rowY + 14.5f, textPaint)

                    val tMedio = if (row.count > 0) row.total / row.count else 0.0
                    canvas.drawText(formatCurrency(tMedio), marginX + 280f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 390f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }

                // Total Row
                ensureSpace(22f)
                val totRowY = currentY
                bgPaint.color = Color.rgb(254, 243, 199)
                canvas.drawRect(marginX, totRowY, rightMargin, totRowY + 22f, bgPaint)

                textPaint.color = Color.rgb(17, 24, 39)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAL GERAL CONSOLIDADO", marginX + 8f, totRowY + 14.5f, textPaint)
                canvas.drawText("${sales.size} vendas", marginX + 180f, totRowY + 14.5f, textPaint)
                val totalAvg = if (sales.isNotEmpty()) totalRevenue / sales.size else 0.0
                canvas.drawText(formatCurrency(totalAvg), marginX + 280f, totRowY + 14.5f, textPaint)
                canvas.drawText(formatCurrency(totalRevenue), marginX + 390f, totRowY + 14.5f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("100%", rightMargin - 10f, totRowY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 28f

                // Detalhamento das Vendas
                drawSectionHeader("VENDAS FILTRADAS POR FORMA DE PAGAMENTO", primaryColor)
                val salesHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(51, 65, 85)
                canvas.drawRoundRect(salesHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DATA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("FORMA DE PAGAMENTO", marginX + 88f, currentY + 14.5f, textPaint)
                canvas.drawText("PRODUTO / OBRA", marginX + 230f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("VALOR TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                sales.forEachIndexed { rowIndex, saleWithItems ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (rowIndex % 2 == 0) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(tableRowDateFmt.format(Date(saleWithItems.sale.timestamp)), marginX + 8f, rowY + 14.5f, textPaint)

                    val pay = saleWithItems.sale.paymentMethod.ifBlank { "PIX" }
                    val truncPay = if (pay.length > 20) pay.take(18) + "..." else pay
                    canvas.drawText(truncPay, marginX + 88f, rowY + 14.5f, textPaint)

                    val pName = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${saleWithItems.sale.id}"
                    val truncP = if (pName.length > 28) pName.take(25) + "..." else pName
                    canvas.drawText(truncP, marginX + 230f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(formatCurrency(saleWithItems.sale.totalAmount), rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
            }

            // --- CASO 5: RELATÓRIO POR CATEGORIA DE OBRAS ---
            else if (effectiveType == "CATEGORY") {
                drawSectionHeader("PERFORMANCE COMERCIAL POR CATEGORIA DE OBRAS", primaryColor)

                val catHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(catHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("CATEGORIA / COLEÇÃO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("PEÇAS VENDIDAS", marginX + 180f, currentY + 14.5f, textPaint)
                canvas.drawText("TICKET / PEÇA", marginX + 280f, currentY + 14.5f, textPaint)
                canvas.drawText("FATURAMENTO", marginX + 390f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% PART.", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                categoryRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(240, 253, 250)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val truncCat = if (row.category.length > 24) row.category.take(21) + "..." else row.category
                    canvas.drawText(truncCat, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count} peças", marginX + 180f, rowY + 14.5f, textPaint)

                    val tMedio = if (row.count > 0) row.total / row.count else 0.0
                    canvas.drawText(formatCurrency(tMedio), marginX + 280f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 390f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }

                // Total Row
                ensureSpace(22f)
                val totRowY = currentY
                bgPaint.color = Color.rgb(204, 251, 241)
                canvas.drawRect(marginX, totRowY, rightMargin, totRowY + 22f, bgPaint)

                textPaint.color = Color.rgb(17, 24, 39)
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("TOTAL GERAL DE OBRAS", marginX + 8f, totRowY + 14.5f, textPaint)
                canvas.drawText("$totalCategoryItems peças", marginX + 180f, totRowY + 14.5f, textPaint)
                val ticketPecaGeral = if (totalCategoryItems > 0) totalRevenue / totalCategoryItems else 0.0
                canvas.drawText(formatCurrency(ticketPecaGeral), marginX + 280f, totRowY + 14.5f, textPaint)
                canvas.drawText(formatCurrency(totalRevenue), marginX + 390f, totRowY + 14.5f, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("100%", rightMargin - 10f, totRowY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 28f

                // Detalhamento das Obras Vendidas
                drawSectionHeader("RELAÇÃO DE OBRAS VENDIDAS NO PERÍODO", primaryColor)
                val obrasHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(51, 65, 85)
                canvas.drawRoundRect(obrasHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DATA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("CATEGORIA", marginX + 88f, currentY + 14.5f, textPaint)
                canvas.drawText("OBRA / PRODUTO", marginX + 210f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("VALOR TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                sales.forEachIndexed { rowIndex, saleWithItems ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (rowIndex % 2 == 0) {
                        bgPaint.color = Color.rgb(248, 250, 252)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(tableRowDateFmt.format(Date(saleWithItems.sale.timestamp)), marginX + 8f, rowY + 14.5f, textPaint)

                    val firstItem = saleWithItems.items.firstOrNull()
                    val cat = firstItem?.let {
                        productCategoryMap[it.productId] ?: productNameCategoryMap[it.productName.lowercase()]
                    } ?: "Sem Categoria"
                    val truncCat = if (cat.length > 20) cat.take(18) + "..." else cat
                    canvas.drawText(truncCat, marginX + 88f, rowY + 14.5f, textPaint)

                    val pName = firstItem?.productName ?: "Venda #${saleWithItems.sale.id}"
                    val truncP = if (pName.length > 28) pName.take(25) + "..." else pName
                    canvas.drawText(truncP, marginX + 210f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(formatCurrency(saleWithItems.sale.totalAmount), rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
            }

            // --- CASO 6: RELATÓRIO COMPLETO (DOSSIÊ EXECUTIVO) ---
            else {
                // Seção 1: Resumo de Formas de Pagamento
                drawSectionHeader("RESUMO POR FORMA DE PAGAMENTO", Color.rgb(234, 88, 12))
                val payHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(234, 88, 12)
                canvas.drawRoundRect(payHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FORMA DE PAGAMENTO", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("QTD", marginX + 230f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL", marginX + 330f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                paymentRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(255, 251, 235)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(row.method, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count}", marginX + 230f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 330f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
                currentY += 10f

                // Seção 2: Resumo por Categoria
                drawSectionHeader("RESUMO POR CATEGORIA DE OBRAS", Color.rgb(8, 145, 178))
                val catHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = Color.rgb(8, 145, 178)
                canvas.drawRoundRect(catHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("CATEGORIA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("QTD", marginX + 230f, currentY + 14.5f, textPaint)
                canvas.drawText("TOTAL", marginX + 330f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("% DO TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                categoryRows.forEachIndexed { idx, row ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (idx % 2 == 0) {
                        bgPaint.color = Color.rgb(240, 253, 250)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val catTrunc = if (row.category.length > 28) row.category.take(25) + "..." else row.category
                    canvas.drawText(catTrunc, marginX + 8f, rowY + 14.5f, textPaint)

                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("${row.count}", marginX + 230f, rowY + 14.5f, textPaint)
                    canvas.drawText(formatCurrency(row.total), marginX + 330f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    val pctStr = if (row.percentage >= 99.95) "100%" else String.format(Locale.US, "%.1f%%", row.percentage)
                    canvas.drawText(pctStr, rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
                currentY += 10f

                // Seção 3: Histórico Completo de Vendas
                drawSectionHeader("HISTÓRICO GERAL DE VENDAS", primaryColor)
                val fullSalesHdr = RectF(marginX, currentY, rightMargin, currentY + 22f)
                bgPaint.color = primaryColor
                canvas.drawRoundRect(fullSalesHdr, 5f, 5f, bgPaint)

                textPaint.color = Color.WHITE
                textPaint.textSize = 8f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DATA", marginX + 8f, currentY + 14.5f, textPaint)
                canvas.drawText("PRODUTO / OBRA", marginX + 88f, currentY + 14.5f, textPaint)
                canvas.drawText("PAGAMENTO", marginX + 260f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("VALOR TOTAL", rightMargin - 10f, currentY + 14.5f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                currentY += 22f

                sales.forEachIndexed { rowIndex, saleWithItems ->
                    ensureSpace(22f)
                    val rowY = currentY
                    if (rowIndex % 2 == 0) {
                        bgPaint.color = Color.rgb(250, 249, 254)
                        canvas.drawRect(marginX, rowY, rightMargin, rowY + 22f, bgPaint)
                    }
                    canvas.drawLine(marginX, rowY + 22f, rightMargin, rowY + 22f, linePaint)

                    textPaint.color = Color.rgb(31, 41, 55)
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(tableRowDateFmt.format(Date(saleWithItems.sale.timestamp)), marginX + 8f, rowY + 14.5f, textPaint)

                    val pName = saleWithItems.items.firstOrNull()?.productName ?: "Venda #${saleWithItems.sale.id}"
                    val truncP = if (pName.length > 28) pName.take(25) + "..." else pName
                    canvas.drawText(truncP, marginX + 88f, rowY + 14.5f, textPaint)

                    val pay = saleWithItems.sale.paymentMethod.ifBlank { "PIX" }
                    val truncPay = if (pay.length > 20) pay.take(18) + "..." else pay
                    canvas.drawText(truncPay, marginX + 260f, rowY + 14.5f, textPaint)

                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(formatCurrency(saleWithItems.sale.totalAmount), rightMargin - 10f, rowY + 14.5f, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    currentY += 22f
                }
            }

            // Finish Final Page
            drawFooterOnCanvas(canvas, currentPageIndex)
            pdfDocument.finishPage(activePages.last())

            // Save PDF to reports cache directory
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cleanType = effectiveType.lowercase()
            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(reportsDir, "Relatorio_${cleanType}_$timestampStr.pdf")

            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Opens the generated PDF file in the system default or chosen PDF viewer.
     */
    fun openPdf(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir Relatório"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Não foi possível abrir visualizador de PDF: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Shares the generated PDF file via standard Android share sheet (WhatsApp, Email, Drive, etc).
     */
    fun sharePdf(context: Context, file: File) {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Atelier Control")
                putExtra(Intent.EXTRA_TEXT, "Segue em anexo o relatório gerado pelo Atelier Control.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Compartilhar Relatório PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Erro ao compartilhar PDF: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
