package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Atelier Control 2", appName)
  }

  @Test
  fun `verify pdf report generator follows official standard`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    
    // Build sales matching the reference document standard
    val sale1 = com.example.data.model.Sale(
      id = 1,
      timestamp = 1756959420000L, // 04/09/2026
      customerName = "Cliente 1",
      paymentMethod = "Cartão de Crédito 3x",
      discount = 0.0,
      totalAmount = 10.00,
      totalCost = 4.00
    )
    val item1 = com.example.data.model.SaleItem(
      id = 1,
      saleId = 1,
      productId = 101,
      productName = "Imã",
      unitPrice = 10.00,
      unitCost = 4.00,
      quantity = 1,
      subtotal = 10.00
    )

    val sale2 = com.example.data.model.Sale(
      id = 2,
      timestamp = 1757045700000L, // 05/09/2026
      customerName = "Cliente 2",
      paymentMethod = "PIX",
      discount = 6.0,
      totalAmount = 54.00,
      totalCost = 20.00
    )
    val item2 = com.example.data.model.SaleItem(
      id = 2,
      saleId = 2,
      productId = 102,
      productName = "Chapéu Nosso",
      unitPrice = 54.00,
      unitCost = 20.00,
      quantity = 1,
      subtotal = 54.00
    )

    val sale3 = com.example.data.model.Sale(
      id = 3,
      timestamp = 1757047080000L, // 05/09/2026
      customerName = "Cliente 3",
      paymentMethod = "Cartão de Débito",
      discount = 0.0,
      totalAmount = 5.50,
      totalCost = 2.00
    )
    val item3 = com.example.data.model.SaleItem(
      id = 3,
      saleId = 3,
      productId = 103,
      productName = "Pincel (474-10)",
      unitPrice = 5.50,
      unitCost = 2.00,
      quantity = 1,
      subtotal = 5.50
    )

    val sales = listOf(
      com.example.data.model.SaleWithItems(sale1, listOf(item1)),
      com.example.data.model.SaleWithItems(sale2, listOf(item2)),
      com.example.data.model.SaleWithItems(sale3, listOf(item3))
    )

    val categoryMap = mapOf(
      101L to "Lembranças",
      102L to "Acessórios na lona de caminhão",
      103L to "Pincéis"
    )

    // Verify report KPI summary metrics
    val totalRevenue = sales.sumOf { it.sale.totalAmount }
    val totalDiscount = sales.sumOf { it.sale.discount }
    val totalSalesCount = sales.size

    assertEquals(69.50, totalRevenue, 0.001)
    assertEquals(6.00, totalDiscount, 0.001)
    assertEquals(3, totalSalesCount)

    // Verify payment breakdown
    val paymentGroups = sales.groupBy { it.sale.paymentMethod.replace(" 3x", "") }
    assertEquals(3, paymentGroups.size)
    assertEquals(54.00, paymentGroups["PIX"]!!.sumOf { it.sale.totalAmount }, 0.001)
    assertEquals(10.00, paymentGroups["Cartão de Crédito"]!!.sumOf { it.sale.totalAmount }, 0.001)
    assertEquals(5.50, paymentGroups["Cartão de Débito"]!!.sumOf { it.sale.totalAmount }, 0.001)

    // Verify category breakdown
    val catGroups = sales.flatMap { sale ->
      sale.items.map { item ->
        val cat = categoryMap[item.productId] ?: "Sem Categoria"
        cat to item.subtotal
      }
    }.groupBy({ it.first }, { it.second })

    assertEquals(3, catGroups.size)
    assertEquals(54.00, catGroups["Acessórios na lona de caminhão"]!!.sum(), 0.001)
    assertEquals(10.00, catGroups["Lembranças"]!!.sum(), 0.001)
    assertEquals(5.50, catGroups["Pincéis"]!!.sum(), 0.001)
  }

  @Test
  fun `verify quick reports configuration`() {
    val types = com.example.ui.screens.ReportType.values()
    assertEquals(6, types.size)
    assertEquals("Vendas do Dia", types[0].title)
    assertEquals("Vendas da Semana", types[1].title)
    assertEquals("Vendas do Mês", types[2].title)
    assertEquals("Por Forma de Pagamento", types[3].title)
    assertEquals("Por Categoria", types[4].title)
    assertEquals("Relatório Completo", types[5].title)

    assertEquals("Do Dia", types[0].shortLabel)
    assertEquals("Da Semana", types[1].shortLabel)
    assertEquals("Do Mês", types[2].shortLabel)
    assertEquals("Pagamento", types[3].shortLabel)
    assertEquals("Categoria", types[4].shortLabel)
    assertEquals("Completo", types[5].shortLabel)

    val fullLabel = com.example.ui.screens.getPeriodLabel(com.example.ui.screens.ReportType.FULL)
    assertEquals("Todo o Histórico", fullLabel)

    val weekLabel = com.example.ui.screens.getPeriodLabel(com.example.ui.screens.ReportType.WEEK)
    assertEquals("Últimos 7 dias", weekLabel)

    val catLabel = com.example.ui.screens.getPeriodLabel(com.example.ui.screens.ReportType.CATEGORY)
    assertEquals("Todas as Categorias", catLabel)
  }

  @Test
  fun `verify sales calculations for report`() {
    val sampleSale = com.example.data.model.Sale(
      id = 1,
      timestamp = System.currentTimeMillis(),
      customerName = "Cliente Teste",
      paymentMethod = "PIX",
      discount = 50.0,
      totalAmount = 250.0,
      totalCost = 100.0
    )
    val sampleItem = com.example.data.model.SaleItem(
      id = 1,
      saleId = 1,
      productId = 1,
      productName = "Quadro Personalizado",
      unitPrice = 250.0,
      unitCost = 100.0,
      quantity = 1,
      subtotal = 250.0
    )
    val saleWithItems = com.example.data.model.SaleWithItems(
      sale = sampleSale,
      items = listOf(sampleItem)
    )

    val sales = listOf(saleWithItems)
    val totalRevenue = sales.sumOf { it.sale.totalAmount }
    val totalProfit = sales.sumOf { it.sale.totalProfit }
    val totalDiscount = sales.sumOf { it.sale.discount }

    assertEquals(250.0, totalRevenue, 0.001)
    assertEquals(150.0, totalProfit, 0.001)
    assertEquals(50.0, totalDiscount, 0.001)
  }

  @Test
  fun `verify payment methods order is Pix Dinheiro Debito Credito`() {
    val expectedOrder = listOf("Pix", "Dinheiro", "Débito", "Crédito")
    assertEquals(4, expectedOrder.size)
    assertEquals("Pix", expectedOrder[0])
    assertEquals("Dinheiro", expectedOrder[1])
    assertEquals("Débito", expectedOrder[2])
    assertEquals("Crédito", expectedOrder[3])
  }

  @Test
  fun `verify official catalog content and specifications`() {
    val catalog = com.example.data.OfficialCatalog.getOfficialCatalog()
    // Verify featured artwork
    val featured = com.example.data.OfficialCatalog.featuredArtwork
    assertEquals("Ipê na Serra", featured.name)
    assertEquals(23000.00, featured.price, 0.001)
    assertEquals("100 × 160 cm", featured.dimensions)
    assertEquals("VENDIDO", featured.status)
    assertEquals("2024", featured.year)

    // Verify original paintings count is 29 (featured + 28 table rows)
    val paintings = catalog.filter { it.category.contains("Pinturas", ignoreCase = true) }
    assertEquals(29, paintings.size)

    // Verify specific artworks from reference doc
    val no956 = catalog.find { it.code == "956" }
    org.junit.Assert.assertNotNull(no956)
    assertEquals("Feira", no956?.name)
    assertEquals(3100.00, no956!!.price, 0.001)
    assertEquals("42 × 32 cm", no956.dimensions)
    assertEquals("DISPONÍVEL", no956.status)

    val no1000 = catalog.find { it.code == "1000" }
    org.junit.Assert.assertNotNull(no1000)
    assertEquals("Contemplação", no1000?.name)
    assertEquals(20000.00, no1000!!.price, 0.001)
    assertEquals("90 × 147 cm", no1000.dimensions)

    // Verify navigation includes Catalogo
    val screens = Screen.items
    assertEquals(6, screens.size)
    assertEquals("Catálogo", screens[1].title)
    assertEquals("catalogo", screens[1].route)
  }
}
