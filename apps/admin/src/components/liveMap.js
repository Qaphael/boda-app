function initMap() {
    map = L.map('map').setView(GULU_LAT_LON, 13);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
        attribution: '&copy; OpenStreetMap &copy; CARTO',
        subdomains: 'abcd', maxZoom: 20
    }).addTo(map);

    const landmarks = [
        { name: "Gulu Main Market", coords: [2.7712, 32.2985] },
        { name: "Lacor Hospital", coords: [2.7933, 32.2571] },
        { name: "Gulu University", coords: [2.7842, 32.3214] },
        { name: "Pece Stadium", coords: [2.7745, 32.3112] }
    ];
    landmarks.forEach(lm => {
        const icon = L.divIcon({
            className: 'bg-indigo-500/20 text-indigo-400 border border-indigo-400/40 rounded-lg text-[9px] font-bold px-2 py-1 whitespace-nowrap',
            html: `📍 ${lm.name}`, iconSize: [120, 24]
        });
        L.marker(lm.coords, { icon }).addTo(map).bindPopup(`<b>${lm.name}</b>`);
    });
    logConsole("Map initialized", "system");
}

function updateMapMarkers() {
    Object.values(driverMarkers).forEach(m => map.removeLayer(m));
    driverMarkers = {};
    driversData.forEach(d => {
        if (!d.latitude || !d.longitude) return;
        const icon = L.divIcon({
            html: `<div class="relative flex items-center justify-center">
                <div class="absolute w-5 h-5 rounded-full ${d.is_online ? 'bg-amber-500/30 animate-ping' : 'hidden'}"></div>
                <div class="w-3.5 h-3.5 rounded-full ${d.is_online ? 'bg-amber-500' : 'bg-slate-400'} border-2 border-slate-900 shadow-md"></div>
            </div>`,
            className: 'custom-div-icon', iconSize: [20, 20]
        });
        const m = L.marker([d.latitude, d.longitude], { icon }).addTo(map)
            .bindPopup(`<b>${d.full_name}</b><br>Plate: ${d.plate_number}<br>${d.is_online ? 'Online' : 'Offline'}`);
        driverMarkers[d.uid] = m;
    });
}
