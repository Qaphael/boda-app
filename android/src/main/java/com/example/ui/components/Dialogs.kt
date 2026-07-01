package com.example.ui.components

import android.Manifest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.BodaViewModel
import com.example.ui.wallet.confirmWalletTopupWithPin
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@Composable
fun WelcomeBonusDialog(
    userName: String,
    usedReferralCode: Boolean,
    onDismiss: () -> Unit,
    onGoToReferrals: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDF89", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Welcome to Boda Gulu, ${userName.substringBefore(" ").ifEmpty { "Rider" }}!",
                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (usedReferralCode) {
                    Text(
                        "Your referral bonus of UGX 3,000 will be added to your wallet automatically after your first completed ride.",
                        color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                } else {
                    Text(
                        "Your account is ready. Share your referral code with friends and earn UGX 3,000 for every friend who completes their first ride.",
                        color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center, lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (usedReferralCode) "UGX 3,000 bonus incoming \uD83C\uDFCD\uFE0F"
                        else "Earn UGX 3,000 per referral \uD83C\uDFCD\uFE0F",
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                BodaButton(text = "Book My First Ride", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(10.dp))
                if (!usedReferralCode) {
                    TextButton(onClick = onGoToReferrals) {
                        Text("Share my referral code", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionNudge() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("boda_prefs", android.content.Context.MODE_PRIVATE) }
    val alreadyAsked = remember { prefs.getBoolean("notif_permission_asked", false) }
    if (alreadyAsked) return

    val notifState: com.google.accompanist.permissions.PermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else return

    var showNudge by remember { mutableStateOf(!notifState.status.isGranted) }

    if (showNudge) {
        AlertDialog(
            onDismissRequest = {
                showNudge = false
                prefs.edit().putBoolean("notif_permission_asked", true).apply()
            },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Know when your driver arrives", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Get notified the moment your boda is 2 minutes away — so you're ready at the pickup point.",
                    color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                BodaButton(
                    text = "Allow Notifications",
                    onClick = {
                        notifState.launchPermissionRequest()
                        showNudge = false
                        prefs.edit().putBoolean("notif_permission_asked", true).apply()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = {
                    showNudge = false
                    prefs.edit().putBoolean("notif_permission_asked", true).apply()
                }) {
                    Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun SystemOverlayDialog(title: String, desc: String, cta: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(desc, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                BodaButton(
                    text = cta,
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MoMoPinDialog(viewModel: BodaViewModel) {
    val isMtn = viewModel.momoPromptProvider.contains("MTN", ignoreCase = true)
    val brandColor = if (isMtn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val brandTextColor = if (isMtn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
            .padding(24.dp)
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandColor)
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = viewModel.momoPromptProvider.uppercase(),
                            color = brandTextColor,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = brandTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "GULU SECURE ESCROW TRANSACTION",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authorize payment of UGX ${viewModel.momoPromptAmount.toInt()} to Boda Gulu Wallet?",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Target Number: ${viewModel.momoPromptPhone}",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (viewModel.momoPinInput.isEmpty()) "Enter PIN" else "• ".repeat(viewModel.momoPinInput.length),
                            color = if (viewModel.momoPinInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else brandColor,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }

                    if (viewModel.momoPinError) {
                        Text(
                            text = "Invalid PIN format. Must be 4-5 digits.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("Clear", "0", "Delete")
                        )
                        keys.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                when (key) {
                                                    "Clear" -> viewModel.momoPinInput = ""
                                                    "Delete" -> {
                                                        if (viewModel.momoPinInput.isNotEmpty()) {
                                                            viewModel.momoPinInput = viewModel.momoPinInput.dropLast(1)
                                                        }
                                                    }
                                                    else -> {
                                                        if (viewModel.momoPinInput.length < 5) {
                                                            viewModel.momoPinInput += key
                                                            viewModel.momoPinError = false
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            color = if (key in listOf("Clear", "Delete")) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BodaSecondaryButton(
                            text = "Cancel",
                            onClick = { viewModel.showMoMoPinDialog = false },
                            modifier = Modifier.weight(1f)
                        )

                        BodaButton(
                            text = "Approve",
                            onClick = { viewModel.confirmWalletTopupWithPin() },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }
        }
    }
}
