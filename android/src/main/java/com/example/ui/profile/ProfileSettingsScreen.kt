package com.example.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.EmergencyContact
import com.example.data.UserProfile
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.driver.*
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaErrorButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaSecondaryButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.home.navigateTo
import com.example.ui.home.updateLanguage
import com.example.ui.home.saveUserProfile
import com.example.ui.home.updateAppThemeSetting
import com.example.ui.home.toggleNetworkConnection
import com.example.ui.home.connectPostgresWebSocket
import com.example.ui.auth.signOut
import com.example.ui.auth.deleteAccount
import kotlinx.coroutines.launch

// --- SCREEN 12: PROFILE SETTINGS ENGINE ---
@Composable
fun ProfileSettingsScreen(viewModel: BodaViewModel, user: UserProfile?, contacts: List<EmergencyContact>) {
    val coroutineScope = rememberCoroutineScope()
    val referralEarnings by viewModel.referralEarnings.collectAsState()
    val myReferralCode = user?.referralCode?.ifEmpty { "GULU-BODA-256" } ?: "GULU-BODA-256"
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    val trips by viewModel.trips.collectAsState()
    val tripCount = trips.size
    val avgRating = if (trips.any { it.rating > 0 })
        trips.filter { it.rating > 0 }.map { it.rating }.average() else 0.0

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
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(user?.name ?: "Boda Gulu Customer", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Text(user?.phoneNumber ?: "No verified phone", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$tripCount trips · ⭐ ${"%.1f".format(avgRating)} rating",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Community Referral Earnings Code Card
        BodaCard(
            onClick = { viewModel.navigateTo(Screen.Referrals) },
            testTag = "referrals_card",
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Referral Earnings", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UGX ${referralEarnings.toInt()}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Share code $myReferralCode to earn 3,000 UGX for each friend registered in Gulu. Click to view dashboard & refer friends!", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gulu Local Language Selector Card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("App Language Localization", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(8.dp))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Local Gulu Dialect:", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val languages = listOf(
                        Triple("en", "English", "🇬🇧"),
                        Triple("ach", "Acholi/Luo", "🇺🇬"),
                        Triple("luo", "Lango/Luo", "🇺🇬")
                    )
                    languages.forEach { (code, label, flag) ->
                        val isSelected = viewModel.appLanguage == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                .clickable {
                                    coroutineScope.launch {
                                        viewModel.updateLanguage(code)
                                        val updatedProfile = user?.copy(language = code) ?: UserProfile(id = 1, isSetupComplete = true, language = code)
                                        viewModel.saveUserProfile(updatedProfile)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(flag, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Theme Selector Card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("App Theme & Styling Mode", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(modifier = Modifier.height(8.dp))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Theme mode for Boda Gulu:", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Triple("system", "System", Icons.Default.Settings),
                        Triple("dark", "Dark Mode", Icons.Default.NightsStay),
                        Triple("light", "Light Mode", Icons.Default.WbSunny)
                    )
                    themes.forEach { (mode, label, icon) ->
                        val isSelected = viewModel.appThemeSetting == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                .clickable {
                                    viewModel.updateAppThemeSetting(mode)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PostgreSQL Real-time WebSocket Sync & Room Cache Control Center
        Text("PostgreSQL Real-time Sync & Cache Monitor", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Connection state row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (viewModel.isOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isOnline) "Postgres WebSocket Connected" else "Postgres Offline (Room Active)",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Switch to toggle online/offline
                    Switch(
                        checked = viewModel.isOnline,
                        onCheckedChange = { viewModel.toggleNetworkConnection() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cache stats row
                val cachedTrips = viewModel.trips.collectAsState().value
                val unsyncedCount = viewModel.unsyncedTripsCount.collectAsState().value
                val cachedPlacesCount = viewModel.savedPlaces.collectAsState().value.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Trips", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("${cachedTrips.size}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Places", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("$cachedPlacesCount", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unsynced to PG", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text("$unsyncedCount", color = if (unsyncedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real-time scrolling CDC WebSocket log terminal!
                Text("PostgreSQL Live CDC Replication Log:", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(viewModel.postgresWebSocketLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("SUCCESS") || log.contains("\u2713")) MaterialTheme.colorScheme.tertiary else if (log.contains("Warning") || log.contains("offline")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forced manual replication trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BodaSecondaryButton(
                        text = "Force PG Sync",
                        onClick = {
                            viewModel.connectPostgresWebSocket()
                        },
                        enabled = viewModel.isOnline,
                        modifier = Modifier.weight(1f)
                    )

                    BodaSecondaryButton(
                        text = "Reset Sync",
                        onClick = {
                            coroutineScope.launch {
                                viewModel.connectPostgresWebSocket()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Mode Switch: Driver Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (viewModel.isDriverRegistered) {
                            viewModel.isDriverMode = !viewModel.isDriverMode
                            viewModel.navigateTo(Screen.Home)
                        } else {
                            viewModel.startDriverOnboarding()
                        }
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Gulu Boda Driver Mode", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (!viewModel.isDriverRegistered) "Tap to Register as Vetted Rider"
                            else if (viewModel.isDriverMode) "Active (Gulu Cockpit)"
                            else "Inactive (Passenger)",
                            color = if (viewModel.isDriverRegistered) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    checked = viewModel.isDriverMode,
                    onCheckedChange = {
                        if (viewModel.isDriverRegistered) {
                            viewModel.isDriverMode = it
                            viewModel.navigateTo(Screen.Home)
                        } else {
                            viewModel.startDriverOnboarding()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Buttons list
        Text("Preferences & Customization", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(8.dp))

        // Option: Emergency Contacts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.EmergencyContacts) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Emergency, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Emergency Contacts", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                    Text("${contacts.size} of 3 emergency linkages setup", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        // Option: Saved Places
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.SavedPlacesManage) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Saved Places", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        // Option: Help & Support
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(Screen.Support) }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Help & Support", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        // Option: System Overlay Triggers (For testing and complete 75 screen path coverage)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showDowntimeNotice = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate App Maintenance Error", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showSuspendedNotice = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Account Suspension Error", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.isOffline = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate Offline State Error Banner", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)

        Spacer(Modifier.height(16.dp))
        BodaSecondaryButton(
            text = "Sign out",
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Account?", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone and you will lose all Gulu wallet balances, trip history, and settings.", color = MaterialTheme.colorScheme.outline) },
                containerColor = MaterialTheme.colorScheme.surface,
                confirmButton = {
                    BodaErrorButton(
                        text = "Delete",
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteAccount()
                        }
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "Delete account",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.clickable { showDeleteConfirmDialog = true }
        )
        Spacer(Modifier.height(16.dp))
    }
}
