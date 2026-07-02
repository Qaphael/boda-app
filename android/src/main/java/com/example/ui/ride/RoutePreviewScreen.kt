package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.offline.triggerOfflineSMSBookingFlow
import com.example.ui.home.navigateBack

import com.example.ui.components.BodaButton
import com.example.ui.components.BodaTextField
import com.example.ui.components.BodaCard
import com.example.ui.components.GuluMapView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// --- SCREEN 5: ROUTE PREVIEW & PRICE ESTIMATES ---
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RoutePreviewScreen(viewModel: BodaViewModel, walletBalance: Double) {
    val locationPermState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermState.status) {
        if (locationPermState.status.isGranted) {
            viewModel.locationPermissionGranted = true
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermState.status.isGranted) {
            locationPermState.launchPermissionRequest()
        }
    }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm Ride Booking", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Vector Map showing route preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            GuluMapView(
                modifier = Modifier.fillMaxSize(),
                pickup = viewModel.pickupPlace,
                dropoff = viewModel.dropoffPlace,
                viewModel = viewModel,
                userLocation = viewModel.currentLocation
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pickup / Dropoff nodes summary
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.pickupPlace?.name ?: "", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(viewModel.dropoffPlace?.name ?: "", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PARCEL FORM IF SERVICE TYPE IS DELIVERY
        if (viewModel.serviceType == "delivery") {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Delivery Package Details", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    BodaTextField(
                        value = viewModel.parcelDetails,
                        onValueChange = { viewModel.parcelDetails = it },
                        label = "Item details",
                        placeholder = "What is in the package? (e.g. food, documents)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        BodaTextField(
                            value = viewModel.recipientName,
                            onValueChange = { viewModel.recipientName = it },
                            label = "Name",
                            placeholder = "Recipient Name",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BodaTextField(
                            value = viewModel.recipientPhone,
                            onValueChange = { viewModel.recipientPhone = it },
                            label = "Phone",
                            placeholder = "Recipient Phone",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Schedule ride panel
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.scheduledBookingDateTime = "Today, 18:30"
                    }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Schedule for Later", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                        Text(viewModel.scheduledBookingDateTime ?: "Leave now (Immediate booking)", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PAYMENT METHD SELECTOR: MTN / Airtel / Wallet
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select MTN / Airtel / Wallet Payment", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                val walletSufficient = walletBalance >= viewModel.calculatedFare - viewModel.activePromoDiscount.value
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    listOf("MTN" to "MTN MoMo", "Airtel" to "Airtel", "Wallet" to "Boda Wallet").forEach { (method, label) ->
                        val isSelected = viewModel.selectedPaymentMethod == method
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.selectedPaymentMethod = method }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                if (method == "Wallet") {
                                    Text(
                                        text = "UGX ${walletBalance.toInt()}",
                                        color = if (walletSufficient) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Promo Code Row
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                BodaTextField(
                    value = viewModel.promoCodeInput,
                    onValueChange = { viewModel.promoCodeInput = it },
                    label = "Promo Code",
                    placeholder = "e.g. GULU3000",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            BodaButton(
                text = "Apply",
                onClick = { viewModel.validatePromoViaBackend(viewModel.promoCodeInput) },
                modifier = Modifier.weight(0.5f)
            )
        }
        if (viewModel.activePromoMessage.isNotEmpty()) {
            Text(viewModel.activePromoMessage, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DETAILED ETA & METRICS DASHBOARD
        val arrivalTimeStr = remember(viewModel.calculatedTimeMinutes) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MINUTE, viewModel.calculatedTimeMinutes)
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
        }

        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED ROUTE TIME", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "${viewModel.calculatedTimeMinutes} mins",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EST. ARRIVAL TIME", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = arrivalTimeStr,
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Distance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${"%.2f".format(viewModel.calculatedDistanceKm)} km",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${viewModel.calculatedTimeMinutes} mins",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // FARE ESTIMATE — Hero element with breakdown
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL FARE", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(4.dp))
                val discounted = (viewModel.calculatedFare - viewModel.activePromoDiscount.value).coerceAtLeast(1000.0)
                Text("UGX ${discounted.toInt()}",
                    color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${viewModel.calculatedDistanceKm.let { "%.1f".format(it) }} km",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text("·", color = MaterialTheme.colorScheme.surfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text("${viewModel.calculatedTimeMinutes} min",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
                val baseFare = if (viewModel.serviceType == "ride") 1500.0 else 2500.0
                val distCharge = viewModel.calculatedDistanceKm * 1000.0
                listOf("Base" to "UGX ${baseFare.toInt()}",
                    "Distance" to "UGX ${distCharge.toInt()}").forEach { (lbl, val_) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lbl, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text(val_, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (viewModel.activePromoDiscount.value > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Promo ${viewModel.activePromoCode}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        Text("- UGX ${viewModel.activePromoDiscount.value.toInt()}",
                            color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CONFIRM CTA
        val walletSufficient = walletBalance >= viewModel.calculatedFare - viewModel.activePromoDiscount.value
        val walletBlocked = viewModel.selectedPaymentMethod == "Wallet" && !walletSufficient
        BodaButton(
            text = if (walletBlocked) "Insufficient balance — top up first"
                else if (viewModel.isOnline) "Confirm — UGX ${(viewModel.calculatedFare - viewModel.activePromoDiscount.value).toInt()}"
                else "Book via SMS Fallback",
            enabled = !walletBlocked,
            onClick = {
                if (viewModel.isOnline) {
                    if (locationPermState.status.isGranted) {
                        viewModel.locationPermissionGranted = true
                        viewModel.confirmBooking()
                    } else {
                        locationPermState.launchPermissionRequest()
                    }
                } else {
                    viewModel.triggerOfflineSMSBookingFlow()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "confirm_ride_btn"
        )
    }
}
