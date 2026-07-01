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
import com.example.data.EmergencyContact
import com.example.ui.BodaViewModel
import com.example.ui.home.navigateBack
import com.example.ui.components.BodaButton
import com.example.ui.components.BodaCard
import com.example.ui.components.BodaTextField
import androidx.compose.material3.MaterialTheme

// --- SCREEN 13: EMERGENCY CONTACTS (SAFETY PATHS) ---
@Composable
fun EmergencyContactsScreen(viewModel: BodaViewModel, contacts: List<EmergencyContact>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Emergency Contacts Linkage", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Link up to 3 trusted contacts. We will SMS them a live Gulu tracking map link whenever you trigger the 1-Tap SOS.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        // Contact fields insertion
        if (contacts.size < 3) {
            BodaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Add Emergency Contact Link", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    BodaTextField(
                        value = viewModel.newEmergencyName,
                        onValueChange = { viewModel.newEmergencyName = it },
                        label = "Contact Name",
                        placeholder = "e.g. Uncle Benson",
                        testTag = "emergency_contact_name_input"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BodaTextField(
                        value = viewModel.newEmergencyPhone,
                        onValueChange = { viewModel.newEmergencyPhone = it },
                        label = "Phone Number",
                        placeholder = "e.g. +256 772 111222",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        testTag = "emergency_contact_phone_input"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BodaButton(
                        text = "Add Contact Now",
                        onClick = { viewModel.addEmergencyContact() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                        Text(contact.name, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                        Text(contact.phoneNumber, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.removeEmergencyContact(contact) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            }
        }
    }
}
