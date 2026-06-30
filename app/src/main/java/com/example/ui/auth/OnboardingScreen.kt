package com.example.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BodaViewModel
import com.example.ui.driver.startDriverOnboarding
import com.example.ui.components.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.tasks.Tasks

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OnboardingScreen(viewModel: BodaViewModel) {
    val activity = LocalContext.current as? android.app.Activity
    if (!viewModel.onboardingCarouselCompleted) {
        val slideTitle = when (viewModel.onboardingSlideIndex) {
            0 -> BodaLang.get(viewModel.appLanguage, "welcome_title_1")
            1 -> BodaLang.get(viewModel.appLanguage, "welcome_title_2")
            else -> BodaLang.get(viewModel.appLanguage, "welcome_title_3")
        }
        val slideDesc = when (viewModel.onboardingSlideIndex) {
            0 -> BodaLang.get(viewModel.appLanguage, "welcome_desc_1")
            1 -> BodaLang.get(viewModel.appLanguage, "welcome_desc_2")
            else -> BodaLang.get(viewModel.appLanguage, "welcome_desc_3")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skip",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { viewModel.completeOnboardingCarousel() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("skip_onboarding_btn")
                )
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            BodaCard(
                modifier = Modifier.size(240.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val imageRes = when (viewModel.onboardingSlideIndex) {
                        0 -> R.drawable.img_onboarding_ride
                        1 -> R.drawable.img_onboarding_payment
                        else -> R.drawable.img_onboarding_delivery
                    }
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = slideTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(Sp.xl))

            Text(
                text = slideTitle,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Sp.sm))
            Text(
                text = slideDesc,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(Sp.xl))
            Row(horizontalArrangement = Arrangement.Center) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(if (viewModel.onboardingSlideIndex == i) 14.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (viewModel.onboardingSlideIndex == i) Color(0xFFFDB913) else Color(0xFF475569))
                    )
                }
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewModel.onboardingSlideIndex > 0) {
                    BodaSecondaryButton(
                        text = BodaLang.get(viewModel.appLanguage, "btn_back"),
                        onClick = { viewModel.onboardingSlideIndex-- },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                }

                BodaButton(
                    text = if (viewModel.onboardingSlideIndex < 2) BodaLang.get(viewModel.appLanguage, "btn_continue") else "Get Started",
                    onClick = {
                        if (viewModel.onboardingSlideIndex < 2) {
                            viewModel.onboardingSlideIndex++
                        } else {
                            viewModel.completeOnboardingCarousel()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = if (viewModel.onboardingSlideIndex > 0) 8.dp else 0.dp),
                    testTag = "next_onboarding_btn"
                )
            }
        }
    } else if (!viewModel.onboardingLanguageSelected) {
        var selectedLang by remember { mutableStateOf(viewModel.appLanguage) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Sp.lg))

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = Color(0xFFFDB913),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            Text(
                text = "Select Preferred Language",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Sp.sm))

            Text(
                text = "Choose your preferred dialect for using Boda Gulu. You can also adjust this later in settings.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.weight(0.1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val languages = listOf(
                    Triple("en", "English", "\uD83C\uDDEC\uD83C\uDDE7 Standard international English interface"),
                    Triple("ach", "Acholi / Luo", "\uD83C\uDDFA\uD83C\uDDEC Local Acholi Gulu dialect localization"),
                    Triple("luo", "Lango / Luo", "\uD83C\uDDFA\uD83C\uDDEC Local Lango northern dialect localization")
                )

                languages.forEach { (code, label, desc) ->
                    val isSelected = selectedLang == code
                    BodaCard(
                        onClick = { selectedLang = code },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFDB913) else Color(0xFF334155)
                        ),
                        testTag = "lang_card_$code"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color(0xFFFDB913) else Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when(code) {
                                        "en" -> "EN"
                                        "ach" -> "ACH"
                                        else -> "LUO"
                                    },
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(Sp.md))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(Sp.xs))
                                Text(
                                    text = desc,
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = Color(0xFFFDB913),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Sp.lg))

            BodaButton(
                text = "Confirm & Register",
                onClick = { viewModel.completeOnboardingLanguage(selectedLang) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "confirm_lang_btn"
            )
        }
    } else {
        android.util.Log.d("BODA_GOOGLE", "OnboardingScreen rendering Step 3 - otpSent=${viewModel.otpSent}, isOtpVerified=${viewModel.isOtpVerified}")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(Sp.md))
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.boda_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(Sp.lg))

            OnboardingProgressBar(
                currentStep = viewModel.onboardingStep,
                stepLabel = viewModel.onboardingStepLabel
            )

            Spacer(modifier = Modifier.height(Sp.lg))

            if (!viewModel.otpSent) {
                val activity = LocalContext.current as androidx.activity.ComponentActivity
                val phoneHintLauncher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        val phoneNumber = com.google.android.gms.auth.api.identity.Identity
                            .getSignInClient(activity)
                            .getPhoneNumberFromIntent(result.data)
                        val digits = phoneNumber
                            ?.replace(Regex("[^0-9]"), "")
                            ?.removePrefix("256")
                            ?.take(9)
                            ?: ""
                        if (digits.isNotEmpty()) {
                            viewModel.phoneInput = digits
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    try {
                        val request = com.google.android.gms.auth.api.identity
                            .GetPhoneNumberHintIntentRequest.builder().build()
                        val result = com.google.android.gms.auth.api.identity.Identity
                            .getSignInClient(activity)
                            .getPhoneNumberHintIntent(request)
                        val senderResult = Tasks.await(result)
                        phoneHintLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(senderResult).build()
                        )
                    } catch (e: Exception) {
                        android.util.Log.d("BODA_HINT", "Phone hint unavailable: ${e.message}")
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(BodaLang.get(viewModel.appLanguage, "phone_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text(BodaLang.get(viewModel.appLanguage, "phone_sub"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+256", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(Sp.sm))
                        OutlinedTextField(
                            value = viewModel.phoneInput,
                            onValueChange = { viewModel.phoneInput = it.take(9) },
                            placeholder = { Text("772 123456", color = Color(0xFF475569)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = Color(0xFFFDB913),
                                unfocusedBorderColor = Color(0xFF334155)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("phone_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.lg))
                    BodaButton(
                        text = "Get Verification Code",
                        onClick = {
                            if (viewModel.phoneInput.length >= 9) {
                                viewModel.startOtpFlow(activity)
                            }
                        },
                        enabled = viewModel.phoneInput.length >= 9 && !viewModel.isSendingOtp,
                        loading = viewModel.isSendingOtp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "send_otp_btn"
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    BodaOutlinedButton(
                        text = "LOGIN / REGISTER AS DRIVER",
                        onClick = { viewModel.startDriverOnboarding() },
                        borderColor = Color(0xFFFDB913),
                        contentColor = Color(0xFFFDB913),
                        icon = Icons.Default.TwoWheeler,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Sp.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF334155)
                        )
                        Text(
                            "  or  ",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF334155)
                        )
                    }

                    Spacer(modifier = Modifier.height(Sp.md))

                    val googleActivity = LocalContext.current as? android.app.Activity
                        ?: LocalContext.current
                    GoogleSignInButton(
                        onClick = { viewModel.signInWithGoogle(googleActivity) },
                        isLoading = viewModel.isSigningInWithGoogle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (!viewModel.isOtpVerified) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(BodaLang.get(viewModel.appLanguage, "otp_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.xs))
                    Text(BodaLang.get(viewModel.appLanguage, "otp_sub"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    OutlinedTextField(
                        value = viewModel.otpInput,
                        onValueChange = { viewModel.otpInput = it.take(6) },
                        placeholder = { Text("Enter 123456", color = Color(0xFF475569)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFFFDB913),
                            unfocusedBorderColor = Color(0xFF334155)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("otp_input")
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (viewModel.otpResendTimer > 0) {
                            Text(
                                "${BodaLang.get(viewModel.appLanguage, "otp_resend")} ${viewModel.otpResendTimer}s",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                "Resend Code Now",
                                color = if (viewModel.isSendingOtp) Color(0xFF64748B) else Color(0xFFFDB913),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = !viewModel.isSendingOtp) { viewModel.startOtpFlow() }
                            )
                        }
                        Text(
                            "Change Number",
                            color = if (viewModel.isSendingOtp) Color(0xFF64748B) else Color(0xFFE4002B),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable(enabled = !viewModel.isSendingOtp) { viewModel.otpSent = false }
                        )
                    }
                    Spacer(modifier = Modifier.height(Sp.lg))
                    BodaButton(
                        text = "Verify & Continue",
                        onClick = { viewModel.verifyOtp() },
                        enabled = viewModel.otpInput.length == 6 && !viewModel.isVerifyingOtp,
                        loading = viewModel.isVerifyingOtp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "verify_otp_btn"
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(BodaLang.get(viewModel.appLanguage, "profile_title"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(BodaLang.get(viewModel.appLanguage, "profile_desc"), color = Color(0xFF64748B), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaTextField(
                        value = viewModel.signupName,
                        onValueChange = { viewModel.signupName = it },
                        label = "Full Name",
                        placeholder = "Enter your full name",
                        testTag = "name_input"
                    )

                    Spacer(modifier = Modifier.height(Sp.md))
                    BodaTextField(
                        value = viewModel.referralCodeInput,
                        onValueChange = { viewModel.referralCodeInput = it },
                        label = "Referral Code (Optional)",
                        placeholder = "e.g. GULU-BODA-256",
                        testTag = "referral_code_input"
                    )

                    Spacer(modifier = Modifier.height(Sp.md))
                    Text("Select Avatar:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                        listOf(1, 2, 3, 4).forEach { id ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.selectedAvatarRes == id) Color(0xFFFDB913) else Color(0xFF1E293B))
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable { viewModel.selectedAvatarRes = id }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Sp.lg))
                    BodaButton(
                        text = "Register & Open App",
                        onClick = { viewModel.completeProfileSetup() },
                        enabled = viewModel.signupName.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "complete_setup_btn"
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ComposeColor.White,
            contentColor = ComposeColor(0xFF1F1F1F),
            disabledContainerColor = ComposeColor.White.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, ComposeColor(0xFFDADCE0))
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ComposeColor(0xFF4285F4),
                strokeWidth = 2.dp
            )
        } else {
            Canvas(modifier = Modifier.size(20.dp)) {
                val w = size.width
                val h = size.height
                val strokeW = w * 0.18f
                drawArc(ComposeColor(0xFF4285F4), -10f, 100f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(strokeW))
                drawArc(ComposeColor(0xFFEA4335), 90f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(strokeW))
                drawArc(ComposeColor(0xFFFBBC05), 180f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(strokeW))
                drawArc(ComposeColor(0xFF34A853), 270f, 90f, false,
                    topLeft = Offset(0f, 0f), size = Size(w, h), style = Stroke(strokeW))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Continue with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ComposeColor(0xFF3C4043)
            )
        }
    }
}

@Composable
fun OnboardingProgressBar(
    currentStep: Int,
    totalSteps: Int = 3,
    stepLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stepLabel, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("Step $currentStep of $totalSteps", color = Color(0xFF64748B), fontSize = 11.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(totalSteps) { index ->
                val isComplete = index < currentStep
                val isCurrent = index == currentStep - 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                isComplete -> Color(0xFFFDB913)
                                isCurrent -> Color(0xFFFDB913).copy(alpha = 0.4f)
                                else -> Color(0xFF334155)
                            }
                        )
                )
            }
        }
    }
}
