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
