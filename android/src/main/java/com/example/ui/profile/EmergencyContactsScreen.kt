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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyContact
import com.example.ui.BodaViewModel
import com.example.ui.home.navigateBack
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField
import com.example.ui.components.Color
import com.example.ui.components.Sp

// --- SCREEN 13: EMERGENCY CONTACTS (SAFETY PATHS) ---
@Composable
fun EmergencyContactsScreen(viewModel: BodaViewModel, contacts: List<EmergencyContact>) {
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
            Text("Emergency Contacts Linkage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(Sp.sm))
        Text("Link up to 3 trusted contacts. We will SMS them a live Gulu tracking map link whenever you trigger the 1-Tap SOS.", color = Color(0xFF94A3B8), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(Sp.md))

        // Contact fields insertion
        if (contacts.size < 3) {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Add Emergency Contact Link", color = Color(0xFFFDB913), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.newEmergencyName,
                        onValueChange = { viewModel.newEmergencyName = it },
                        label = "Contact Name",
                        placeholder = "e.g. Uncle Benson",
                        testTag = "emergency_contact_name_input"
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaTextField(
                        value = viewModel.newEmergencyPhone,
                        onValueChange = { viewModel.newEmergencyPhone = it },
                        label = "Phone Number",
                        placeholder = "e.g. +256 772 111222",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        testTag = "emergency_contact_phone_input"
                    )
                    Spacer(modifier = Modifier.height(Sp.sm))
                    BodaButton(
                        text = "Add Contact Now",
                        onClick = { viewModel.addEmergencyContact() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Sp.md))

        LazyColumn {
            items(contacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(contact.phoneNumber, color = Color(0xFF64748B), fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.removeEmergencyContact(contact) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE4002B))
                    }
                }
                HorizontalDivider(color = Color(0xFF1E293B))
            }
        }
    }
}
