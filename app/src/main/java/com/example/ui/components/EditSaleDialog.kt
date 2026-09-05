package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
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
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.example.data.model.SaleWithItems
import com.example.ui.theme.*

@Composable
fun EditSaleDialog(
    saleWithItems: SaleWithItems,
    onDismiss: () -> Unit,
    onSave: (Sale, List<SaleItem>) -> Unit
) {
    val initialSale = saleWithItems.sale
    var customerName by remember { mutableStateOf(initialSale.customerName) }
    var paymentMethod by remember { mutableStateOf(initialSale.paymentMethod) }
    var discountText by remember { mutableStateOf(initialSale.discount.toString()) }
    var notes by remember { mutableStateOf(initialSale.notes) }
    var isCancelled by remember { mutableStateOf(initialSale.isCancelled) }

    // Editable Items list
    var items by remember { mutableStateOf(saleWithItems.items) }

    val paymentOptions = listOf(
        "PIX",
        "Cartão de Crédito",
        "Cartão de Débito",
        "Dinheiro",
        "Transferência",
        "Outro"
    )

    // Calculate subtotal from items
    val itemsSubtotal = items.sumOf { it.quantity * it.unitPrice }
    val parsedDiscount = discountText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val calculatedTotal = (itemsSubtotal - parsedDiscount).coerceAtLeast(0.0)

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
                // Header
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
                            color = NeonGreenContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = NeonGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Editar Registro de Venda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Venda #${initialSale.id} • ${formatDateTime(initialSale.timestamp)}",
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

                // Status: Ativa vs Cancelada
                Text(
                    text = "Status da Venda",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = if (!isCancelled) NeonGreenContainer else Color(0xFF1E1E22),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (!isCancelled) NeonGreen else Color(0xFF2E2E33)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCancelled = false }
                    ) {
                        Text(
                            text = "✓ VENDA CONCLUÍDA",
                            color = if (!isCancelled) NeonGreen else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }

                    Surface(
                        color = if (isCancelled) Color(0xFF3F1316) else Color(0xFF1E1E22),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isCancelled) Color(0xFFEF4444) else Color(0xFF2E2E33)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isCancelled = true }
                    ) {
                        Text(
                            text = "✕ CANCELADA",
                            color = if (isCancelled) Color(0xFFFCA5A5) else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cliente
                Text(
                    text = "Nome do Cliente",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    placeholder = { Text("Ex: Cliente Balcão", color = TextMuted, fontSize = 13.sp) },
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

                Spacer(modifier = Modifier.height(12.dp))

                // Forma de Pagamento
                Text(
                    text = "Forma de Pagamento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentOptions.forEach { method ->
                        val isSelected = paymentMethod.equals(method, ignoreCase = true)
                        Surface(
                            color = if (isSelected) MagentaContainer else Color(0xFF1E1E22),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) AccentMagenta else Color(0xFF2E2E33)),
                            modifier = Modifier.clickable { paymentMethod = method }
                        ) {
                            Text(
                                text = method,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MagentaPillText else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Itens da Venda
                Text(
                    text = "Itens da Venda (${items.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEachIndexed { index, item ->
                        Surface(
                            color = Color(0xFF1C1C1F),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${formatCurrency(item.unitPrice)} un.",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                // Quantity Controls
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF27272A),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                if (item.quantity > 1) {
                                                    val updatedList = items.toMutableList()
                                                    val newQty = item.quantity - 1
                                                    updatedList[index] = item.copy(
                                                        quantity = newQty,
                                                        subtotal = newQty * item.unitPrice
                                                    )
                                                    items = updatedList
                                                } else if (items.size > 1) {
                                                    val updatedList = items.toMutableList()
                                                    updatedList.removeAt(index)
                                                    items = updatedList
                                                }
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (item.quantity > 1) Icons.Default.Remove else Icons.Default.Delete,
                                                contentDescription = "Diminuir",
                                                tint = if (item.quantity > 1) TextSecondary else ActionRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${item.quantity}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    Surface(
                                        color = Color(0xFF27272A),
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable {
                                                val updatedList = items.toMutableList()
                                                val newQty = item.quantity + 1
                                                updatedList[index] = item.copy(
                                                    quantity = newQty,
                                                    subtotal = newQty * item.unitPrice
                                                )
                                                items = updatedList
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Aumentar",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = formatCurrency(item.quantity * item.unitPrice),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonGreen
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Desconto & Observações
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Desconto (R$)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it },
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
                            text = "Anotação de Desconto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = { Text("Ex: 10% Desc.", color = TextMuted, fontSize = 12.sp) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Resumo do Total
                Surface(
                    color = Color(0xFF141416),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal itens:", fontSize = 12.sp, color = TextSecondary)
                            Text(text = formatCurrency(itemsSubtotal), fontSize = 12.sp, color = TextSecondary)
                        }
                        if (parsedDiscount > 0) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Desconto:", fontSize = 12.sp, color = ActionRed)
                                Text(text = "-${formatCurrency(parsedDiscount)}", fontSize = 12.sp, color = ActionRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color(0xFF27272A))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Valor Total Final:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text(
                                text = formatCurrency(calculatedTotal),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NeonGreen
                            )
                        }
                    }
                }

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
                            val updatedSale = initialSale.copy(
                                customerName = customerName.trim().ifBlank { "Cliente Balcão" },
                                paymentMethod = paymentMethod.trim(),
                                discount = parsedDiscount,
                                totalAmount = calculatedTotal,
                                isCancelled = isCancelled,
                                notes = notes.trim()
                            )
                            onSave(updatedSale, items)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMagenta),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_sale_changes_button")
                    ) {
                        Text("Salvar Alterações", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
