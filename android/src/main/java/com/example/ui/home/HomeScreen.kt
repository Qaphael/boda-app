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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedPlace
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.driver.*
import com.example.ui.components.*

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
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                Text("$greeting, $userName", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                Text("UGX ${bal.toInt().toString().reversed().chunked(3).joinToString(",").reversed()}",
                    color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                Spacer(modifier = Modifier.height(Sp.sm))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(4.dp)
                ) {
                    val isRide = viewModel.serviceType == "ride"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRide) MaterialTheme.colorScheme.primary else ComposeColor.Transparent)
                            .clickable { viewModel.serviceType = "ride" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, contentDescription = null, tint = if (isRide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "ride"), color = if (isRide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isRide) MaterialTheme.colorScheme.primary else ComposeColor.Transparent)
                            .clickable { viewModel.serviceType = "delivery" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = if (!isRide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Sp.sm))
                            Text(BodaLang.get(viewModel.appLanguage, "delivery"), color = if (!isRide) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "pickup"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.pickupPlace?.name ?: "Set current Gulu pickup...", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { viewModel.navigateTo(Screen.SearchPlaces) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text(BodaLang.get(viewModel.appLanguage, "dropoff"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(viewModel.dropoffPlace?.name ?: BodaLang.get(viewModel.appLanguage, "where_to"), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))

                if (savedPlaces.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(savedPlaces.take(5)) { place ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (viewModel.dropoffPlace?.id == place.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.dropoffPlace = place
                                        viewModel.dropoffText = place.name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(place.label, fontSize = 11.sp,
                                    color = if (viewModel.dropoffPlace?.id == place.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (viewModel.pickupPlace != null && viewModel.dropoffPlace != null) {
                    Spacer(modifier = Modifier.height(Sp.md))
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
                Text(initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Driver Mode", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (viewModel.isDriverOnline) "Online" else "Offline",
                    color = if (viewModel.isDriverOnline) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                Spacer(modifier = Modifier.height(Sp.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(
                            if (viewModel.isDriverOnline) "Accepting Rides" else "Paused",
                            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp
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

                Spacer(modifier = Modifier.height(Sp.md))

                if (activeTrip != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Active Trip", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("From: ${activeTrip.pickupName}", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                            Text("To: ${activeTrip.dropoffName}", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                            Text("Fare: UGX ${activeTrip.fare.toInt()}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else if (incomingReq != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Ride Request", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(Sp.sm))
                            Text("${incomingReq.pickupName} → ${incomingReq.dropoffName}", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                            Text("Fare: UGX ${incomingReq.fare.toInt()}", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(Sp.sm))
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
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
