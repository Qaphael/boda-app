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

async function creditRider(uid) {
    const amt = prompt('Credit amount (UGX):');
    if (!amt) return;
    try {
        await fetch(`${API_BASE}/api/admin/riders/${uid}/credit`, {
            method: 'POST', headers: getAdminHeaders(),
            body: JSON.stringify({ amount: parseFloat(amt) })
        });
        loadProductionData();
    } catch (e) { logConsole('Credit failed', 'error'); }
}
