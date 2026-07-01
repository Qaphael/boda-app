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
