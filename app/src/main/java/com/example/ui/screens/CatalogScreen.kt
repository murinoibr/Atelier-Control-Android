package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OfficialCatalog
import com.example.data.model.CatalogItem
import com.example.data.model.Product
import com.example.data.model.toCatalogItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel

enum class CatalogFilter(val label: String) {
    ALL("Todas"),
    IN_CADASTROS("Nos Cadastros"),
    NOT_IN_CADASTROS("Não Cadastrados"),
    PAINTINGS("Pinturas Originais"),
    PRINTS_PAPER("Gravuras no Papel"),
    PRINTS_FRAME("Gravuras com Moldura"),
    AVAILABLE("Disponíveis"),
    SOLD("Vendidas"),
    YEAR_2025("Ano 2025"),
    YEAR_2024("Ano 2024")
}

@Composable
fun CatalogScreen(
    viewModel: SalesViewModel,
    onSelectProductForSale: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val catalogItems by viewModel.catalogItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(CatalogFilter.ALL) }
    var selectedItemForDetails by remember { mutableStateOf<CatalogItem?>(null) }
    var itemToEdit by remember { mutableStateOf<CatalogItem?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Filtered catalog items list
    val filteredItems = remember(catalogItems, searchQuery, selectedFilter) {
        catalogItems.filter { item ->
            // Search query matches title, code, dimensions, year, category, notes, or artist
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.code.contains(searchQuery, ignoreCase = true) ||
                item.dimensions.contains(searchQuery, ignoreCase = true) ||
                item.year.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true) ||
                item.status.contains(searchQuery, ignoreCase = true) ||
                item.technique.contains(searchQuery, ignoreCase = true)

            // Category/status filter
            val matchesFilter = when (selectedFilter) {
                CatalogFilter.ALL -> true
                CatalogFilter.IN_CADASTROS -> item.isIncludedInCadastros
                CatalogFilter.NOT_IN_CADASTROS -> !item.isIncludedInCadastros
                CatalogFilter.PAINTINGS -> item.category.contains("Pinturas", ignoreCase = true) ||
                    item.category.contains("Obras Originais", ignoreCase = true) ||
                    item.category.contains("Obras de Arte", ignoreCase = true)
                CatalogFilter.PRINTS_PAPER -> item.category.contains("Papel", ignoreCase = true)
                CatalogFilter.PRINTS_FRAME -> item.category.contains("Moldura", ignoreCase = true)
                CatalogFilter.AVAILABLE -> item.status.equals("DISPONÍVEL", ignoreCase = true)
                CatalogFilter.SOLD -> item.status.equals("VENDIDO", ignoreCase = true)
                CatalogFilter.YEAR_2025 -> item.year.contains("2025")
                CatalogFilter.YEAR_2024 -> item.year.contains("2024")
            }

            matchesQuery && matchesFilter
        }
    }

    // Statistics
    val totalCount = catalogItems.size
    val availableCount = remember(catalogItems) { catalogItems.count { it.status.equals("DISPONÍVEL", ignoreCase = true) } }
    val soldCount = remember(catalogItems) { catalogItems.count { it.status.equals("VENDIDO", ignoreCase = true) } }
    val inCadastrosCount = remember(catalogItems) { catalogItems.count { it.isIncludedInCadastros } }
    val totalEstimatedValue = remember(catalogItems) { catalogItems.sumOf { it.price } }

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
            // Header: Title and Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = AccentMagenta.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "DOC. Nº ${OfficialCatalog.CATALOG_NUMBER}",
                                    color = AccentMagenta,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Acervo Jonas Lemes",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Catálogo de Obras",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 28.sp
                        )
                    }

                    // Reload/Restore Catalog Button
                    IconButton(
                        onClick = { showRestoreConfirmDialog = true },
                        modifier = Modifier.testTag("catalog_reload_button")
                    ) {
                        Surface(
                            color = Color(0xFF27272A),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Restaurar Catálogo Oficial",
                                    tint = AccentMagenta,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Featured Artwork Card: Ipê na Serra (Anotação Superior)
            item {
                val featuredItem = catalogItems.find { it.name.contains("Ipê na Serra", ignoreCase = true) && it.dimensions.contains("100") }
                    ?: OfficialCatalog.featuredArtwork.toCatalogItem()
                FeaturedArtworkBanner(
                    item = featuredItem,
                    onDetailsClick = { selectedItemForDetails = featuredItem },
                    onIncludeClick = {
                        viewModel.includeCatalogItemInCadastros(featuredItem)
                    }
                )
            }

            // Summary Stats Cards (Total, Nos Cadastros, Disponíveis, Vendidas)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total Acervo
                        Surface(
                            color = IosSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, IosCardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Total Acervo", color = TextSecondary, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$totalCount obras",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = formatCurrency(totalEstimatedValue),
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Nos Cadastros (In Store Products)
                        Surface(
                            color = IosSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = CatalogFilter.IN_CADASTROS }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentMagenta,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Nos Cadastros",
                                        color = AccentMagenta,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$inCadastrosCount / $totalCount",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Incluídos na loja",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Disponíveis
                        Surface(
                            color = IosSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = CatalogFilter.AVAILABLE }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = NeonGreen,
                                        shape = CircleShape,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Disponíveis", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$availableCount obras",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Para comercialização",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Vendidas
                        Surface(
                            color = IosSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = CatalogFilter.SOLD }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFFEF4444),
                                        shape = CircleShape,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Vendidas", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$soldCount obras",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Acervo vendido",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar por título, nº (ex: 956), medida, ano...",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = AccentMagenta
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpar busca",
                                    tint = TextSecondary
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosSurface,
                        unfocusedContainerColor = IosSurface,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("catalog_search_field")
                )
            }

            // Filter Chips (Horizontal Scroll)
            item {
                val scrollFilterState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollFilterState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogFilter.values().forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            color = if (isSelected) MagentaContainer else Color(0xFF222226),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AccentMagenta else Color(0xFF2E2E32)
                            ),
                            modifier = Modifier
                                .clickable { selectedFilter = filter }
                                .testTag("catalog_filter_${filter.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                if (filter == CatalogFilter.IN_CADASTROS) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isSelected) MagentaPillText else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = filter.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MagentaPillText else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Results Counter & Active Filter Badge
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${filteredItems.size} obras encontradas",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    if (searchQuery.isNotBlank() || selectedFilter != CatalogFilter.ALL) {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedFilter = CatalogFilter.ALL
                            }
                        ) {
                            Text("Limpar filtros", color = ActionBlue, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Products List
            if (filteredItems.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Collections,
                        title = "Nenhuma obra encontrada",
                        message = "Tente alterar os termos da busca ou os filtros acima."
                    )
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    CatalogArtworkCard(
                        item = item,
                        onClick = { selectedItemForDetails = item },
                        onEditClick = { itemToEdit = item },
                        onSellClick = {
                            viewModel.prepareCatalogItemForSale(item) { prod ->
                                onSelectProductForSale(prod)
                            }
                        },
                        onToggleStatus = { viewModel.toggleCatalogItemStatus(item) },
                        onIncludeInCadastros = {
                            viewModel.includeCatalogItemInCadastros(item)
                        },
                        onRemoveFromCadastros = {
                            viewModel.removeCatalogItemFromCadastros(item)
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    if (selectedItemForDetails != null) {
        val currentDetailItem = catalogItems.find { it.id == selectedItemForDetails!!.id } ?: selectedItemForDetails!!
        ArtworkSpecificationDialog(
            item = currentDetailItem,
            onDismiss = { selectedItemForDetails = null },
            onEditItem = {
                val itm = currentDetailItem
                selectedItemForDetails = null
                itemToEdit = itm
            },
            onToggleStatus = {
                viewModel.toggleCatalogItemStatus(currentDetailItem)
            },
            onIncludeInCadastros = {
                viewModel.includeCatalogItemInCadastros(currentDetailItem)
            },
            onRemoveFromCadastros = {
                viewModel.removeCatalogItemFromCadastros(currentDetailItem)
            },
            onSell = {
                val itm = currentDetailItem
                selectedItemForDetails = null
                viewModel.prepareCatalogItemForSale(itm) { prod ->
                    onSelectProductForSale(prod)
                }
            }
        )
    }

    // Edit Item Dialog (All Fields)
    if (itemToEdit != null) {
        EditCatalogItemDialog(
            item = itemToEdit!!,
            onDismiss = { itemToEdit = null },
            onSave = { updatedItem ->
                viewModel.saveCatalogItem(updatedItem)
                itemToEdit = null
            }
        )
    }

    // Confirmation dialog for reloading official catalog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            containerColor = IosSurface,
            title = {
                Text(
                    text = "Restaurar Catálogo Oficial?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Isso recarregará todas as obras e especificações originais do Catálogo Jonas Lemes Nº 1023. Os produtos que você já incluiu nos cadastros serão preservados.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreOfficialCatalog()
                        showRestoreConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta)
                ) {
                    Text("Restaurar Agora", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

/**
 * Prominent hero banner for the highlighted artwork "Ipê na Serra" (Anotação Superior)
 */
@Composable
fun FeaturedArtworkBanner(
    item: CatalogItem,
    onDetailsClick: () -> Unit,
    onIncludeClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1528),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, AccentMagenta.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() }
            .testTag("featured_artwork_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AccentMagenta,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "★ DESTAQUE DO ACERVO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Anotação Superior",
                        color = Color(0xFFD8B4FE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.isIncludedInCadastros) {
                        Surface(
                            color = NeonGreenContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "✓ NO CADASTRO",
                                color = NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Status badge
                    val isSold = item.status.equals("VENDIDO", ignoreCase = true)
                    Surface(
                        color = if (isSold) Color(0xFF450A0A) else NeonGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSold) Color(0xFFEF4444) else NeonGreen)
                    ) {
                        Text(
                            text = if (isSold) "VENDIDO" else "DISPONÍVEL",
                            color = if (isSold) Color(0xFFFCA5A5) else NeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White
            )

            Text(
                text = "Obra original de Jonas Lemes • Pintura sobre tela",
                fontSize = 13.sp,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFF2E1065),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.dimensions.ifBlank { "100 × 160 cm" },
                            color = Color(0xFFE9D5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Ano ${item.year.ifBlank { "2024" }}",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = formatCurrency(item.price),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGreen
                )
            }

            // If not yet in cadastros, give a direct button on the banner
            if (!item.isIncludedInCadastros) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = AccentMagenta,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clickable { onIncludeClick() }
                        .testTag("featured_include_cadastros_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Incluir nos Cadastros da Loja",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for each artwork displaying complete specifications and Cadastros inclusion option
 */
@Composable
fun CatalogArtworkCard(
    item: CatalogItem,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onSellClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onIncludeInCadastros: () -> Unit,
    onRemoveFromCadastros: () -> Unit
) {
    val isSold = item.status.equals("VENDIDO", ignoreCase = true)
    val hasCode = item.code.isNotBlank()
    val hasDimensions = item.dimensions.isNotBlank()
    val hasYear = item.year.isNotBlank()

    Surface(
        color = IosSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (item.isIncludedInCadastros) AccentMagenta.copy(alpha = 0.5f) else IosCardBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("catalog_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Code/Number & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasCode) {
                        Surface(
                            color = Color(0xFF27272A),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46))
                        ) {
                            Text(
                                text = if (item.code.startsWith("Nº") || item.code == "S/N" || item.code == "Destaque") {
                                    item.code
                                } else {
                                    "Nº ${item.code}"
                                },
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Category text
                    Text(
                        text = item.category,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Cadastros inclusion badge
                    if (item.isIncludedInCadastros) {
                        Surface(
                            color = NeonGreenContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "NO CADASTRO",
                                    color = NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Status Badge (Disponível ou Vendido)
                    Surface(
                        color = if (isSold) Color(0xFF450A0A) else NeonGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSold) Color(0xFFEF4444).copy(alpha = 0.5f) else NeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onToggleStatus() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Surface(
                                color = if (isSold) Color(0xFFEF4444) else NeonGreen,
                                shape = CircleShape,
                                modifier = Modifier.size(6.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isSold) "VENDIDO" else "DISPONÍVEL",
                                color = if (isSold) Color(0xFFFCA5A5) else NeonGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Artwork Title
            Text(
                text = item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )

            // Specifications badges row (Dimensions, Year, Notes)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasDimensions) {
                    Surface(
                        color = Color(0xFF1E1B2E),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.dimensions,
                            color = Color(0xFFC084FC),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (hasYear) {
                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.year,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (item.notes.isNotBlank()) {
                    Surface(
                        color = Color(0xFF1C1917),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF78716C))
                    ) {
                        Text(
                            text = item.notes,
                            color = Color(0xFFD6D3D1),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value & Primary Options Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Valor",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = formatCurrency(item.price),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSold) TextSecondary else NeonGreen
                    )
                }

                // Action Buttons Row: Include in Cadastros, Edit, Info, Sell
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ================= BOTAO INCLUIR NOS CADASTROS =================
                    if (item.isIncludedInCadastros) {
                        Surface(
                            color = Color(0xFF14532D).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable { onRemoveFromCadastros() }
                                .height(34.dp)
                                .testTag("catalog_remove_cadastros_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Já incluído nos cadastros",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cadastrado",
                                    color = NeonGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = AccentMagenta,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clickable { onIncludeInCadastros() }
                                .height(34.dp)
                                .testTag("catalog_include_cadastros_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Incluir nos Cadastros",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Cadastrar",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Editar Obra Button
                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onEditClick() }
                            .height(34.dp)
                            .testTag("catalog_edit_button_${item.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Ver Ficha Técnica Button
                    Surface(
                        color = Color(0xFF27272A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onClick() }
                            .height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Ficha Técnica",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Sell / Add to Cart Button (Only enabled if available)
                    if (!isSold) {
                        Surface(
                            color = Color(0xFF27272A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                            modifier = Modifier
                                .clickable { onSellClick() }
                                .height(34.dp)
                                .testTag("catalog_sell_button_${item.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Vender",
                                    tint = AccentMagenta,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed Specification Dialog for an artwork with clear Cadastros toggle
 */
@Composable
fun ArtworkSpecificationDialog(
    item: CatalogItem,
    onDismiss: () -> Unit,
    onEditItem: () -> Unit,
    onToggleStatus: () -> Unit,
    onIncludeInCadastros: () -> Unit,
    onRemoveFromCadastros: () -> Unit,
    onSell: () -> Unit
) {
    val isSold = item.status.equals("VENDIDO", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosSurface,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ficha Técnica da Obra",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        color = if (isSold) Color(0xFF450A0A) else NeonGreenContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSold) Color(0xFFEF4444) else NeonGreen)
                    ) {
                        Text(
                            text = if (isSold) "VENDIDO" else "DISPONÍVEL",
                            color = if (isSold) Color(0xFFFCA5A5) else NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref. Documento Jonas Lemes Nº 1023",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = IosCardBorder)

                SpecDetailRow("Título da Obra", item.name)
                SpecDetailRow("Artista", item.artist.ifBlank { "Jonas Lemes" })
                if (item.code.isNotBlank()) {
                    SpecDetailRow("Nº / Registro", item.code)
                }
                if (item.dimensions.isNotBlank()) {
                    SpecDetailRow("Dimensões", item.dimensions)
                }
                if (item.year.isNotBlank()) {
                    SpecDetailRow("Ano", item.year)
                }
                if (item.technique.isNotBlank()) {
                    SpecDetailRow("Técnica / Suporte", item.technique)
                }
                SpecDetailRow("Categoria", item.category)
                if (item.notes.isNotBlank()) {
                    SpecDetailRow("Observações", item.notes)
                }
                SpecDetailRow("Valor Cadastrado", formatCurrency(item.price), valueColor = NeonGreen)

                // Cadastros inclusion status
                SpecDetailRow(
                    label = "Status no Sistema",
                    value = if (item.isIncludedInCadastros) "✓ Incluído nos Cadastros Principais" else "Apenas no Catálogo (Não Cadastrado)",
                    valueColor = if (item.isIncludedInCadastros) NeonGreen else TextSecondary
                )

                HorizontalDivider(color = IosCardBorder)

                // ================= OPTION TO INCLUDE / REMOVE FROM CADASTROS =================
                if (!item.isIncludedInCadastros) {
                    Button(
                        onClick = onIncludeInCadastros,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_include_cadastros_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Incluir nos Cadastros Principais",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRemoveFromCadastros,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF87171)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_remove_cadastros_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Remover dos Produtos Cadastrados",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF87171),
                            fontSize = 13.sp
                        )
                    }
                }

                // Edit artwork button inside dialog
                OutlinedButton(
                    onClick = onEditItem,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ActionBlue
                    ),
                    border = BorderStroke(1.dp, ActionBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = ActionBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Editar Informações da Obra",
                        fontWeight = FontWeight.SemiBold,
                        color = ActionBlue,
                        fontSize = 13.sp
                    )
                }

                // Quick toggle status button inside dialog
                OutlinedButton(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSold) NeonGreen else Color(0xFFF87171)
                    ),
                    border = BorderStroke(1.dp, if (isSold) NeonGreen else Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isSold) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSold) "Marcar como DISPONÍVEL" else "Marcar como VENDIDO",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            if (!isSold) {
                Button(
                    onClick = onSell,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lançar Venda", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = TextSecondary)
            }
        }
    )
}

/**
 * Edit dialog for any CatalogItem
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCatalogItemDialog(
    item: CatalogItem,
    onDismiss: () -> Unit,
    onSave: (CatalogItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var priceText by remember { mutableStateOf(item.price.toString()) }
    var code by remember { mutableStateOf(item.code) }
    var category by remember { mutableStateOf(item.category) }
    var dimensions by remember { mutableStateOf(item.dimensions) }
    var year by remember { mutableStateOf(item.year) }
    var technique by remember { mutableStateOf(item.technique) }
    var notes by remember { mutableStateOf(item.notes) }
    var status by remember { mutableStateOf(item.status) }

    val quickCategories = listOf(
        "Pinturas / Obras Originais",
        "Gravuras no Papel Especial",
        "Gravuras com Moldura",
        "Obras de Arte"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosSurface,
        title = {
            Text(
                text = "Editar Obra do Catálogo",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Título da Obra") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Preço (R$)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Nº / Registro") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dimensions,
                        onValueChange = { dimensions = it },
                        label = { Text("Dimensões (cm)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it },
                        label = { Text("Ano") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentMagenta,
                            unfocusedBorderColor = IosCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Category selection chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickCategories.forEach { cat ->
                        Surface(
                            color = if (category == cat) MagentaContainer else Color(0xFF222226),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (category == cat) AccentMagenta else Color(0xFF2E2E32)),
                            modifier = Modifier.clickable { category = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                color = if (category == cat) MagentaPillText else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = technique,
                    onValueChange = { technique = it },
                    label = { Text("Técnica / Suporte") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Status da Obra:", fontSize = 13.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status.equals("DISPONÍVEL", ignoreCase = true),
                            onClick = { status = "DISPONÍVEL" },
                            label = { Text("Disponível") }
                        )
                        FilterChip(
                            selected = status.equals("VENDIDO", ignoreCase = true),
                            onClick = { status = "VENDIDO" },
                            label = { Text("Vendido") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val priceVal = priceText.replace(",", ".").toDoubleOrNull() ?: item.price
                    onSave(
                        item.copy(
                            name = name.trim().ifBlank { item.name },
                            price = priceVal,
                            code = code.trim(),
                            category = category.trim().ifBlank { item.category },
                            dimensions = dimensions.trim(),
                            year = year.trim(),
                            technique = technique.trim(),
                            notes = notes.trim(),
                            status = status
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta)
            ) {
                Text("Salvar Alterações", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
fun SpecDetailRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.weight(0.55f)
        )
    }
}
