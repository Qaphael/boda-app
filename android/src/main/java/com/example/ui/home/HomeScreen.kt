package com.example.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { viewModel.navigateTo(Screen.ProfileSettings) },
                contentAlignment = Alignment.Center
            ) {
                val name = viewModel.userProfile.collectAsState().value?.name ?: "?"
                val initials = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
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
                Text("$greeting, $userName", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                Text("UGX ${bal.toInt().toString().reversed().chunked(3).joinToString(",").reversed()}",
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
        }

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "pickup"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(viewModel.pickupPlace?.name ?: "Set current Gulu pickup...", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "dropoff"), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(viewModel.dropoffPlace?.name ?: BodaLang.get(viewModel.appLanguage, "where_to"), color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
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
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { viewModel.navigateTo(Screen.ProfileSettings) },
                contentAlignment = Alignment.Center
            ) {
                val name = viewModel.userProfile.collectAsState().value?.name ?: "?"
                val initials = name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
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

        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
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
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        windowInsets = WindowInsets.navigationBars
    ) {
        val curr = viewModel.currentScreen
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = curr == Screen.Home || curr == Screen.RoutePreview || curr == Screen.Matching || curr == Screen.RiderEnRoute || curr == Screen.ActiveTrip || curr == Screen.PostTrip,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { viewModel.navigateTo(Screen.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text(BodaLang.get(viewModel.appLanguage, "history")) },
            selected = curr == Screen.TripsHistory,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { viewModel.navigateTo(Screen.TripsHistory) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
            label = { Text("Wallet") },
            selected = curr == Screen.Wallet,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { viewModel.navigateTo(Screen.Wallet) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = curr == Screen.ProfileSettings || curr == Screen.EmergencyContacts || curr == Screen.SavedPlacesManage,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onBackground,
                unselectedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            onClick = { viewModel.navigateTo(Screen.ProfileSettings) }
        )
    }
}
