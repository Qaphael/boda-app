package com.example.ui.ride

import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.example.ui.home.navigateTo
import com.example.ui.home.navigateBack

import com.example.ui.components.BodaTextField
import com.example.ui.components.BodaSecondaryButton
import com.example.data.SavedPlace
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// --- SCREEN 4: SEARCH PLACES ---
@Composable
fun SearchPlacesScreen(viewModel: BodaViewModel, savedPlaces: List<SavedPlace>) {
    val defaultSuggestions = listOf(
        SavedPlace(label = "Home", name = "Gulu Main Market, Gulu", latitude = 2.7712, longitude = 32.2985),
        SavedPlace(label = "Work", name = "Lacor Hospital, Gulu", latitude = 2.7933, longitude = 32.2571),
        SavedPlace(label = "University", name = "Gulu University, Laroo", latitude = 2.7842, longitude = 32.3214),
        SavedPlace(label = "Stadium", name = "Pece Stadium, Gulu", latitude = 2.7745, longitude = 32.3112),
        SavedPlace(label = "Town Hall", name = "Gulu Town Hall, Gulu", latitude = 2.7720, longitude = 32.3005),
        SavedPlace(label = "Airfield", name = "Gulu Airfield, Gulu", latitude = 2.7961, longitude = 32.2801)
    )

    // Determine initial focused field based on what's empty
    var activeFocus by remember { 
        mutableStateOf(if (viewModel.pickupPlace == null) "pickup" else "dropoff") 
    }

    // Active query text
    val activeQuery = if (activeFocus == "pickup") viewModel.pickupText else viewModel.dropoffText

    // Trigger debounced real search
    LaunchedEffect(activeQuery) {
        viewModel.searchLocations(activeQuery)
    }

    // Filtered places: use real-time search results if query is typed, otherwise default lists
    val filteredPlaces = if (activeQuery.trim().length >= 2) {
        viewModel.searchResults
    } else {
        (savedPlaces + defaultSuggestions).distinctBy { it.name }.filter {
            it.label.contains(activeQuery, ignoreCase = true) ||
            it.name.contains(activeQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Select Locations", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pickup query
        BodaTextField(
            value = viewModel.pickupText,
            onValueChange = { 
                viewModel.pickupText = it
                activeFocus = "pickup"
            },
            label = "Pickup Location" + if (activeFocus == "pickup") " 🟢 (Searching...)" else "",
            placeholder = "Search pickup location...",
            leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
            trailingIcon = if (viewModel.pickupText.isNotEmpty() || viewModel.pickupPlace != null) {
                {
                    IconButton(onClick = {
                        viewModel.pickupText = ""
                        viewModel.pickupPlace = null
                        activeFocus = "pickup"
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) activeFocus = "pickup" }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dropoff query
        BodaTextField(
            value = viewModel.dropoffText,
            onValueChange = { 
                viewModel.dropoffText = it
                activeFocus = "dropoff"
            },
            label = "Drop-off Destination" + if (activeFocus == "dropoff") " 🟢 (Searching...)" else "",
            placeholder = "Where are you heading?",
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            trailingIcon = if (viewModel.dropoffText.isNotEmpty() || viewModel.dropoffPlace != null) {
                {
                    IconButton(onClick = {
                        viewModel.dropoffText = ""
                        viewModel.dropoffPlace = null
                        activeFocus = "dropoff"
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) activeFocus = "dropoff" }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- NEW OPTION: CURRENT LOCATION ---
        val loc = viewModel.currentLocation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)
                .clickable {
                    val currentLocation = if (loc != null) {
                        SavedPlace(
                            label = "Current Location",
                            name = "Current Location",
                            latitude = loc.latitude,
                            longitude = loc.longitude
                        )
                    } else {
                        SavedPlace(
                            label = "Current Location",
                            name = "Gulu City Centre",
                            latitude = 2.7750,
                            longitude = 32.2950
                        )
                    }
                    if (activeFocus == "pickup") {
                        viewModel.pickupPlace = currentLocation
                        viewModel.pickupText = currentLocation.name
                        activeFocus = "dropoff"
                    } else {
                        viewModel.dropoffPlace = currentLocation
                        viewModel.dropoffText = currentLocation.name
                        if (viewModel.pickupPlace != null) {
                            viewModel.navigateTo(Screen.RoutePreview)
                        } else {
                            activeFocus = "pickup"
                        }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Use Current Location", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                Text(
                    if (loc != null) "GPS: ${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)}"
                    else "Waiting for GPS...",
                    color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (activeFocus == "pickup") "Pickup Suggestions (${filteredPlaces.size})" else "Drop-off Suggestions (${filteredPlaces.size})",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f)
            )
            if (viewModel.isSearchingPlaces) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Searching map...", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredPlaces.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching Gulu locations found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Try typing Lacor, Market, Pece, or University", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(filteredPlaces) { place ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (activeFocus == "pickup") {
                                    viewModel.pickupPlace = place
                                    viewModel.pickupText = place.name
                                    activeFocus = "dropoff"
                                } else {
                                    viewModel.dropoffPlace = place
                                    viewModel.dropoffText = place.name
                                    if (viewModel.pickupPlace != null) {
                                        viewModel.navigateTo(Screen.RoutePreview)
                                    } else {
                                        activeFocus = "pickup"
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isSaved = savedPlaces.any { it.name == place.name }
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.Place,
                            contentDescription = null,
                            tint = if (isSaved) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(place.label, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.labelLarge)
                            Text(place.name, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sim map pin selection option
        BodaSecondaryButton(
            text = "Select Location via Map Pin (Simulator)",
            onClick = {
                val randomPlace = defaultSuggestions.random()
                if (activeFocus == "pickup") {
                    viewModel.pickupPlace = randomPlace
                    viewModel.pickupText = randomPlace.name
                    activeFocus = "dropoff"
                } else {
                    viewModel.dropoffPlace = randomPlace
                    viewModel.dropoffText = randomPlace.name
                    if (viewModel.pickupPlace != null) {
                        viewModel.navigateTo(Screen.RoutePreview)
                    } else {
                        activeFocus = "pickup"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
