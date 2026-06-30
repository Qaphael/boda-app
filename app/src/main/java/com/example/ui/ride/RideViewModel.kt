package com.example.ui.ride

import com.example.data.*
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

fun BodaViewModel.fetchRouteForPoints(startLatLng: LatLng, endLatLng: LatLng) {
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        isLoadingRoute = true
        routeError = null
        
        val googleApiKey = try {
            com.example.BuildConfig.MAPS_API_KEY
        } catch (e: Throwable) {
            ""
        }
        
        val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                googleApiKey != "MY_MAPS_API_KEY" && 
                googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
        
        var pointsLoaded = false
        
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // --- OPTION A: GOOGLE DIRECTIONS API ---
        if (hasGoogleMapsKey && isOnline) {
            try {
                addPostgresLog("Requesting premium road routing from Google Directions API...")
                val googleUrl = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=${startLatLng.latitude},${startLatLng.longitude}" +
                        "&destination=${endLatLng.latitude},${endLatLng.longitude}" +
                        "&key=$googleApiKey"

                val request = okhttp3.Request.Builder()
                    .url(googleUrl)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = org.json.JSONObject(bodyString)
                        val status = json.optString("status")
                        if (status == "OK") {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val overviewPolyline = route.getJSONObject("overview_polyline")
                                val pointsStr = overviewPolyline.getString("points")
                                val points = decodePolyline(pointsStr)

                                // Parse turn-by-turn navigation steps
                                val steps = mutableListOf<NavStep>()
                                var totalDist = 0
                                var totalDur = 0
                                if (route.has("legs")) {
                                    val legs = route.getJSONArray("legs")
                                    if (legs.length() > 0) {
                                        val leg = legs.getJSONObject(0)
                                        totalDist = leg.optJSONObject("distance")?.optInt("value", 0) ?: 0
                                        totalDur = leg.optJSONObject("duration")?.optInt("value", 0) ?: 0
                                        if (leg.has("steps")) {
                                            val stepsArr = leg.getJSONArray("steps")
                                            for (s in 0 until stepsArr.length()) {
                                                val step = stepsArr.getJSONObject(s)
                                                val htmlInstr = step.optString("html_instructions", "")
                                                val instr = htmlInstr.replace(Regex("<[^>]*>"), "")
                                                val dist = step.optJSONObject("distance")?.optInt("value", 0) ?: 0
                                                val dur = step.optJSONObject("duration")?.optInt("value", 0) ?: 0
                                                val startLoc = step.optJSONObject("start_location")
                                                steps.add(NavStep(
                                                    instruction = instr,
                                                    distanceMeters = dist,
                                                    durationSeconds = dur,
                                                    startLat = startLoc?.optDouble("lat", 0.0) ?: 0.0,
                                                    startLng = startLoc?.optDouble("lng", 0.0) ?: 0.0
                                                ))
                                            }
                                        }
                                    }
                                }

                                if (points.isNotEmpty()) {
                                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        osrmRoutePoints = points
                                        navigationSteps = steps
                                        totalRouteDistanceMeters = totalDist
                                        totalRouteDurationSeconds = totalDur
                                        isLoadingRoute = false
                                        addPostgresLog("✓ Successfully loaded premium polyline from Google Directions API (${points.size} points, ${steps.size} nav steps).")
                                    }
                                    pointsLoaded = true
                                }
                            }
                        } else {
                            addPostgresLog("Google Directions error status: $status. Falling back to OSRM.")
                        }
                    } else {
                        addPostgresLog("Google Directions HTTP error: ${response.code}. Falling back to OSRM.")
                    }
                }
            } catch (e: Exception) {
                addPostgresLog("Google Directions fetch failed (${e.message}). Falling back to OSRM.")
            }
        }

        // --- OPTION B: OSRM API (FALLBACK / NO-KEY DEFAULT) ---
        if (!pointsLoaded) {
            try {
                if (!isOnline) {
                    throw java.io.IOException("Device is offline. OSRM lookup skipped.")
                }
                
                // OSRM expects coordinates in lng,lat format!
                val url = "https://router.project-osrm.org/route/v1/driving/" +
                        "${startLatLng.longitude},${startLatLng.latitude};" +
                        "${endLatLng.longitude},${endLatLng.latitude}" +
                        "?overview=full&geometries=geojson"
                
                addPostgresLog("Requesting real-world road routing coordinates from OSRM public API...")
                    
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw java.io.IOException("OSRM API response unsuccessful: ${response.code}")
                    }
                    val bodyString = response.body?.string() ?: throw java.io.IOException("Empty response body")
                    val json = org.json.JSONObject(bodyString)
                    val code = json.optString("code")
                    if (code != "Ok") {
                        throw java.io.IOException("OSRM error code: $code")
                    }
                    
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val firstRoute = routes.getJSONObject(0)
                        val geometry = firstRoute.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")
                        
                        val points = mutableListOf<LatLng>()
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            points.add(LatLng(lat, lng))
                        }
                        
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            osrmRoutePoints = points
                            isLoadingRoute = false
                            addPostgresLog("✓ Successfully loaded real road routing polyline from OSRM (${points.size} points).")
                        }
                    } else {
                        throw java.io.IOException("No routes found in OSRM response")
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    routeError = e.message
                    isLoadingRoute = false
                    errorMessage.value = "Routing failed. Showing estimated route."
                    addPostgresLog("Warning: OSRM Routing API unavailable (${e.message}). Falling back to simulated/grid street route.")
                }
            }
        }
    }
}

fun BodaViewModel.searchLocations(query: String) {
    searchJob?.cancel()
    if (query.trim().length < 2) {
        searchResults = emptyList()
        return
    }
    
    searchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        // Debounce for 400ms
        kotlinx.coroutines.delay(400)
        
        isSearchingPlaces = true
        searchError = null
        
        val googleApiKey = try {
            com.example.BuildConfig.MAPS_API_KEY
        } catch (e: Throwable) {
            ""
        }
        
        val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                googleApiKey != "MY_MAPS_API_KEY" && 
                googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
                
        val results = mutableListOf<SavedPlace>()
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        var searchSuccess = false
        
        // 1. Try Google Places Text Search API if key is present (returns POIs with coordinates)
        if (hasGoogleMapsKey && isOnline) {
            try {
                val searchQuery = if (query.lowercase().contains("gulu")) query else "$query, Gulu, Uganda"
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                        "?query=$encodedQuery" +
                        "&location=2.7750,32.2950" +
                        "&radius=50000" +
                        "&key=$googleApiKey"

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = org.json.JSONObject(bodyString)
                        val status = json.optString("status")
                        if (status == "OK") {
                            val jsonResults = json.getJSONArray("results")
                            for (i in 0 until minOf(jsonResults.length(), 8)) {
                                val item = jsonResults.getJSONObject(i)
                                val name = item.optString("name", "")
                                val formattedAddress = item.optString("formatted_address", "")
                                val geometry = item.getJSONObject("geometry")
                                val location = geometry.getJSONObject("location")
                                val lat = location.getDouble("lat")
                                val lng = location.getDouble("lng")
                                val label = name.ifEmpty { formattedAddress.substringBefore(",") }

                                results.add(SavedPlace(
                                    label = label,
                                    name = if (formattedAddress.isNotEmpty()) "$name, $formattedAddress" else name,
                                    latitude = lat,
                                    longitude = lng
                                ))
                            }
                            searchSuccess = true
                            addPostgresLog("Places Text Search returned ${results.size} results for '$query'")
                        } else {
                            addPostgresLog("Places Text Search status: $status")
                        }
                    }
                }
            } catch (e: Exception) {
                addPostgresLog("Google Places search failed: ${e.message}")
            }
        }

        // 1b. Try Google Geocoding API as secondary if Places returned nothing
        if (!searchSuccess && hasGoogleMapsKey && isOnline) {
            try {
                val addressQuery = if (query.lowercase().contains("gulu")) query else "$query, Gulu, Uganda"
                val encodedQuery = java.net.URLEncoder.encode(addressQuery, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedQuery&key=$googleApiKey"

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = org.json.JSONObject(bodyString)
                        val status = json.optString("status")
                        if (status == "OK") {
                            val jsonResults = json.getJSONArray("results")
                            for (i in 0 until minOf(jsonResults.length(), 6)) {
                                val item = jsonResults.getJSONObject(i)
                                val formattedAddress = item.getString("formatted_address")
                                val geometry = item.getJSONObject("geometry")
                                val location = geometry.getJSONObject("location")
                                val lat = location.getDouble("lat")
                                val lng = location.getDouble("lng")

                                val addressComponents = item.getJSONArray("address_components")
                                val label = if (addressComponents.length() > 0) {
                                    addressComponents.getJSONObject(0).getString("long_name")
                                } else {
                                    formattedAddress.substringBefore(",")
                                }

                                results.add(SavedPlace(
                                    label = label,
                                    name = formattedAddress,
                                    latitude = lat,
                                    longitude = lng
                                ))
                            }
                            searchSuccess = true
                        }
                    }
                }
            } catch (e: Exception) {
                addPostgresLog("Google Geocoding fallback failed: ${e.message}")
            }
        }
        
        // 2. Fallback or direct to Nominatim (OpenStreetMap) if Google fails or key is missing
        if (!searchSuccess && isOnline) {
            try {
                // Append Gulu, Uganda if not present
                val searchQuery = if (query.lowercase().contains("gulu")) query else "$query, Gulu, Uganda"
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=6&addressdetails=1"
                
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val jsonArray = org.json.JSONArray(bodyString)
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val displayName = obj.getString("display_name")
                            val lat = obj.getDouble("lat")
                            val lon = obj.getDouble("lon")
                            
                            val addressObj = obj.optJSONObject("address")
                            val label = when {
                                addressObj != null -> {
                                    addressObj.optString("amenity")
                                        .ifEmpty { addressObj.optString("building") }
                                        .ifEmpty { addressObj.optString("shop") }
                                        .ifEmpty { addressObj.optString("road") }
                                        .ifEmpty { addressObj.optString("suburb") }
                                        .ifEmpty { displayName.substringBefore(",") }
                                }
                                else -> displayName.substringBefore(",")
                            }
                            
                            results.add(SavedPlace(
                                label = label,
                                name = displayName,
                                latitude = lat,
                                longitude = lon
                            ))
                        }
                        searchSuccess = true
                    }
                }
            } catch (e: Exception) {
                addPostgresLog("OSM search failed: ${e.message}")
                errorMessage.value = "Location search unavailable. Showing default suggestions."
            }
        }
        
        // If both failed or device is offline, search local default suggestions as fallback
        if (results.isEmpty()) {
            val defaultSuggestions = listOf(
                SavedPlace(label = "Home", name = "Gulu Main Market, Gulu", latitude = 2.7712, longitude = 32.2985),
                SavedPlace(label = "Work", name = "Lacor Hospital, Gulu", latitude = 2.7933, longitude = 32.2571),
                SavedPlace(label = "University", name = "Gulu University, Laroo", latitude = 2.7842, longitude = 32.3214),
                SavedPlace(label = "Stadium", name = "Pece Stadium, Gulu", latitude = 2.7745, longitude = 32.3112),
                SavedPlace(label = "Town Hall", name = "Gulu Town Hall, Gulu", latitude = 2.7720, longitude = 32.3005),
                SavedPlace(label = "Airfield", name = "Gulu Airfield, Gulu", latitude = 2.7961, longitude = 32.2801)
            )
            val filteredLocal = defaultSuggestions.filter {
                it.label.contains(query, ignoreCase = true) ||
                it.name.contains(query, ignoreCase = true)
            }
            results.addAll(filteredLocal)
        }
        
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            searchResults = results
            isSearchingPlaces = false
        }
    }
}

fun BodaViewModel.fetchDistanceMatrix() {
    val pickup = pickupPlace
    val dropoff = dropoffPlace
    if (pickup == null || dropoff == null) {
        googleDistanceKm = null
        googleDurationMins = null
        return
    }
    
    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            isFetchingDistanceMatrix = true
            distanceMatrixError = null
        }
        
        val googleApiKey = try {
            com.example.BuildConfig.MAPS_API_KEY
        } catch (e: Throwable) {
            ""
        }
        
        val hasGoogleMapsKey = googleApiKey.isNotEmpty() && 
                googleApiKey != "MY_MAPS_API_KEY" && 
                googleApiKey != "MAPS_API_KEY_DEFAULT_VALUE"
                
        if (hasGoogleMapsKey && isOnline) {
            try {
                addPostgresLog("Querying Google Maps Distance Matrix API for dynamic fare & traffic surge: FROM [${pickup.label}] TO [${dropoff.label}]")
                val url = "https://maps.googleapis.com/maps/api/distancematrix/json" +
                        "?origins=${pickup.latitude},${pickup.longitude}" +
                        "&destinations=${dropoff.latitude},${dropoff.longitude}" +
                        "&key=$googleApiKey"
                        
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                    
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "BodaGuluApp/1.0")
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    val bodyString = response.body?.string() ?: ""
                    addPostgresLog("Distance Matrix HTTP ${response.code}: ${bodyString.take(300)}")
                    if (response.isSuccessful) {
                        val json = org.json.JSONObject(bodyString)
                        val status = json.optString("status")
                        if (status == "OK") {
                            val rows = json.getJSONArray("rows")
                            if (rows.length() > 0) {
                                val elements = rows.getJSONObject(0).getJSONArray("elements")
                                if (elements.length() > 0) {
                                    val element = elements.getJSONObject(0)
                                    val elementStatus = element.optString("status")
                                    if (elementStatus == "OK") {
                                        val distanceObj = element.getJSONObject("distance")
                                        val durationObj = element.getJSONObject("duration")
                                        
                                        val meters = distanceObj.getDouble("value")
                                        val seconds = durationObj.getDouble("value")
                                        
                                        val km = meters / 1000.0
                                        val mins = (seconds / 60.0).toInt().coerceAtLeast(1)
                                        
                                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            googleDistanceKm = km
                                            googleDurationMins = mins
                                            addPostgresLog("✓ Distance Matrix: ${"%.2f".format(km)} km, $mins mins.")
                                        }
                                    } else {
                                        throw Exception("Distance Matrix element status: $elementStatus")
                                    }
                                } else {
                                    throw Exception("No elements returned")
                                }
                            } else {
                                throw Exception("No rows returned")
                            }
                        } else {
                            throw Exception("Distance Matrix status: $status")
                        }
                    } else {
                        throw Exception("HTTP Error: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    distanceMatrixError = e.message
                    addPostgresLog("Distance Matrix API failed (${e.message}). Using Haversine fallback.")
                }
            } finally {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isFetchingDistanceMatrix = false
                }
            }
        } else {
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                isFetchingDistanceMatrix = false
                addPostgresLog("Google Maps key missing/invalid. Distance Matrix bypassed, using Gulu Local Haversine Engine.")
            }
        }
    }
}

fun BodaViewModel.triggerOSRMRouteFetch() {
    val pickup = pickupPlace ?: return
    val dropoff = dropoffPlace ?: return

    val startLatLng = LatLng(pickup.latitude, pickup.longitude)
    val endLatLng = LatLng(dropoff.latitude, dropoff.longitude)
    fetchRouteForPoints(startLatLng, endLatLng)
    fetchDistanceMatrix()
}

fun BodaViewModel.createRideRequest() {
    bookTripViaBackend()
}

fun BodaViewModel.cancelRideRequest() {
    currentRideRequest = null
    isSearchingForDriver = false
}

fun BodaViewModel.bookTripViaBackend() {
    val pickup = pickupPlace ?: return
    val dropoff = dropoffPlace ?: return
    val user = userProfile.value ?: return
    
    viewModelScope.launch {
        val fare = calculatedFare
        apiRepository.bookTrip(
            pickupName = pickup.name,
            pickupLat = pickup.latitude,
            pickupLon = pickup.longitude,
            dropoffName = dropoff.name,
            dropoffLat = dropoff.latitude,
            dropoffLon = dropoff.longitude,
            distanceKm = googleDistanceKm ?: 0.0,
            durationMins = googleDurationMins ?: 0,
            fare = fare,
            paymentMethod = selectedPaymentMethod
        ).fold(
            onSuccess = { trip ->
                addPostgresLog("✓ Trip booked via PostgreSQL: ${trip.id}")
                currentRideRequest = RideRequest(
                    id = trip.id.toString(),
                    riderId = auth.currentUser?.uid ?: "",
                    riderName = user.name,
                    riderPhone = user.phoneNumber,
                    pickupName = pickup.name,
                    pickupLat = pickup.latitude,
                    pickupLng = pickup.longitude,
                    dropoffName = dropoff.name,
                    dropoffLat = dropoff.latitude,
                    dropoffLng = dropoff.longitude,
                    fare = fare,
                    paymentMethod = selectedPaymentMethod,
                    status = trip.status
                )
                isSearchingForDriver = true
            },
            onFailure = { e ->
                errorMessage.value = "Failed to book trip: ${e.message}"
                addPostgresLog("✗ Trip booking failed: ${e.message}")
            }
        )
    }
}

fun BodaViewModel.calculateFareViaBackend(distanceKm: Double, durationMins: Int) {
    viewModelScope.launch {
        apiRepository.calculateFare(distanceKm, durationMins).fold(
            onSuccess = { fareResponse ->
                addPostgresLog("✓ Fare calculated: UGX ${fareResponse.final_fare} (surge: ${fareResponse.surge_multiplier}x)")
            },
            onFailure = { e ->
                addPostgresLog("✗ Fare calculation failed: ${e.message}")
            }
        )
    }
}

fun BodaViewModel.validatePromoViaBackend(code: String) {
    viewModelScope.launch {
        val fare = calculatedFare
        apiRepository.validatePromo(code, fare).fold(
            onSuccess = { promoResponse ->
                if (promoResponse.valid) {
                    activePromoDiscount.value = promoResponse.discount_amount.toDouble()
                    activePromoCode = code
                    addPostgresLog("✓ Promo applied: -UGX ${promoResponse.discount_amount}")
                } else {
                    errorMessage.value = promoResponse.message ?: "Invalid promo code"
                }
            },
            onFailure = { e ->
                errorMessage.value = "Failed to validate promo: ${e.message}"
            }
        )
    }
}

fun BodaViewModel.confirmBooking() {
    if (pickupPlace == null || dropoffPlace == null) {
        errorMessage.value = "Please select both pickup and dropoff locations."
        return
    }
    
    simulationState = "searching"
    navigateTo(Screen.Matching)
    
    simulationJob?.cancel()
    simulationJob = viewModelScope.launch {
        // Simulate matching for 4 seconds
        for (i in 1..40) {
            delay(100)
            matchProgress = i / 40f
        }
        
        // Randomly select one of Gulu's top riders
        val riderNames = listOf("Okeny Patrick", "Adong Scovia", "Akena Christopher", "Kidega Moses")
        val plates = listOf("UEG 412X", "UED 891B", "UEF 201A", "UEH 556W")
        val photoIds = listOf(2, 3, 4, 1)
        val index = Random.nextInt(riderNames.size)
        
        val fareWithDiscount = (calculatedFare - activePromoDiscount.value).coerceAtLeast(1000.0)

        val riderPhone = "+256 781 " + (100000 + Random.nextInt(899999))
        val packageDetails = if (serviceType == "delivery") parcelDetails else null
        val recipientNameVal = if (serviceType == "delivery") recipientName else null
        val recipientPhoneVal = if (serviceType == "delivery") recipientPhone else null

        val backendTrip = apiRepository.bookTrip(
            pickupName = pickupPlace!!.name,
            pickupLat = pickupPlace!!.latitude,
            pickupLon = pickupPlace!!.longitude,
            dropoffName = dropoffPlace!!.name,
            dropoffLat = dropoffPlace!!.latitude,
            dropoffLon = dropoffPlace!!.longitude,
            distanceKm = googleDistanceKm ?: 0.0,
            durationMins = googleDurationMins ?: 0,
            fare = fareWithDiscount,
            paymentMethod = selectedPaymentMethod
        ).getOrNull()

        if (backendTrip != null) {
            currentSimulationTrip = Trip(
                id = backendTrip.id,
                type = serviceType,
                pickupName = pickupPlace!!.name,
                dropoffName = dropoffPlace!!.name,
                fare = fareWithDiscount,
                paymentMethod = selectedPaymentMethod,
                status = "matched",
                riderName = riderNames[index],
                riderPlate = plates[index],
                riderPhone = riderPhone,
                riderPhotoResId = photoIds[index],
                packageDetails = packageDetails,
                recipientName = recipientNameVal,
                recipientPhone = recipientPhoneVal
            )
            bookingMatchTripId = backendTrip.id.toLong()
            currentRideRequest = RideRequest(
                id = backendTrip.id.toString(),
                riderId = auth.currentUser?.uid ?: "",
                riderName = userProfile.value?.name ?: "",
                riderPhone = userProfile.value?.phoneNumber ?: "",
                pickupName = pickupPlace!!.name,
                pickupLat = pickupPlace!!.latitude,
                pickupLng = pickupPlace!!.longitude,
                dropoffName = dropoffPlace!!.name,
                dropoffLat = dropoffPlace!!.latitude,
                dropoffLng = dropoffPlace!!.longitude,
                fare = fareWithDiscount,
                paymentMethod = selectedPaymentMethod,
                status = "matched"
            )
            addPostgresLog("✓ Trip booked via PostgreSQL: ${backendTrip.id}")
        } else {
            val localTrip = Trip(
                type = serviceType,
                pickupName = pickupPlace!!.name,
                dropoffName = dropoffPlace!!.name,
                fare = fareWithDiscount,
                paymentMethod = selectedPaymentMethod,
                status = "matched",
                riderName = riderNames[index],
                riderPlate = plates[index],
                riderPhone = riderPhone,
                riderPhotoResId = photoIds[index],
                packageDetails = packageDetails,
                recipientName = recipientNameVal,
                recipientPhone = recipientPhoneVal
            )
            val tripId = repository.addTrip(localTrip)
            bookingMatchTripId = tripId
            currentSimulationTrip = localTrip.copy(id = tripId.toInt())
        }

        // Shift screen to Rider En Route
        simulationState = "enroute"

        // Wallet hold at booking confirmation
        if (selectedPaymentMethod == "Wallet") {
            val holdRef = "HOLD-BODA-${bookingMatchTripId}"
            repository.addTransaction(WalletTransaction(
                amount = fareWithDiscount,
                type = "payment",
                status = "pending",
                phoneNumber = userProfile.value?.phoneNumber ?: "",
                timestamp = System.currentTimeMillis(),
                provider = "Wallet",
                reference = holdRef
            ))
            refreshWalletBalance()
        }
        simulationCountdown = 8
        fetchRouteForPoints(
            LatLng(2.775, 32.295),
            LatLng(pickupPlace!!.latitude, pickupPlace!!.longitude)
        )
        navigateTo(Screen.RiderEnRoute)
        
        // Countdown for rider's ETA
        while (simulationCountdown > 0) {
            delay(1000)
            simulationCountdown--
        }
        
        // Rider Arrives
        simulationState = "arrived"
        // Wait for user to tap "Start Ride" on the en route screen (simulates meeting rider and entering security OTP)
    }
}

fun BodaViewModel.startActiveTrip() {
    if (currentSimulationTrip == null) return
    
    simulationState = "active"
    simulationCountdown = 10
    simulationRouteProgress = 0f
    fetchRouteForPoints(
        LatLng(pickupPlace!!.latitude, pickupPlace!!.longitude),
        LatLng(dropoffPlace!!.latitude, dropoffPlace!!.longitude)
    )
    navigateTo(Screen.ActiveTrip)
    
    simulationJob?.cancel()
    simulationJob = viewModelScope.launch {
        while (simulationCountdown > 0) {
            delay(1000)
            simulationCountdown--
            simulationRouteProgress = (10 - simulationCountdown) / 10f
        }
        
        // Trip completes
        simulationState = "completed"
        
        val ongoing = currentSimulationTrip
        if (ongoing != null) {
            val updated = ongoing.copy(status = "completed")
            repository.updateTrip(updated)
            currentSimulationTrip = updated
            onPassengerTripCompleted()
        }
        
        navigateTo(Screen.PostTrip)
    }
}

fun BodaViewModel.cancelActiveTrip(reason: String) {
    simulationJob?.cancel()
    viewModelScope.launch {
        val ongoing = currentSimulationTrip
        if (ongoing != null) {
            val updated = ongoing.copy(status = "canceled", comment = "Canceled: $reason")
            repository.updateTrip(updated)
            apiRepository.updateTripStatus(ongoing.id, "canceled")
            
            // Cancellation fee only if trip is actively in progress
            if (simulationState == "active") {
                repository.addTransaction(WalletTransaction(
                    amount = 1000.0,
                    type = "payment",
                    status = "completed",
                    phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                    timestamp = System.currentTimeMillis(),
                    provider = "Wallet",
                    reference = "BODA-CANCELLATION-FEE-${ongoing.id}"
                ))
                apiRepository.walletPay(1000.0, "Wallet")
                refreshWalletBalance()
            }
        }
        simulationState = "idle"
        currentSimulationTrip = null
        navigateTo(Screen.Home)
    }
}

fun BodaViewModel.submitPostTripRating(stars: Int, comment: String) {
    viewModelScope.launch {
        val ongoing = currentSimulationTrip
        if (ongoing != null) {
            val updated = ongoing.copy(rating = stars, comment = comment)
            repository.updateTrip(updated)
            apiRepository.updateTripStatus(ongoing.id, "completed", stars, comment)
        }
        
        // Complete the pending wallet hold if payment method is Wallet
        val finalFare = (calculatedFare - activePromoDiscount.value).coerceAtLeast(1000.0)
        if (selectedPaymentMethod == "Wallet" && ongoing != null) {
            repository.completePendingPayment("HOLD-BODA-${ongoing.id}")
            apiRepository.walletPay(finalFare, "Wallet")
        } else if (ongoing != null) {
            // Mobile money payments — record at completion
            repository.addTransaction(WalletTransaction(
                amount = finalFare,
                type = "payment",
                status = "completed",
                phoneNumber = userProfile.value?.phoneNumber ?: "+256 772 123456",
                timestamp = System.currentTimeMillis(),
                provider = selectedPaymentMethod,
                reference = "${selectedPaymentMethod}-MOM-RIDE-${ongoing.id}"
            ))
            apiRepository.walletPay(finalFare, selectedPaymentMethod)
        }

        refreshWalletBalance()

        // Reset state
        pickupPlace = null
        dropoffPlace = null
        pickupText = ""
        dropoffText = ""
        parcelDetails = ""
        recipientName = ""
        recipientPhone = ""
        scheduledBookingDateTime = null
        activePromoDiscount.value = 0.0
        activePromoCode = null
        googleDistanceKm = null
        googleDurationMins = null
        distanceMatrixError = null
        currentSimulationTrip = null
        simulationState = "idle"
        osrmRoutePoints = emptyList()
        
        navigateTo(Screen.Home)
    }
}

fun BodaViewModel.onPassengerTripCompleted() {
    viewModelScope.launch {
        val myPhone = userProfile.value?.phoneNumber ?: ""
        if (myPhone.isNotEmpty()) {
            val pendingRef = repository.getReferralByPhone(myPhone)
            if (pendingRef != null && pendingRef.status == "pending") {
                // Credit the referred user's wallet with the welcome bonus
                repository.addTransaction(WalletTransaction(
                    amount = 3000.0,
                    type = "topup",
                    status = "completed",
                    phoneNumber = myPhone,
                    timestamp = System.currentTimeMillis(),
                    provider = "Wallet",
                    reference = "REF-WELCOME-${pendingRef.id}"
                ))

                // Complete referral on backend — credits the referrer's wallet there
                apiRepository.completeReferralOnBackend(pendingRef.id).onSuccess {
                    val completedRef = pendingRef.copy(status = "completed", timestamp = System.currentTimeMillis())
                    repository.addReferral(completedRef)
                    activePromoMessage = "Welcome Bonus: UGX 3,000 credited to your wallet!"
                }.onFailure {
                    // Still mark locally so we don't retry forever
                    val completedRef = pendingRef.copy(status = "completed", timestamp = System.currentTimeMillis())
                    repository.addReferral(completedRef)
                    activePromoMessage = "Welcome Bonus credited. Referrer reward pending."
                }

                refreshWalletBalance()
            }
        }
    }
}

fun BodaViewModel.disputeTrip(tripId: Int, reason: String, details: String) {
    viewModelScope.launch {
        val list = trips.first()
        val match = list.find { it.id == tripId }
        if (match != null) {
            repository.updateTrip(match.copy(
                status = "disputed",
                disputeReason = reason,
                disputeEvidence = details
            ))
            apiRepository.updateTripStatus(tripId, "disputed", disputeReason = reason, disputeEvidence = details)
        }
    }
}
