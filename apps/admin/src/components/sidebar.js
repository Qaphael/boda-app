function switchTab(tabName) {
    document.querySelectorAll('[id^="tab-content-"]').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('[id^="nav-"]').forEach(el => { el.className = el.className.replace('bg-amber-500 text-slate-950', 'text-slate-400 hover:bg-slate-900 hover:text-white'); });
    const target = document.getElementById('tab-content-' + tabName.replace('-tab', ''));
    const nav = document.getElementById('nav-' + tabName);
    if (target) target.classList.remove('hidden');
    if (nav) nav.className = nav.className.replace('text-slate-400 hover:bg-slate-900 hover:text-white', 'bg-amber-500 text-slate-950 shadow-lg shadow-amber-500/10');
    if (map) setTimeout(() => map.invalidateSize(), 100);
}
