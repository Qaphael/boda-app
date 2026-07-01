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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField
import com.example.ui.components.Color
import com.example.ui.components.Sp

@Composable
fun DriverOnboardingScreen(viewModel: BodaViewModel) {
    val step = viewModel.driverOnboardingStep
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Driver Onboarding Portal", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.md))

        // Progress bar indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Step $step of 5", color = ComposeColor(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(
                text = when(step) {
                    1 -> "Personal & NIN Details"
                    2 -> "Motorcycle & Stage"
                    3 -> "Document Security Scan"
                    4 -> "Gulu Safety Code Quiz"
                    else -> "Approved!"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(Sp.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 1..5) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= step) ComposeColor(0xFF10B981) else Color(0xFF334155)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.lg))

        when (step) {
            1 -> {
                // Step 1: Personal Details
                Text(
                    "Register as Gulu Boda Member",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your legal bio-data and National ID details. All drivers must be vetted for safety.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegName,
                    onValueChange = { viewModel.driverRegName = it },
                    label = "Driver Legal Name",
                    testTag = "driver_reg_name"
                )

                Spacer(modifier = Modifier.height(Sp.sm))

                BodaTextField(
                    value = viewModel.driverRegPhone,
                    onValueChange = { viewModel.driverRegPhone = it },
                    label = "Payout Phone Number (+256...)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    testTag = "driver_reg_phone"
                )

                Spacer(modifier = Modifier.height(Sp.sm))

                BodaTextField(
                    value = viewModel.driverRegNID,
                    onValueChange = { viewModel.driverRegNID = it.uppercase().take(14) },
                    label = "National ID / NIN (e.g. CM8400...)",
                    placeholder = "14 character NIN",
                    testTag = "driver_reg_nid"
                )

                Spacer(modifier = Modifier.height(Sp.xl))

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
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Enter your license plate and local Gulu security stage association.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                BodaTextField(
                    value = viewModel.driverRegPlate,
                    onValueChange = { viewModel.driverRegPlate = it.uppercase() },
                    label = "License Plate Number",
                    placeholder = "e.g. UFL 123X",
                    testTag = "driver_reg_plate"
                )

                Spacer(modifier = Modifier.height(Sp.md))

                Text("Select Assigned Security Stage in Gulu:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Sp.sm))

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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.driverRegStage == s) Color(0xFF1E293B) else Color.Transparent)
                            .border(1.dp, if (viewModel.driverRegStage == s) ComposeColor(0xFFFDB913) else Color(0xFF334155), RoundedCornerShape(8.dp))
                            .clickable { viewModel.driverRegStage = s }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = viewModel.driverRegStage == s,
                            onClick = { viewModel.driverRegStage = s },
                            colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Text(s, color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(Sp.md))
                Text("Select Helmet Color Scheme:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(Sp.sm))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Yellow", "Orange", "Black").forEach { color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (viewModel.driverRegHelmetColor == color) Color(0xFF1E293B) else Color.Transparent)
                                .border(1.dp, if (viewModel.driverRegHelmetColor == color) ComposeColor(0xFFFDB913) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                .clickable { viewModel.driverRegHelmetColor = color }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(color, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xl))

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
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Upload high-quality scans of required legal documents to verify identification.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
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
                            containerColor = if (isUploaded) Color(0xFF1E293B) else Color(0xFF131A2A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .border(1.dp, if (isUploaded) ComposeColor(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (isUploaded) "Document Security Approved" else "Requires High-Res JPEG Scan",
                                    color = if (isUploaded) ComposeColor(0xFF10B981) else Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                            
                            BodaButton(
                                text = if (isUploaded) "UPLOADED" else "UPLOAD",
                                onClick = { viewModel.simulateDocUpload(doc) },
                                containerColor = if (isUploaded) Color(0xFF10B981) else Color(0xFFFDB913),
                                contentColor = ComposeColor.Black,
                                modifier = Modifier.height(36.dp).widthIn(max = 120.dp)
                            )
                        }
                    }
                }

                if (viewModel.driverUploadProgress > 0f && viewModel.driverUploadProgress < 1f) {
                    Spacer(modifier = Modifier.height(Sp.md))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Uploading ${viewModel.driverDocumentType}...",
                            color = ComposeColor(0xFFFDB913),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(Sp.sm))
                        LinearProgressIndicator(
                            progress = viewModel.driverUploadProgress,
                            color = ComposeColor(0xFF10B981),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xl))

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
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Text(
                    "Verify Gulu street safety compliance rules. Select correct answers below.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp).align(Alignment.Start)
                )

                // Question 1
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("1. What is the maximum speed limit for Bodas inside Gulu Town center?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
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
                                    colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                                )
                                Text(choice, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Question 2
                BodaCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("2. What are you mandatory required to provide to all passengers?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(Sp.sm))
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
                                    colors = RadioButtonDefaults.colors(selectedColor = ComposeColor(0xFFFDB913), unselectedColor = Color.White)
                                )
                                Text(choice, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.sm))

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
                        colors = CheckboxDefaults.colors(checkedColor = ComposeColor(0xFFFDB913), uncheckedColor = Color.White)
                    )
                    Spacer(modifier = Modifier.width(Sp.sm))
                    Text(
                        "I pledge to drive safely and respect all traffic codes and passengers in Gulu.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(Sp.xl))

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
                        .background(ComposeColor(0xFF10B981).copy(alpha = 0.2f))
                        .border(2.dp, ComposeColor(0xFF10B981), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Verified", tint = ComposeColor(0xFF10B981), modifier = Modifier.size(50.dp))
                }

                Spacer(modifier = Modifier.height(Sp.md))
                Text(
                    "Congratulations & Welcome!",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                Text(
                    "Your Boda-Gulu Driver Account has been dynamically approved & synchronized! You are officially registered in Gulu.",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(Sp.lg))

                BodaCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Driver Name:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Plate Number:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegPlate, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Assigned Stage:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegStage, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(Sp.sm))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Helmet Preference:", color = Color(0xFF64748B), fontSize = 12.sp)
                            Text(viewModel.driverRegHelmetColor, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Sp.xxl))

                BodaButton(
                    text = "LAUNCH DRIVER COCKPIT",
                    onClick = { viewModel.completeDriverOnboarding() },
                    containerColor = Color(0xFF10B981),
                    contentColor = ComposeColor.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
