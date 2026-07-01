function statsCard({ id, label, value, description, onClick, badge }) {
    return `<div ${onClick ? `onclick="${onClick}"` : ''} class="${onClick ? 'cursor-pointer' : ''} bg-slate-900 border border-slate-800/80 p-5 rounded-2xl hover:border-amber-500/50 transition">
        <div class="flex justify-between items-start">
            <span class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">${label}</span>
            ${badge ? `<span class="text-[10px] font-extrabold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full animate-pulse">${badge}</span>` : ''}
        </div>
        <p class="text-2xl font-black text-white mt-3" ${id ? `id="${id}"` : ''}>${value}</p>
        ${description ? `<p class="text-[10px] text-slate-400 mt-1">${description}</p>` : ''}
    </div>`;
}
