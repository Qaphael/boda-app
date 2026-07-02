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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            val initials = (user?.name ?: "Boda Gulu Customer").split(" ")
                .filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(initials, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(user?.name ?: "Boda Gulu Customer", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
                Text(user?.phoneNumber ?: "No verified phone", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$tripCount trips · ${"%.1f".format(avgRating)} rating",
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
                                .clip(MaterialTheme.shapes.small)
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
                                .clip(MaterialTheme.shapes.small)
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

        // Dual Mode Switch: Driver Mode Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
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
        Text("Preferences", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        // Option: Emergency Contacts
        ListItem(
            headlineContent = { Text("Emergency Contacts") },
            supportingContent = { Text("${contacts.size} of 3 emergency linkages setup") },
            leadingContent = { Icon(Icons.Default.Emergency, null, tint = MaterialTheme.colorScheme.error) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.EmergencyContacts) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Option: Saved Places
        ListItem(
            headlineContent = { Text("Manage Saved Places") },
            leadingContent = { Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.SavedPlacesManage) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Option: Help & Support
        ListItem(
            headlineContent = { Text("Help & Support") },
            leadingContent = { Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
            modifier = Modifier.clickable { viewModel.navigateTo(Screen.Support) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Option: System Overlay Triggers
        ListItem(
            headlineContent = { Text("Simulate App Maintenance Error") },
            leadingContent = { Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { viewModel.showDowntimeNotice = true }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            headlineContent = { Text("Simulate Account Suspension Error") },
            leadingContent = { Icon(Icons.Default.Gavel, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { viewModel.showSuspendedNotice = true }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        ListItem(
            headlineContent = { Text("Simulate Offline State Error Banner") },
            leadingContent = { Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { viewModel.isOffline = true }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(Modifier.height(16.dp))
        BodaSecondaryButton(
            text = "Sign out",
            onClick = { viewModel.signOut() },
            modifier = Modifier.fillMaxWidth()
        )

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Account?") },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone and you will lose all Gulu wallet balances, trip history, and settings.") },
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
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = { showDeleteConfirmDialog = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete Account") }
        Spacer(Modifier.height(16.dp))
    }
}
