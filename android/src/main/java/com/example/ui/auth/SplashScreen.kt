package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.BodaViewModel
import com.example.ui.components.Color
import com.example.ui.components.Sp
import com.example.ui.components.BodaLang

@Composable
fun SplashScreen(viewModel: BodaViewModel) {
    var authChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Wait briefly for auth state listener to resolve
        kotlinx.coroutines.delay(500)
        authChecked = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.boda_logo),
                contentDescription = "Boda Gulu Brand Logo",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(Sp.md))
            Text(
                "Boda Gulu",
                color = Color(0xFFFDB913),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(Sp.xs))
            Text(
                BodaLang.get(viewModel.appLanguage, "splash_tagline"),
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(Sp.xxl))
            if (!authChecked) {
                CircularProgressIndicator(
                    color = Color(0xFFFDB913),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
