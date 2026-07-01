// SafeBoda Gulu - Admin Dashboard Core

const GULU_LAT_LON = [2.7712, 32.2985];
const API_BASE = window.location.origin;
let map, driverMarkers = {};
let driversData = [], ridersData = [], tripsData = [];
let currentSOSAlert = null;

function getAdminHeaders() {
    const key = localStorage.getItem('admin_secret_key') || '';
    return { 'Content-Type': 'application/json', 'x-admin-key': key };
}

async function attemptLogin() {
    const key = document.getElementById('login-key-input').value.trim();
    if (!key) return;
    try {
        const res = await fetch(`${API_BASE}/api/admin/stats`, {
            headers: { 'Content-Type': 'application/json', 'x-admin-key': key }
        });
        if (res.ok) {
            localStorage.setItem('admin_secret_key', key);
            document.getElementById('login-overlay').classList.add('hidden');
            document.getElementById('login-error').classList.add('hidden');
            bootDashboard();
        } else {
            document.getElementById('login-error').classList.remove('hidden');
        }
    } catch (e) {
        document.getElementById('login-error').classList.remove('hidden');
    }
}

document.getElementById('login-key-input')?.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') attemptLogin();
});

function bootDashboard() {
    const saved = localStorage.getItem('admin_secret_key') || '';
    const input = document.getElementById('admin-key-input');
    if (input && saved) input.value = saved;
    initMap();
    startClock();
    loadProductionData();
    loadPromoCampaigns();
    setInterval(loadProductionData, 10000);
    setInterval(checkSOSAlerts, 5000);
}

document.addEventListener('DOMContentLoaded', () => {
    const saved = localStorage.getItem('admin_secret_key') || '';
    if (saved) {
        attemptLogin();
    }
});

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

function setText(id, val) { const e = document.getElementById(id); if (e) e.innerText = val; }

async function loadProductionData() {
    try {
        const r = await fetch(`${API_BASE}/api/admin/stats`, { headers: getAdminHeaders() });
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

    try {
        const r = await fetch(`${API_BASE}/api/admin/drivers`, { headers: getAdminHeaders() });
        if (r.ok) {
            driversData = await r.json();
            renderDriversTable();
            updateMapMarkers();
        }
    } catch (e) {}

    try {
        const r = await fetch(`${API_BASE}/api/admin/riders`, { headers: getAdminHeaders() });
        if (r.ok) { ridersData = await r.json(); renderRidersTable(); }
    } catch (e) {}

    try {
        const r = await fetch(`${API_BASE}/api/admin/trips`, { headers: getAdminHeaders() });
        if (r.ok) {
            tripsData = await r.json();
            renderTripsTable();
            renderDispatchList(tripsData.filter(t => ['matching', 'en_route', 'active'].includes(t.status)));
        }
    } catch (e) {}

    try {
        const r = await fetch(`${API_BASE}/api/admin/active-trips`, { headers: getAdminHeaders() });
        if (r.ok) {
            const active = await r.json();
            renderDispatchList(active);
            setText('dispatch-count', `${active.length} Active`);
        }
    } catch (e) {}
}
