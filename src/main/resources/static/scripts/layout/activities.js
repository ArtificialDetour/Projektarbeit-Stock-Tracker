window.LayoutActivities = (function () {
    const iconMap = {
        'TRADE':  { icon: 'shopping_cart', cls: 'bg-primary-container/10 text-blue-800' },
        'EXPORT': { icon: 'download',      cls: 'bg-tertiary-fixed/30 text-on-tertiary-fixed' },
        'ALERT':  { icon: 'warning',       cls: 'bg-error-container text-error' },
        'SYSTEM': { icon: 'sync_alt',      cls: 'bg-secondary-container text-on-secondary-container' }
    };

    function escHtml(value) {
        // Activity text comes from persisted backend data and must remain plain text.
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Render activity list.
    function renderActivities(activities, listEl) {
        if (!listEl) return;
        if (!activities || !activities.length) {
            listEl.innerHTML = '<p class="p-6 text-sm text-on-surface-variant">No recent activities.</p>';
            return;
        }
        listEl.innerHTML = activities.map(function (a, idx) {
            const isLast = idx === activities.length - 1;
            const cfg = iconMap[a.type] || { icon: 'history', cls: 'bg-surface-container-high text-on-surface-variant' };
            return '<div class="p-6 bg-white dark:bg-slate-900 hover:bg-slate-50/80 transition-colors' + (isLast ? '' : ' border-b border-slate-100 dark:border-slate-700') + '">'
                + '<div class="flex gap-4">'
                + '<div class="flex-shrink-0 w-10 h-10 rounded-full ' + cfg.cls + ' flex items-center justify-center">'
                + '<span class="material-symbols-outlined text-xl">' + cfg.icon + '</span>'
                + '</div>'
                + '<div class="flex-1 space-y-1">'
                + '<div class="flex justify-between items-start">'
                + '<p class="text-sm font-bold text-slate-900 dark:text-slate-50">' + escHtml(a.title) + '</p>'
                + '<span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">' + escHtml(a.timeLabel) + '</span>'
                + '</div>'
                + '<p class="text-sm text-on-surface-variant leading-relaxed">' + escHtml(a.description) + '</p>'
                + '</div></div></div>';
        }).join('');
    }

    // Load activity data.
    function loadActivities(listEl) {
        fetch('/api/activities')
            .then(r => r.ok ? r.json() : [])
            .then(data => renderActivities(data, listEl))
            .catch(() => {});
    }

    // Wire activity popover.
    function initActivityLog(mount) {
        if (!mount) return;

        const trigger = mount.querySelector('#activity-trigger');
        const popover = mount.querySelector('#activity-popover');
        const overlay = mount.querySelector('#activity-overlay');
        const listEl = mount.querySelector('#activity-list');

        if (!trigger || !popover || !overlay) return;

        let isOpen = false;

        function togglePopover() {
            isOpen = !isOpen;
            if (isOpen) {
                loadActivities(listEl);
                popover.classList.remove('hidden');
                overlay.classList.remove('hidden');
                popover.style.animation = 'none';
                void popover.offsetWidth;
                popover.style.animation = '';
            } else {
                popover.classList.add('hidden');
                overlay.classList.add('hidden');
            }
        }

        function closePopover() {
            isOpen = false;
            popover.classList.add('hidden');
            overlay.classList.add('hidden');
        }

        trigger.addEventListener('click', togglePopover);
        overlay.addEventListener('click', closePopover);

        document.addEventListener('keydown', e => {
            if (e.key === 'Escape') closePopover();
        });

        document.addEventListener('click', e => {
            if (!trigger.contains(e.target) && !popover.contains(e.target)) {
                closePopover();
            }
        });
    }

    return {
        initActivityLog
    };
})();
