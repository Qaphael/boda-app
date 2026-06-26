// SafeBoda Gulu - Admin Dashboard (Live Data Only)

const GULU_LAT_LON = [2.7712, 32.2985];
const API_BASE = window.location.origin;
let map, driverMarkers = {};
let driversData = [], ridersData = [], tripsData = [];
let currentSOSAlert = null;

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    startClock();
    loadProductionData();
    loadPromoCampaigns();
    setInterval(loadProductionData, 10000);
    setInterval(checkSOSAlerts, 5000);
});

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

function startClock() {
    setInterval(() => {
        document.getElementById('current-time').innerText = `UTC ${new Date().toISOString().replace('T', ' ').substring(0, 19)}`;
    }, 1000);
}

function logConsole(message, type = "info") {
    const el = document.getElementById('system-console-logs');
    if (!el) return;
    const d = document.createElement('div');
    const ts = new Date().toISOString().substring(11, 19);
    const color = type === "system" ? "text-amber-400" : type === "error" ? "text-rose-400" : "text-emerald-400";
    d.className = `${color} flex space-x-2 py-0.5`;
    d.innerHTML = `<span class="text-slate-500">[${ts}]</span><span>${message}</span>`;
    el.appendChild(d);
    el.scrollTop = el.scrollHeight;
}

function clearLogs() { document.getElementById('system-console-logs').innerHTML = ''; }

// ---- PRODUCTION DATA ----
async function loadProductionData() {
    // Stats
    try {
        const r = await fetch(`${API_BASE}/api/admin/stats`);
        if (r.ok) {
            const s = await r.json();
            setText('dash-revenue-val', `UGX ${(s.grossRevenue || 0).toLocaleString()}`);
            setText('dash-active-val', `${s.activeTrips || 0} Active`);
            setText('dash-online-val', `${s.onlineDrivers || 0} Online`);
            setText('completed-trips-val', (s.completedTrips || 0).toLocaleString());
            setText('system-revenue-val', `UGX ${((s.grossRevenue || 0) / 1000000).toFixed(2)}M`);
            setText('online-drivers-val', s.onlineDrivers || 0);
            setText('active-trips-val', s.activeTrips || 0);
        }
    } catch (e) { logConsole('Stats fetch failed', 'error'); }

    // Drivers
    try {
        const r = await fetch(`${API_BASE}/api/admin/drivers`);
        if (r.ok) {
            driversData = await r.json();
            renderDriversTable();
            updateMapMarkers();
        }
    } catch (e) {}

    // Riders
    try {
        const r = await fetch(`${API_BASE}/api/admin/riders`);
        if (r.ok) { ridersData = await r.json(); renderRidersTable(); }
    } catch (e) {}

    // Trips + Dispatch
    try {
        const r = await fetch(`${API_BASE}/api/admin/trips`);
        if (r.ok) {
            tripsData = await r.json();
            renderTripsTable();
            renderDispatchList(tripsData.filter(t => ['matching', 'en_route', 'active'].includes(t.status)));
        }
    } catch (e) {}

    // Active trips
    try {
        const r = await fetch(`${API_BASE}/api/admin/active-trips`);
        if (r.ok) {
            const active = await r.json();
            renderDispatchList(active);
            setText('dispatch-count', `${active.length} Active`);
        }
    } catch (e) {}
}

function setText(id, val) { const e = document.getElementById(id); if (e) e.innerText = val; }

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

function renderDispatchList(trips) {
    const el = document.getElementById('dispatch-list');
    if (!el) return;
    if (!trips || trips.length === 0) {
        el.innerHTML = '<div class="text-center text-slate-500 text-xs py-8">No active dispatches</div>';
        return;
    }
    el.innerHTML = '';
    trips.forEach(t => {
        const sc = t.status === 'matching' ? 'bg-sky-500/10 text-sky-400 border-sky-500/20' :
                   t.status === 'en_route' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                   'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
        const sl = t.status === 'matching' ? 'Matching' : t.status === 'en_route' ? 'En Route' : 'Active';
        el.innerHTML += `<div class="bg-slate-800/80 border border-slate-700/60 p-4 rounded-xl space-y-2">
            <div class="flex justify-between items-center">
                <span class="text-xs font-bold text-amber-500">${t.trip_code}</span>
                <span class="text-[10px] border px-2 py-0.5 rounded-full font-semibold ${sc}">${sl}</span>
            </div>
            <p class="text-[11px] text-slate-300">🟢 ${t.pickup_name}</p>
            <p class="text-[11px] text-slate-300">🔴 ${t.dropoff_name}</p>
            <div class="border-t border-slate-700/50 pt-2 flex justify-between items-center">
                <span class="text-xs text-slate-400">Fare:</span>
                <span class="text-xs font-extrabold text-emerald-400">UGX ${parseFloat(t.fare || 0).toLocaleString()}</span>
            </div>
        </div>`;
    });
}

function renderDriversTable() {
    const el = document.getElementById('drivers-table-body');
    if (!el) return;
    if (!driversData.length) { el.innerHTML = '<tr><td colspan="5" class="p-8 text-center text-slate-500">No drivers registered yet</td></tr>'; return; }
    el.innerHTML = driversData.map(d => `<tr class="hover:bg-slate-800/50">
        <td class="p-4 font-bold">${d.full_name}</td>
        <td class="p-4 font-mono">${d.plate_number}</td>
        <td class="p-4">${d.phone}</td>
        <td class="p-4"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${d.is_online ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-700 text-slate-400'}">${d.is_online ? 'Online' : 'Offline'}</span></td>
        <td class="p-4 text-right"><button onclick="toggleDriver('${d.uid}')" class="text-[10px] bg-slate-700 hover:bg-slate-600 px-3 py-1 rounded-lg text-white font-bold">Toggle</button></td>
    </tr>`).join('');
}

function renderRidersTable() {
    const el = document.getElementById('riders-table-body');
    if (!el) return;
    if (!ridersData.length) { el.innerHTML = '<tr><td colspan="5" class="p-8 text-center text-slate-500">No riders registered yet</td></tr>'; return; }
    el.innerHTML = ridersData.map(r => `<tr class="hover:bg-slate-800/50">
        <td class="p-4 font-bold">${r.full_name}</td>
        <td class="p-4">${r.phone}</td>
        <td class="p-4 font-extrabold text-emerald-400">UGX ${parseFloat(r.wallet_balance || 0).toLocaleString()}</td>
        <td class="p-4"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${r.status === 'Active' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-rose-500/10 text-rose-400'}">${r.status || 'Active'}</span></td>
        <td class="p-4 text-right"><button onclick="creditRider('${r.uid}')" class="text-[10px] bg-slate-700 hover:bg-slate-600 px-3 py-1 rounded-lg text-white font-bold">Credit</button></td>
    </tr>`).join('');
}

function renderTripsTable() {
    const el = document.getElementById('trips-table-body');
    if (!el) return;
    if (!tripsData.length) { el.innerHTML = '<tr><td colspan="6" class="p-8 text-center text-slate-500">No trips yet</td></tr>'; return; }
    el.innerHTML = tripsData.map(t => `<tr class="hover:bg-slate-800/50">
        <td class="p-4 font-mono font-bold text-amber-500">${t.trip_code}</td>
        <td class="p-4">${t.passenger_name || 'N/A'}</td>
        <td class="p-4">${t.driver_name || 'Unassigned'}</td>
        <td class="p-4 text-[11px]">${t.pickup_name} → ${t.dropoff_name}</td>
        <td class="p-4 font-extrabold text-emerald-400">UGX ${parseFloat(t.fare || 0).toLocaleString()}</td>
        <td class="p-4"><span class="px-2 py-0.5 rounded-full text-[10px] font-bold ${t.status === 'completed' ? 'bg-emerald-500/10 text-emerald-400' : t.status === 'matching' ? 'bg-sky-500/10 text-sky-400' : 'bg-amber-500/10 text-amber-400'}">${t.status}</span></td>
    </tr>`).join('');
}

// ---- ACTIONS ----
async function toggleDriver(uid) {
    try {
        await fetch(`${API_BASE}/api/admin/drivers/${uid}/toggle-status`, { method: 'POST' });
        loadProductionData();
    } catch (e) { logConsole('Toggle failed', 'error'); }
}

async function creditRider(uid) {
    const amt = prompt('Credit amount (UGX):');
    if (!amt) return;
    try {
        await fetch(`${API_BASE}/api/admin/riders/${uid}/credit`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: parseFloat(amt) })
        });
        loadProductionData();
    } catch (e) { logConsole('Credit failed', 'error'); }
}

// ---- PROMOS ----
async function loadPromoCampaigns() {
    try {
        const r = await fetch(`${API_BASE}/api/admin/promos`);
        if (r.ok) renderPromoList(await r.json());
    } catch (e) {}
}

function renderPromoList(promos) {
    const el = document.getElementById('promo-list');
    if (!el) return;
    el.innerHTML = promos.map(p => `<div class="flex justify-between items-center bg-slate-950/40 p-2 rounded border border-slate-800/40 text-[11px] mt-1">
        <span class="font-bold text-amber-500">${p.code}</span>
        <span class="text-slate-400">${p.discount_type === 'percent' ? p.value + '% Off' : 'UGX ' + parseInt(p.value).toLocaleString()}</span>
    </div>`).join('');
}

async function createNewPromo() {
    const code = document.getElementById('promo-code').value.trim().toUpperCase();
    const value = document.getElementById('promo-value').value.trim();
    const type = document.getElementById('promo-type').value;
    if (!code || !value) return alert('Fill in code and value');
    try {
        await fetch(`${API_BASE}/api/admin/promos`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code, discount_type: type, value })
        });
        document.getElementById('promo-code').value = '';
        document.getElementById('promo-value').value = '';
        loadPromoCampaigns();
        logConsole(`Promo deployed: ${code}`, 'system');
    } catch (e) { logConsole('Promo deploy failed', 'error'); }
}

// ---- SURGE ----
function updateSurgeLabel(v) { document.getElementById('live-surge-lbl').innerText = `${parseFloat(v).toFixed(1)}x`; }

async function applySurgePricing() {
    const m = document.getElementById('surge-slider').value;
    const r = document.getElementById('surge-reason').value;
    try {
        await fetch(`${API_BASE}/api/admin/pricing`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ multiplier: m, reason: r })
        });
        logConsole(`Surge updated: ${m}x [${r}]`, 'system');
    } catch (e) { logConsole('Surge update failed', 'error'); }
}

async function applySettingsSurge() {
    const m = document.getElementById('settings-surge-slider').value;
    const r = document.getElementById('settings-surge-reason').value;
    try {
        await fetch(`${API_BASE}/api/admin/pricing`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ multiplier: m, reason: r })
        });
        logConsole(`Settings surge: ${m}x`, 'system');
    } catch (e) { logConsole('Settings surge failed', 'error'); }
}

async function saveCorePricing() {
    const base = document.getElementById('base-fare-input').value;
    const km = document.getElementById('rate-km-input').value;
    const min = document.getElementById('rate-min-input').value;
    logConsole(`Pricing updated: Base=${base} KM=${km} Min=${min}`, 'system');
}

// ---- SOS ----
async function checkSOSAlerts() {
    try {
        const r = await fetch(`${API_BASE}/api/admin/sos`);
        if (!r.ok) return;
        const alerts = await r.json();
        const active = alerts.find(a => a.status === 'unresolved');
        if (active) {
            currentSOSAlert = active;
            document.getElementById('sos-alert-box').classList.remove('hidden');
            document.getElementById('sos-details').innerHTML = `<p><strong>Alert:</strong> ${active.id}</p><p><strong>Trip:</strong> ${active.trip_code}</p><p>${active.description}</p>`;
            setText('dash-sos-count', '1 Crisis');
            document.getElementById('dash-sos-container').innerHTML = `<div class="bg-rose-950/20 border border-rose-500/25 rounded-xl p-3 animate-pulse"><p class="text-xs font-bold text-rose-400">${active.id}</p><p class="text-[11px] text-rose-200">${active.description}</p></div>`;
            const btn = document.getElementById('dash-sos-resolve-btn');
            btn.classList.remove('hidden');
            logConsole(`SOS: ${active.id}`, 'error');
        } else {
            document.getElementById('sos-alert-box').classList.add('hidden');
            setText('dash-sos-count', '0 Crises');
            document.getElementById('dash-sos-container').innerHTML = '<div class="bg-emerald-950/20 border border-emerald-500/20 rounded-xl p-4 text-center"><div class="text-base">🛡️</div><p class="text-xs font-bold text-emerald-400">System Secure</p></div>';
            document.getElementById('dash-sos-resolve-btn').classList.add('hidden');
        }
    } catch (e) {}
}

async function resolveActiveSOS() {
    if (!currentSOSAlert) return;
    try {
        await fetch(`${API_BASE}/api/admin/sos/${currentSOSAlert.id}/resolve`, { method: 'POST' });
        logConsole(`Resolved: ${currentSOSAlert.id}`, 'system');
        currentSOSAlert = null;
        checkSOSAlerts();
    } catch (e) { logConsole('Resolve failed', 'error'); }
}

function resolveDashboardSOS() { resolveActiveSOS(); }

// ---- TABS ----
function switchTab(tabName) {
    document.querySelectorAll('[id^="tab-content-"]').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('[id^="nav-"]').forEach(el => { el.className = el.className.replace('bg-amber-500 text-slate-950', 'text-slate-400 hover:bg-slate-900 hover:text-white'); });
    const target = document.getElementById('tab-content-' + tabName.replace('-tab', ''));
    const nav = document.getElementById('nav-' + tabName);
    if (target) target.classList.remove('hidden');
    if (nav) nav.className = nav.className.replace('text-slate-400 hover:bg-slate-900 hover:text-white', 'bg-amber-500 text-slate-950 shadow-lg shadow-amber-500/10');
    if (map) setTimeout(() => map.invalidateSize(), 100);
}
