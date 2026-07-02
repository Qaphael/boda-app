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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text("\uD83C\uDF89", style = MaterialTheme.typography.displayLarge) },
        title = {
            Text(
                "Welcome to Boda Gulu, ${userName.substringBefore(" ").ifEmpty { "Rider" }}!",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (usedReferralCode) {
                    Text(
                        "Your referral bonus of UGX 3,000 will be added to your wallet automatically after your first completed ride.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Your account is ready. Share your referral code with friends and earn UGX 3,000 for every friend who completes their first ride.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        if (usedReferralCode) "UGX 3,000 bonus incoming"
                        else "Earn UGX 3,000 per referral",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        confirmButton = {
            BodaButton(text = "Book My First Ride", onClick = onDismiss)
        },
        dismissButton = {
            if (!usedReferralCode) {
                TextButton(onClick = onGoToReferrals) {
                    Text("Share my referral code")
                }
            }
        }
    )
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
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Know when your driver arrives") },
            text = {
                Text(
                    "Get notified the moment your boda is 2 minutes away — so you're ready at the pickup point.",
                    style = MaterialTheme.typography.bodyMedium
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
                    Text("Maybe later")
                }
            }
        )
    }
}

@Composable
fun SystemOverlayDialog(title: String, desc: String, cta: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center) },
        text = { Text(desc, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center) },
        confirmButton = {
            BodaButton(text = cta, onClick = onDismiss)
        }
    )
}

@Composable
fun MoMoPinDialog(viewModel: BodaViewModel) {
    val isMtn = viewModel.momoPromptProvider.contains("MTN", ignoreCase = true)
    val brandColor = if (isMtn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    ModalBottomSheet(
        onDismissRequest = { viewModel.showMoMoPinDialog = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enter ${viewModel.momoPromptProvider} PIN",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Authorize UGX ${viewModel.momoPromptAmount.toInt()} to Boda Gulu Wallet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Target: ${viewModel.momoPromptPhone}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (viewModel.momoPinInput.isEmpty()) "Enter PIN" else "\u2022 ".repeat(viewModel.momoPinInput.length),
                    color = if (viewModel.momoPinInput.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else brandColor,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (viewModel.momoPinError) {
                Text(
                    text = "Invalid PIN format. Must be 4-5 digits.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
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
                            Surface(
                                onClick = {
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
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = key,
                                        color = if (key in listOf("Clear", "Delete")) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
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
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
