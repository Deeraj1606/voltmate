package com.company.voltmate.ui.wallet

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.company.voltmate.data.model.TransactionType
import com.company.voltmate.data.model.WalletTransaction
import com.company.voltmate.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddMoneySheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    var receiptTx by remember { mutableStateOf<WalletTransaction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val filteredTxs = remember(transactions, uiState.selectedFilter) {
        when (uiState.selectedFilter) {
            "EARNING" -> transactions.filter { it.type == TransactionType.EARNING.name }
            "CHARGE_PAYMENT" -> transactions.filter { it.type == TransactionType.CHARGE_PAYMENT.name }
            "TOPUP" -> transactions.filter { it.type == TransactionType.TOPUP.name }
            "WITHDRAWAL" -> transactions.filter { it.type == TransactionType.WITHDRAWAL.name }
            else -> transactions
        }
    }

    Scaffold(
        containerColor = SurfaceBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("VoltMate Wallet", fontWeight = FontWeight.Bold, color = InkPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = InkPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhiteCanvas,
                    titleContentColor = InkPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Deep-Green Balance Card (White text explicitly allowed here on deep gradient)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(DeepGreen, PrimaryGreen)))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Total Available Balance",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        "In-App Only",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                "₹%.2f".format(wallet.balanceRupees),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )

                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Use for EV charging across India or withdraw earnings",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showAddMoneySheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = DeepGreen
                                    )
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add Money", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { showWithdrawSheet = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Filled.ArrowOutward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Withdraw", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "EARNING" to "Earnings",
                        "CHARGE_PAYMENT" to "Payments",
                        "TOPUP" to "Top-ups",
                        "WITHDRAWAL" to "Withdrawals"
                    )
                    filters.forEach { (key, label) ->
                        val isSelected = uiState.selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) PrimaryGreen else WhiteCanvas,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, BorderHairline),
                            modifier = Modifier.clickable { viewModel.setFilter(key) }
                        ) {
                            Text(
                                label,
                                color = if (isSelected) Color.White else InkPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction Activity (${filteredTxs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    Text(
                        "Offline Ready",
                        fontSize = 11.sp,
                        color = InkSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Transaction Items
            if (filteredTxs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("No transactions found", fontWeight = FontWeight.SemiBold, color = InkPrimary)
                                Text("Your wallet top-ups, payments, and earnings will appear here", fontSize = 12.sp, color = InkSecondary)
                            }
                        }
                    }
                }
            } else {
                items(filteredTxs) { tx ->
                    TransactionItemCard(
                        tx = tx,
                        onClick = { receiptTx = tx }
                    )
                }
            }
        }
    }

    // Add Money Sheet
    if (showAddMoneySheet) {
        AddMoneyBottomSheet(
            onDismiss = { showAddMoneySheet = false },
            onConfirmAdd = { amount, method ->
                viewModel.topUp(amount, method)
                showAddMoneySheet = false
            }
        )
    }

    // Withdraw Sheet
    if (showWithdrawSheet) {
        WithdrawBottomSheet(
            currentBalance = wallet.balanceRupees,
            savedDest = viewModel.getSavedDestination(),
            onDismiss = { showWithdrawSheet = false },
            onConfirmWithdraw = { amount, dest ->
                viewModel.withdraw(amount, dest)
                showWithdrawSheet = false
            }
        )
    }

    // Receipt Dialog
    receiptTx?.let { tx ->
        TransactionReceiptDialog(
            tx = tx,
            onDismiss = { receiptTx = null }
        )
    }
}

@Composable
fun TransactionItemCard(
    tx: WalletTransaction,
    onClick: () -> Unit
) {
    val isCredit = tx.type == TransactionType.TOPUP.name || tx.type == TransactionType.EARNING.name
    val icon = when (tx.type) {
        TransactionType.TOPUP.name -> Icons.Filled.AddCircle
        TransactionType.EARNING.name -> Icons.Filled.MonetizationOn
        TransactionType.WITHDRAWAL.name -> Icons.Filled.ArrowOutward
        else -> Icons.Filled.ElectricCar
    }
    val iconColor = if (isCredit) PrimaryGreen else InkPrimary
    val iconBg = if (isCredit) TintGreen else Color(0xFFF3F4F6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteCanvas, contentColor = InkPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatTimestamp(tx.createdAt),
                    fontSize = 11.sp,
                    color = InkSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"}₹%.2f".format(tx.amountRupees),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isCredit) PrimaryGreen else InkPrimary
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (tx.status) {
                        "PROCESSING" -> WarningAmber.copy(alpha = 0.15f)
                        "FAILED" -> Color(0xFFFEE2E2)
                        else -> TintGreen
                    }
                ) {
                    Text(
                        text = tx.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (tx.status) {
                            "PROCESSING" -> WarningAmber
                            "FAILED" -> Color(0xFFDC2626)
                            else -> DeepGreen
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneyBottomSheet(
    onDismiss: () -> Unit,
    onConfirmAdd: (Double, String) -> Unit
) {
    var customAmount by remember { mutableStateOf("") }
    var selectedChip by remember { mutableStateOf(500) }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WhiteCanvas,
        contentColor = InkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Add Money to Wallet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = InkPrimary)
            Text("Simulated UPI & Net Banking. Instant wallet credit.", fontSize = 12.sp, color = InkSecondary)

            // Preset Amount Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(100, 200, 500, 1000).forEach { amount ->
                    val isSel = selectedChip == amount && customAmount.isBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) PrimaryGreen else Color(0xFFF3F4F6),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedChip = amount
                                customAmount = ""
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text("₹$amount", fontWeight = FontWeight.Bold, color = if (isSel) Color.White else InkPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Custom Amount Field
            OutlinedTextField(
                value = customAmount,
                onValueChange = {
                    if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                        customAmount = it
                    }
                },
                label = { Text("Or enter custom amount (₹)") },
                placeholder = { Text("e.g. 750") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderHairline
                )
            )

            val finalAmount = customAmount.toDoubleOrNull() ?: selectedChip.toDouble()

            // Payment Source Preview
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9FAFB),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHairline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tata Pay UPI / Google Pay", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkPrimary)
                        Text("Instant confirmation · No transaction fees", fontSize = 11.sp, color = InkSecondary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    isProcessing = true
                    onConfirmAdd(finalAmount, "Tata Pay UPI")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = finalAmount > 0 && !isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Top-up ₹%.0f Now".format(finalAmount), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawBottomSheet(
    currentBalance: Double,
    savedDest: String,
    onDismiss: () -> Unit,
    onConfirmWithdraw: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf(savedDest) }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = WhiteCanvas,
        contentColor = InkPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Withdraw Wallet Funds", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = InkPrimary)

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TintGreen
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available: ₹%.2f".format(currentBalance), fontWeight = FontWeight.Bold, color = DeepGreen, fontSize = 13.sp)
                }
            }

            // Amount Input with "Max" button
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amountText = it },
                label = { Text("Withdrawal Amount (₹, min ₹100)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    TextButton(onClick = { amountText = "%.2f".format(currentBalance) }) {
                        Text("MAX", fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderHairline
                )
            )

            // Destination (Display only, saved locally)
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Payout Destination (UPI ID or Bank A/C)") },
                placeholder = { Text("e.g. host.earning@okaxis") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = InkPrimary,
                    unfocusedTextColor = InkPrimary,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderHairline
                )
            )

            // Clear simulation notice
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Demo: no real money is transferred. Payout will simulate processing and complete in 5s.",
                        fontSize = 11.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }

            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            val isValid = amountVal in 100.0..currentBalance

            Button(
                onClick = {
                    isProcessing = true
                    onConfirmWithdraw(amountVal, destination)
                },
                enabled = isValid && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm Withdrawal of ₹%.2f".format(amountVal), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun TransactionReceiptDialog(
    tx: WalletTransaction,
    onDismiss: () -> Unit
) {
    val isCredit = tx.type == TransactionType.TOPUP.name || tx.type == TransactionType.EARNING.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transaction Receipt", fontWeight = FontWeight.Bold, color = InkPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCredit) TintGreen else Color(0xFFF3F4F6),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "${if (isCredit) "+" else "-"}₹%.2f".format(tx.amountRupees),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCredit) DeepGreen else InkPrimary
                        )
                        Text(tx.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = InkPrimary)
                        Text("Status: ${tx.status}", fontSize = 11.sp, color = InkSecondary)
                    }
                }

                Text("Transaction ID: ${tx.id}", fontSize = 11.sp, color = InkSecondary)
                Text("Timestamp: ${formatTimestamp(tx.createdAt)}", fontSize = 11.sp, color = InkSecondary)
                if (tx.counterparty.isNotBlank()) {
                    Text("Party: ${tx.counterparty}", fontSize = 12.sp, color = InkPrimary)
                }
                if (tx.sessionId.isNotBlank()) {
                    Text("Session ID: ${tx.sessionId}", fontSize = 11.sp, color = InkSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen, contentColor = Color.White)
            ) {
                Text("Done")
            }
        },
        containerColor = WhiteCanvas
    )
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Recent"
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
