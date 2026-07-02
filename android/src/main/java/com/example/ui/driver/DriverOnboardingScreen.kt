package com.example.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField

@Composable
fun DriverOnboardingScreen(viewModel: BodaViewModel) {
    val step = viewModel.driverOnboardingStep
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                if (step > 1) viewModel.driverOnboardingStep-- 
                else viewModel.navigateTo(Screen.WelcomeOnboarding) 
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Driver Onboarding Portal", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress bar indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step $step of 5", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(
                text = when(step) {
                    1 -> "Personal & NIN Details"
                    2 -> "Motorcycle & Stage"
                    3 -> "Document Security Scan"
                    4 -> "Gulu Safety Code Quiz"
                    else -> "Approved!"
                },
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= step) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            1 -> {
                // Step 1: Personal Details
                Text(
                    "Register as Gulu Boda Member",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your legal bio-data and National ID details. All drivers must be vetted for safety.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegName,
                    onValueChange = { viewModel.driverRegName = it },
                    label = "Driver Legal Name",
                    testTag = "driver_reg_name"
                )

                Spacer(modifier = Modifier.height(8.dp))

                BodaTextField(
                    value = viewModel.driverRegPhone,
                    onValueChange = { viewModel.driverRegPhone = it },
                    label = "Payout Phone Number (+256...)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "driver_reg_phone"
                )

                Spacer(modifier = Modifier.height(8.dp))

                BodaTextField(
                    value = viewModel.driverRegNID,
                    onValueChange = { viewModel.driverRegNID = it.uppercase().take(14) },
                    label = "National ID / NIN (e.g. CM8400...)",
                    placeholder = "14 character NIN",
                    testTag = "driver_reg_nid"
                )

                Spacer(modifier = Modifier.height(32.dp))

                BodaButton(
                    text = "Continue to Motorcycle Info",
                    onClick = { viewModel.driverOnboardingStep = 2 },
                    enabled = viewModel.driverRegName.isNotEmpty() && viewModel.driverRegPhone.isNotEmpty() && viewModel.driverRegNID.length >= 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            2 -> {
                // Step 2: Motorcycle Details
                Text(
                    "Motorcycle & Stage Setup",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your license plate and local Gulu security stage association.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegPlate,
                    onValueChange = { viewModel.driverRegPlate = it.uppercase() },
                    label = "License Plate Number",
                    placeholder = "e.g. UFL 123X",
                    testTag = "driver_reg_plate"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Assigned Security Stage in Gulu:", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))

                val stages = listOf(
                    "Gulu Main Market Stage",
                    "Gulu University Gate Stage",
                    "Lacor Hospital Road Stage",
                    "Cereleno Roundabout Stage",
                    "Pece Stadium West Stage"
                )
                stages.forEach { s ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (viewModel.driverRegStage == s) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .border(1.dp, if (viewModel.driverRegStage == s) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                            .clickable { viewModel.driverRegStage = s }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.driverRegStage == s,
                            onClick = { viewModel.driverRegStage = s },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.onBackground)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(s, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Helmet Color Scheme:", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Yellow", "Orange", "Black").forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(if (viewModel.driverRegHelmetColor == color) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .border(1.dp, if (viewModel.driverRegHelmetColor == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                .clickable { viewModel.driverRegHelmetColor = color }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(color, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                BodaButton(
                    text = "Continue to Documents",
                    onClick = { viewModel.driverOnboardingStep = 3 },
                    enabled = viewModel.driverRegPlate.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            3 -> {
                // Step 3: Document Security Upload
                Text(
                    "Security Vetting & Documents",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Upload high-quality scans of required legal documents to verify identification.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                val docs = listOf(
                    "National ID Card (NIN)",
                    "Driving Permit (Class A)",
                    "Stage Recommendation Letter"
                )

                docs.forEach { doc ->
                    val isUploaded = viewModel.driverDocsUploaded.contains(doc)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUploaded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, if (isUploaded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    if (isUploaded) "Document Security Approved" else "Requires High-Res JPEG Scan",
                                    color = if (isUploaded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            
                            BodaButton(
                                text = if (isUploaded) "UPLOADED" else "UPLOAD",
                                onClick = { viewModel.simulateDocUpload(doc) },
                                modifier = Modifier.height(36.dp).widthIn(max = 120.dp)
                            )
                        }
                    }
                }

                if (viewModel.driverUploadProgress > 0f && viewModel.driverUploadProgress < 1f) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Uploading ${viewModel.driverDocumentType}...",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = viewModel.driverUploadProgress,
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                BodaButton(
                    text = "Continue to Safety Quiz",
                    onClick = { viewModel.driverOnboardingStep = 4 },
                    enabled = viewModel.driverDocsUploaded.containsAll(docs),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            4 -> {
                // Step 4: Safety & Community Quiz
                Text(
                    "Gulu Safety & Code of Conduct",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Verify Gulu street safety compliance rules. Select correct answers below.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                // Question 1
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("1. What is the maximum speed limit for Bodas inside Gulu Town center?", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("30 km/h (Town Core Limit)", "60 km/h (High Speed)", "No speed limit").forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.driverQuizAnswer1 = choice }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.driverQuizAnswer1 == choice,
                                    onClick = { viewModel.driverQuizAnswer1 = choice },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.onBackground)
                                )
                                Text(choice, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Question 2
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("2. What are you mandatory required to provide to all passengers?", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf("A clean spare Helmet and Reflector Jacket", "Nothing, passenger holds tightly", "A bottle of water").forEach { choice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.driverQuizAnswer2 = choice }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = viewModel.driverQuizAnswer2 == choice,
                                    onClick = { viewModel.driverQuizAnswer2 = choice },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary, unselectedColor = MaterialTheme.colorScheme.onBackground)
                                )
                                Text(choice, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.driverTermsAccepted = !viewModel.driverTermsAccepted }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.driverTermsAccepted,
                        onCheckedChange = { viewModel.driverTermsAccepted = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, uncheckedColor = MaterialTheme.colorScheme.onBackground)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "I pledge to drive safely and respect all traffic codes and passengers in Gulu.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                BodaButton(
                    text = "Submit Safety Test",
                    onClick = { viewModel.driverOnboardingStep = 5 },
                    enabled = viewModel.driverQuizAnswer1.isNotEmpty() && viewModel.driverQuizAnswer2.isNotEmpty() && viewModel.driverTermsAccepted,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            5 -> {
                // Step 5: Verified & Welcome
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Verified", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(50.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Congratulations & Welcome!",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your Boda-Gulu Driver Account has been dynamically approved & synchronized! You are officially registered in Gulu.",
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                BodaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Driver Name:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.driverRegName, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Plate Number:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.driverRegPlate, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Assigned Stage:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.driverRegStage, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Helmet Preference:", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(viewModel.driverRegHelmetColor, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                BodaButton(
                    text = "LAUNCH DRIVER COCKPIT",
                    onClick = { viewModel.completeDriverOnboarding() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
