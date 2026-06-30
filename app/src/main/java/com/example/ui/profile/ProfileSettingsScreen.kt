package com.example.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyContact
import com.example.data.UserProfile
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.driver.*
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaSecondaryButton
import com.example.ui.components.Color
import com.example.ui.components.Sp
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
        Box(Modifier.fillMaxSize().background(Color(0xFF0F172A)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFFDB913))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFDB913)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Column {
                Text(user?.name ?: "Boda Gulu Customer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(user?.phoneNumber ?: "No verified phone", color = Color(0xFF64748B), fontSize = 14.sp)
                Text(
                    "$tripCount trips · ⭐ ${"%.1f".format(avgRating)} rating",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Community Referral Earnings Code Card
        BodaCard(
            onClick = { viewModel.navigateTo(Screen.Referrals) },
            testTag = "referrals_card",
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Referral Earnings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UGX ${referralEarnings.toInt()}", color = Color(0xFF10B981), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(Sp.xs))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(Sp.xs))
                Text("Share code $myReferralCode to earn 3,000 UGX for each friend registered in Gulu. Click to view dashboard & refer friends!", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Gulu Local Language Selector Card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Sp.xs))
            Text("App Language Localization", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Local Gulu Dialect:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
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
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A))
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
                                Text(flag, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(Sp.xs))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // App Theme Selector Card
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Sp.xs))
            Text("App Theme & Styling Mode", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select Theme mode for Boda Gulu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
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
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A))
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
                                    tint = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(Sp.sm))
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // PostgreSQL Real-time WebSocket Sync & Room Cache Control Center
        Text("PostgreSQL Real-time Sync & Cache Monitor", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))
        BodaCard(
            border = BorderStroke(1.dp, if (viewModel.isOnline) Color(0xFF10B981) else Color(0xFF64748B)),
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
                                .background(if (viewModel.isOnline) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Text(
                            text = if (viewModel.isOnline) "Postgres WebSocket Connected" else "Postgres Offline (Room Active)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Switch to toggle online/offline
                    Switch(
                        checked = viewModel.isOnline,
                        onCheckedChange = { viewModel.toggleNetworkConnection() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Sp.sm))

                // Cache stats row
                val cachedTrips = viewModel.trips.collectAsState().value
                val unsyncedCount = viewModel.unsyncedTripsCount.collectAsState().value
                val cachedPlacesCount = viewModel.savedPlaces.collectAsState().value.size

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Trips", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${cachedTrips.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cached Places", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$cachedPlacesCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unsynced to PG", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("$unsyncedCount", color = if (unsyncedCount > 0) Color(0xFFFDB913) else Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

                // Real-time scrolling CDC WebSocket log terminal!
                Text("PostgreSQL Live CDC Replication Log:", color = Color(0xFFFDB913), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(Sp.xs))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(viewModel.postgresWebSocketLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("SUCCESS") || log.contains("âœ"")) Color(0xFF10B981) else if (log.contains("Warning") || log.contains("offline")) Color(0xFFFDB913) else Color.White,
                                fontSize = 11.sp,
                                style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

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

        Spacer(modifier = Modifier.height(Sp.md))

        // Dual Mode Switch: Driver Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFDB913), RoundedCornerShape(12.dp))
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
                    Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = Color(0xFFFDB913), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text("Gulu Boda Driver Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (!viewModel.isDriverRegistered) "Tap to Register as Vetted Rider"
                            else if (viewModel.isDriverMode) "Active (Gulu Cockpit)"
                            else "Inactive (Passenger)",
                            color = if (viewModel.isDriverRegistered) Color(0xFF10B981) else Color(0xFFFDB913),
                            fontSize = 12.sp
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
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color(0xFFFDB913),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF1E293B)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Settings Buttons list
        Text("Preferences & Customization", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(Sp.sm))

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
                Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFE4002B))
                Spacer(modifier = Modifier.width(Sp.sm))
                Column {
                    Text("Emergency Contacts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${contacts.size} of 3 emergency linkages setup", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

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
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFFFDB913))
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Manage Saved Places", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

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
                Icon(Icons.Default.Help, contentDescription = null, tint = Color(0xFFFDB913))
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Help & Support", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
        }
        HorizontalDivider(color = Color(0xFF1E293B))

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
                Icon(Icons.Default.Build, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate App Maintenance Error", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.showSuspendedNotice = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Gavel, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate Account Suspension Error", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.isOffline = true }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(Sp.sm))
                Text("Simulate Offline State Error Banner", color = Color.White, fontSize = 14.sp)
            }
        }
        HorizontalDivider(color = Color(0xFF1E293B))

        Spacer(Modifier.height(Sp.md))
        BodaSecondaryButton(
            text = "Sign out",
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Account?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone and you will lose all Gulu wallet balances, trip history, and settings.", color = Color(0xFF94A3B8)) },
                containerColor = Color(0xFF1E293B),
                confirmButton = {
                    BodaButton(
                        text = "Delete",
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteAccount()
                        },
                        containerColor = Color(0xFFE4002B),
                        contentColor = Color.White
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }

        Spacer(Modifier.height(Sp.xl))
        Text(
            "Delete account",
            color = Color(0xFF475569),
            fontSize = 11.sp,
            modifier = Modifier.clickable { showDeleteConfirmDialog = true }
        )
        Spacer(Modifier.height(Sp.md))
    }
}
