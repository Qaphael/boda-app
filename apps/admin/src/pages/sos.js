// ---- SOS ----
async function checkSOSAlerts() {
    try {
        const r = await fetch(`${API_BASE}/api/admin/sos`, { headers: getAdminHeaders() });
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
        await fetch(`${API_BASE}/api/admin/sos/${currentSOSAlert.id}/resolve`, { method: 'POST', headers: getAdminHeaders() });
        logConsole(`Resolved: ${currentSOSAlert.id}`, 'system');
        currentSOSAlert = null;
        checkSOSAlerts();
    } catch (e) { logConsole('Resolve failed', 'error'); }
}

function resolveDashboardSOS() { resolveActiveSOS(); }
