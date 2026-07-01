package com.example.ui.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WalletTransaction
import com.example.ui.BodaViewModel
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField
import androidx.compose.material3.MaterialTheme

// --- SCREEN 11: WALLET balance & DEPOSITS ---
@Composable
fun WalletScreen(viewModel: BodaViewModel, balance: Double, txns: List<WalletTransaction>) {
    if (viewModel.isLoadingData) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Boda Gulu Wallet", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Balance Card
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Available Escrow Balance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("UGX ${balance.toInt()}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Secure payments around Gulu without physical cash.", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Spending Stats Row
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val ridesThisMonth = txns.count { it.type == "payment" &&
            System.currentTimeMillis() - it.timestamp < thirtyDaysMs }
        val spentThisMonth = txns.filter { it.type == "payment" &&
            System.currentTimeMillis() - it.timestamp < thirtyDaysMs }
            .sumOf { it.amount }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val stats = listOf(
                "$ridesThisMonth" to "Trips this month",
                "UGX ${spentThisMonth.toInt()}" to "Spent this month",
                (txns.firstOrNull()?.provider ?: "MTN") to "Last used"
            )
            stats.forEach { pair ->
                BodaCard(modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(pair.first, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(pair.second, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Top-up selector
        Text("MTN / Airtel Quick Deposit", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            listOf("2000", "5000", "10000", "20000").forEach { valAmount ->
                val isSelected = viewModel.walletTopupAmountInput == valAmount
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.walletTopupAmountInput = valAmount }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("+$valAmount", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        BodaTextField(
            value = viewModel.walletTopupPhoneInput,
            onValueChange = { viewModel.walletTopupPhoneInput = it },
            label = "Mobile Money Number",
            placeholder = "e.g. 0772 123456",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            testTag = "wallet_phone_input"
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (viewModel.walletTopupStatus == "pending") {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Awaiting Mobile Money OTP and Pin Approval...", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        } else {
            BodaButton(
                text = "Top Up Wallet Balance (UGX ${viewModel.walletTopupAmountInput})",
                onClick = { viewModel.startWalletTopup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wallet_topup_btn")
            )
        }

        if (viewModel.walletTopupStatus == "success") {
            Text("Top up successful! Ref: ${viewModel.activeTransactionReference}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Historic Ledger list
        Text("Transaction History Ledger", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (txns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "No Transactions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions yet", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Your Mobile Money deposits and ride payments will show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            txns.forEach { txn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val txIcon = when {
                            txn.type == "payment" -> Icons.Default.TwoWheeler
                            txn.reference.startsWith("REF-") -> Icons.Default.CardGiftcard
                            else -> Icons.Default.ArrowUpward
                        }
                        val txColor = if (txn.type == "payment") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        Icon(
                            imageVector = txIcon,
                            contentDescription = null,
                            tint = txColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(if (txn.type == "topup") "MoMo Topup Deposit" else "Boda Booking Payment", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Ref: ${txn.reference} • ${txn.provider}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "${if (txn.type == "topup") "+" else "-"} UGX ${txn.amount.toInt()}",
                        color = if (txn.type == "topup") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}
