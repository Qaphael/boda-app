package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SavedPlace
import com.example.ui.BodaViewModel
import com.example.ui.home.navigateBack
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField
import com.example.ui.components.Color
import com.example.ui.components.Sp

// --- SCREEN 14: SAVED PLACES MANAGEMENT ---
@Composable
fun SavedPlacesManageScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(Sp.sm))
            Text("Saved Places", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))

        BodaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Add New Location Bookmark", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaTextField(
                    value = viewModel.newPlaceLabel,
                    onValueChange = { viewModel.newPlaceLabel = it },
                    label = "Place Label",
                    placeholder = "e.g. Market, Church, Gulu School",
                    testTag = "new_place_label_input"
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaTextField(
                    value = viewModel.newPlaceName,
                    onValueChange = { viewModel.newPlaceName = it },
                    label = "Address / Landmarks",
                    placeholder = "Full Address / Landmarks in Gulu",
                    testTag = "new_place_address_input"
                )
                Spacer(modifier = Modifier.height(Sp.sm))
                BodaButton(
                    text = "Bookmark Place Now",
                    onClick = { viewModel.addSavedPlace() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        LazyColumn {
            items(savedPlaces) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFFDB913))
                        Spacer(modifier = Modifier.width(Sp.sm))
                        Column {
                            Text(place.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(place.name, color = Color(0xFF64748B), fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { viewModel.removeSavedPlace(place) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE4002B))
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }
    }
}
