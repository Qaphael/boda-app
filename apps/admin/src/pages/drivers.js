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

async function toggleDriver(uid) {
    try {
        await fetch(`${API_BASE}/api/admin/drivers/${uid}/toggle-status`, { method: 'POST', headers: getAdminHeaders() });
        loadProductionData();
    } catch (e) { logConsole('Toggle failed', 'error'); }
}
