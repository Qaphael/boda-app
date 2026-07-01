package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.offline.triggerOfflineSMSBookingFlow
import com.example.ui.home.navigateBack
import com.example.ui.components.Color
import com.example.ui.components.Sp
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
import androidx.compose.ui.unit.sp

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
                .background(Color(0xFF0F172A))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Confirm Ride Booking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))

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
                viewModel = viewModel
            )
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // Pickup / Dropoff nodes summary
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(viewModel.pickupPlace?.name ?: "", color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF97316)))
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(viewModel.dropoffPlace?.name ?: "", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // PARCEL FORM IF SERVICE TYPE IS DELIVERY
        if (viewModel.serviceType == "delivery") {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Delivery Package Details", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.parcelDetails,
                        onValueChange = { viewModel.parcelDetails = it },
                        label = "Item details",
                        placeholder = "What is in the package? (e.g. food, documents)",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row {
                        BodaTextField(
                            value = viewModel.recipientName,
                            onValueChange = { viewModel.recipientName = it },
                            label = "Name",
                            placeholder = "Recipient Name",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
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
            Spacer(modifier = Modifier.height(Sp.sm))
        }

        // Schedule ride panel
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.scheduledBookingDateTime = "Today, 18:30"
                    }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Column {
                        Text("Schedule for Later", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(viewModel.scheduledBookingDateTime ?: "Leave now (Immediate booking)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // PAYMENT METHD SELECTOR: MTN / Airtel / Wallet
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Select MTN / Airtel / Wallet Payment", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                val walletSufficient = walletBalance >= viewModel.calculatedFare - viewModel.activePromoDiscount.value
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    listOf("MTN" to "MTN MoMo", "Airtel" to "Airtel", "Wallet" to "Boda Wallet").forEach { (method, label) ->
                        val isSelected = viewModel.selectedPaymentMethod == method
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF334155))
                                .clickable { viewModel.selectedPaymentMethod = method }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                if (method == "Wallet") {
                                    Text(
                                        text = "UGX ${walletBalance.toInt()}",
                                        color = if (walletSufficient) Color(0xFF10B981) else Color(0xFFF87171),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

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
            Spacer(modifier = Modifier.width(Sp.sm))
            BodaButton(
                text = "Apply",
                onClick = { viewModel.validatePromoViaBackend(viewModel.promoCodeInput) },
                modifier = Modifier.weight(0.5f)
            )
        }
        if (viewModel.activePromoMessage.isNotEmpty()) {
            Text(viewModel.activePromoMessage, color = Color(0xFF10B981), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // DETAILED ETA & METRICS DASHBOARD
        val arrivalTimeStr = remember(viewModel.calculatedTimeMinutes) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.MINUTE, viewModel.calculatedTimeMinutes)
            java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
        }

        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ESTIMATED ROUTE TIME", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${viewModel.calculatedTimeMinutes} mins",
                            color = Color(0xFFFDB913),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("EST. ARRIVAL TIME", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = arrivalTimeStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Distance", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = "${"%.2f".format(viewModel.calculatedDistanceKm)} km",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(
                            text = "${viewModel.calculatedTimeMinutes} mins",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        // FARE ESTIMATE — Hero element with breakdown
        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TOTAL FARE", color = Color(0xFF64748B), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                val discounted = (viewModel.calculatedFare - viewModel.activePromoDiscount.value).coerceAtLeast(1000.0)
                Text("UGX ${discounted.toInt()}",
                    color = Color(0xFFFDB913), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${viewModel.calculatedDistanceKm.let { "%.1f".format(it) }} km",
                        color = Color(0xFF64748B), fontSize = 11.sp)
                    Text("·", color = Color(0xFF334155), fontSize = 11.sp)
                    Text("${viewModel.calculatedTimeMinutes} min",
                        color = Color(0xFF64748B), fontSize = 11.sp)
                }
                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 10.dp))
                val baseFare = if (viewModel.serviceType == "ride") 1500.0 else 2500.0
                val distCharge = viewModel.calculatedDistanceKm * 1000.0
                listOf("Base" to "UGX ${baseFare.toInt()}",
                    "Distance" to "UGX ${distCharge.toInt()}").forEach { (lbl, val_) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(lbl, color = Color(0xFF64748B), fontSize = 11.sp)
                        Text(val_, color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(3.dp))
                }
                if (viewModel.activePromoDiscount.value > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Promo ${viewModel.activePromoCode}", color = Color(0xFF64748B), fontSize = 11.sp)
                        Text("- UGX ${viewModel.activePromoDiscount.value.toInt()}",
                            color = Color(0xFF10B981), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

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
