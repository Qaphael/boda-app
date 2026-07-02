package com.example.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SavedPlace
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.driver.*
import com.example.ui.components.*
import com.example.ui.util.BodaLang

@Composable
fun HomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    NotificationPermissionNudge()

    if (viewModel.showWelcomeBonus) {
        val user by viewModel.userProfile.collectAsState()
        WelcomeBonusDialog(
            userName = user?.name ?: "Rider",
            usedReferralCode = viewModel.referralCodeInput.isNotEmpty(),
            onDismiss = { viewModel.dismissWelcomeBonus() },
            onGoToReferrals = {
                viewModel.dismissWelcomeBonus()
                viewModel.navigateTo(Screen.Referrals)
            }
        )
    }

    if (viewModel.isDriverMode) {
        DriverHomeScreen(viewModel)
    } else {
        PassengerHomeScreen(viewModel, savedPlaces)
    }
}

@Composable
fun PassengerHomeScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    Box(modifier = Modifier.fillMaxSize()) {
        GuluMapView(
            modifier = Modifier.fillMaxSize(),
            pickup = viewModel.pickupPlace,
            dropoff = viewModel.dropoffPlace,
            viewModel = viewModel,
            userLocation = viewModel.currentLocation
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val name = viewModel.userProfile.collectAsState().value?.name ?: "?"
            val initials = name.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
            FilledIconButton(onClick = { viewModel.navigateTo(Screen.ProfileSettings) }) {
                Text(initials, style = MaterialTheme.typography.labelLarge)
            }

            Column(horizontalAlignment = Alignment.End) {
                val userName = viewModel.userProfile.collectAsState().value?.name?.split(" ")?.firstOrNull() ?: ""
                val bal = viewModel.effectiveBalance.collectAsState().value
                val hour = java.time.LocalTime.now().hour
                val greeting = when {
                    hour < 12 -> "Good morning"
                    hour < 17 -> "Good afternoon"
                    else -> "Good evening"
                }
                Text("$greeting, $userName", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text("UGX ${bal.toInt().toString().reversed().chunked(3).joinToString(",").reversed()}",
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
            }
        }

        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(8.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = { viewModel.serviceType = "ride" },
                        selected = viewModel.serviceType == "ride",
                        icon = { Icon(Icons.Default.TwoWheeler, null) },
                        label = { Text(BodaLang.get(viewModel.appLanguage, "ride")) }
                    )
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = { viewModel.serviceType = "delivery" },
                        selected = viewModel.serviceType == "delivery",
                        icon = { Icon(Icons.Default.LocalShipping, null) },
                        label = { Text(BodaLang.get(viewModel.appLanguage, "delivery")) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    onClick = { viewModel.navigateTo(Screen.SearchPlaces) },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(viewModel.pickupPlace?.name ?: "Set current Gulu pickup...", maxLines = 1) },
                        overlineContent = { Text(BodaLang.get(viewModel.appLanguage, "pickup")) },
                        leadingContent = { Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.tertiary) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    onClick = { viewModel.navigateTo(Screen.SearchPlaces) },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(viewModel.dropoffPlace?.name ?: BodaLang.get(viewModel.appLanguage, "where_to"), maxLines = 1) },
                        overlineContent = { Text(BodaLang.get(viewModel.appLanguage, "dropoff")) },
                        leadingContent = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (savedPlaces.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedPlaces.take(5)) { place ->
                            FilterChip(
                                selected = viewModel.dropoffPlace?.id == place.id,
                                onClick = {
                                    viewModel.dropoffPlace = place
                                    viewModel.dropoffText = place.name
                                },
                                label = { Text(place.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (viewModel.pickupPlace != null && viewModel.dropoffPlace != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    BodaButton(
                        text = "Calculate Boda Fare Estimate",
                        onClick = { viewModel.navigateTo(Screen.RoutePreview) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "preview_route_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun DriverHomeScreen(viewModel: BodaViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        val activeTrip = viewModel.driverActiveTrip
        val incomingReq = viewModel.driverIncomingRequest

        val pName = when {
            activeTrip != null -> activeTrip.pickupName
            incomingReq != null -> incomingReq.pickupName
            else -> null
        }
        val dName = when {
            activeTrip != null -> activeTrip.dropoffName
            incomingReq != null -> incomingReq.dropoffName
            else -> null
        }

        GuluMapView(
            modifier = Modifier.fillMaxSize(),
            isDriverMode = true,
            driverTripState = viewModel.driverTripState,
            driverPickupName = pName,
            driverDropoffName = dName,
            driverProgress = viewModel.driverSimulationProgress,
            viewModel = viewModel,
            userLocation = viewModel.currentLocation
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val name = viewModel.userProfile.collectAsState().value?.name ?: "?"
            val initials = name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
            FilledIconButton(onClick = { viewModel.navigateTo(Screen.ProfileSettings) }) {
                Text(initials, style = MaterialTheme.typography.labelLarge)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Driver Mode", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                Text(
                    if (viewModel.isDriverOnline) "Online" else "Offline",
                    color = if (viewModel.isDriverOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        ElevatedCard(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            if (viewModel.isDriverOnline) "Accepting Rides" else "Paused",
                            color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Switch(
                        checked = viewModel.isDriverOnline,
                        onCheckedChange = { viewModel.toggleDriverOnline() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTrip != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Active Trip", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("From: ${activeTrip.pickupName}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            Text("To: ${activeTrip.dropoffName}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            Text("Fare: UGX ${activeTrip.fare.toInt()}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                } else if (incomingReq != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Ride Request", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${incomingReq.pickupName} → ${incomingReq.dropoffName}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            Text("Fare: UGX ${incomingReq.fare.toInt()}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                BodaButton(
                                    text = "Accept",
                                    onClick = { viewModel.driverAcceptTrip() },
                                    modifier = Modifier.weight(1f)
                                )
                                BodaSecondaryButton(
                                    text = "Reject",
                                    onClick = { viewModel.driverRejectTrip() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        if (viewModel.isDriverOnline) "Waiting for ride requests..." else "Go online to start receiving rides",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun BodaBottomNavigation(viewModel: BodaViewModel) {
    NavigationBar {
        val curr = viewModel.currentScreen
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text(BodaLang.get(viewModel.appLanguage, "home_title")) },
            selected = curr == Screen.Home,
            onClick = { viewModel.navigateTo(Screen.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text(BodaLang.get(viewModel.appLanguage, "history")) },
            selected = curr == Screen.TripsHistory,
            onClick = { viewModel.navigateTo(Screen.TripsHistory) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text(BodaLang.get(viewModel.appLanguage, "wallet")) },
            selected = curr == Screen.Wallet,
            onClick = { viewModel.navigateTo(Screen.Wallet) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text(BodaLang.get(viewModel.appLanguage, "profile")) },
            selected = curr == Screen.ProfileSettings || curr == Screen.EmergencyContacts || curr == Screen.SavedPlacesManage,
            onClick = { viewModel.navigateTo(Screen.ProfileSettings) }
        )
    }
}
