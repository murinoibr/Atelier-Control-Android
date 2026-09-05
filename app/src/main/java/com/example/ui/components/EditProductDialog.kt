package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.theme.*

@Composable
fun EditProductDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var priceText by remember { mutableStateOf(product.price.toString()) }
    var category by remember { mutableStateOf(product.category) }
    var code by remember { mutableStateOf(product.code) }
    var dimensions by remember { mutableStateOf(product.dimensions) }
    var year by remember { mutableStateOf(product.year) }
    var technique by remember { mutableStateOf(product.technique) }
    var artist by remember { mutableStateOf(product.artist.ifBlank { "Jonas Lemes" }) }
    var status by remember { mutableStateOf(product.status.ifBlank { "DISPONÍVEL" }) }
    var notes by remember { mutableStateOf(product.notes) }

    val quickCategories = listOf(
        "Pinturas Originais",
        "Gravuras no Papel",
        "Gravuras com Moldura",
        "Ímãs",
        "Obras de Arte",
        "Acessórios",
        "Geral"
    )

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = IosSurfaceElevated,
            border = BorderStroke(1.dp, IosCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MagentaContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = AccentMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Editar Produto / Obra",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "ID #${product.id}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = IosCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // Status Selector (Disponível / Vendido)
                Text(
                    text = "Status da Obra",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDisponivel = status.equals("DISPONÍVEL", ignoreCase = true)
                    Surface(
                        color = if (isDisponivel) NeonGreenContainer else Color(0xFF1E1E22),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isDisponivel) NeonGreen else Color(0xFF2E2E33)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = "DISPONÍVEL" }
                    ) {
                        Text(
                            text = "✓ DISPONÍVEL",
                            color = if (isDisponivel) NeonGreen else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }

                    val isVendido = status.equals("VENDIDO", ignoreCase = true)
                    Surface(
                        color = if (isVendido) Color(0xFF3F1316) else Color(0xFF1E1E22),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isVendido) Color(0xFFEF4444) else Color(0xFF2E2E33)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { status = "VENDIDO" }
                    ) {
                        Text(
                            text = "✕ VENDIDO",
                            color = if (isVendido) Color(0xFFFCA5A5) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nome da Obra / Produto
                Text(
                    text = "Nome / Título da Obra *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Preço (R$) & Código / Registro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Preço (R$) *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IosInputBackground,
                                unfocusedContainerColor = IosInputBackground,
                                focusedBorderColor = AccentMagenta,
                                unfocusedBorderColor = IosCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nº Registro / Código",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            placeholder = { Text("Ex: 1000", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IosInputBackground,
                                unfocusedContainerColor = IosInputBackground,
                                focusedBorderColor = AccentMagenta,
                                unfocusedBorderColor = IosCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dimensões & Ano
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Dimensões",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dimensions,
                            onValueChange = { dimensions = it },
                            placeholder = { Text("Ex: 42 × 32 cm", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IosInputBackground,
                                unfocusedContainerColor = IosInputBackground,
                                focusedBorderColor = AccentMagenta,
                                unfocusedBorderColor = IosCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ano",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it },
                            placeholder = { Text("Ex: 2025", color = TextMuted, fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = IosInputBackground,
                                unfocusedContainerColor = IosInputBackground,
                                focusedBorderColor = AccentMagenta,
                                unfocusedBorderColor = IosCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Categoria & Quick Chips
                Text(
                    text = "Categoria",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(10.dp))

                // Técnica & Artista
                Text(
                    text = "Técnica / Suporte",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = technique,
                    onValueChange = { technique = it },
                    placeholder = { Text("Ex: Óleo sobre tela, Pintura Original...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Observações
                Text(
                    text = "Observações / Detalhes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Ex: Destaque do acervo, moldura em madeira nobre...", color = TextMuted, fontSize = 12.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IosInputBackground,
                        unfocusedContainerColor = IosInputBackground,
                        focusedBorderColor = AccentMagenta,
                        unfocusedBorderColor = IosCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, IosCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancelar", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: product.price
                            val updated = product.copy(
                                name = name.trim().ifBlank { product.name },
                                price = parsedPrice,
                                category = category.trim().ifBlank { "Geral" },
                                code = code.trim(),
                                dimensions = dimensions.trim(),
                                year = year.trim(),
                                technique = technique.trim(),
                                artist = artist.trim().ifBlank { "Jonas Lemes" },
                                status = status.trim(),
                                notes = notes.trim()
                            )
                            onSave(updated)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_product_changes_button")
                    ) {
                        Text("Salvar Alterações", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
