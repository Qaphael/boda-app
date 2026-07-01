// ---- PROMOS ----
async function loadPromoCampaigns() {
    try {
        const r = await fetch(`${API_BASE}/api/admin/promos`, { headers: getAdminHeaders() });
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
            method: 'POST', headers: getAdminHeaders(),
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
            method: 'POST', headers: getAdminHeaders(),
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
            method: 'POST', headers: getAdminHeaders(),
            body: JSON.stringify({ multiplier: m, reason: r })
        });
        logConsole(`Settings surge: ${m}x`, 'system');
    } catch (e) { logConsole('Settings surge update failed', 'error'); }
}

async function saveCorePricing() {
    const base = document.getElementById('base-fare-input').value;
    const km = document.getElementById('rate-km-input').value;
    const min = document.getElementById('rate-min-input').value;
    logConsole(`Pricing updated: Base=${base} KM=${km} Min=${min}`, 'system');
}
