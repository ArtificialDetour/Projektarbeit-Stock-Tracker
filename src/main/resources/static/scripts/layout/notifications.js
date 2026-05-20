window.LayoutNotifications = (function () {
    const iconClasses = {
        tertiary:  'bg-tertiary-fixed/30 text-on-tertiary-fixed',
        secondary: 'bg-secondary-container text-on-secondary-container',
        primary:   'bg-primary-container/10 text-blue-800'
    };

    function escHtml(value) {
        // Notification text comes from persisted backend data and must remain plain text.
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Update unread state.
    function applyUnread(count, mount) {
        const badge   = mount.querySelector('#notifications-badge');
        const countEl = mount.querySelector('#notifications-unread-count');
        const markReadBtn = mount.querySelector('#notifications-mark-read');
        
        if (badge)   { badge.classList.toggle('hidden', count === 0); }
        if (countEl) { countEl.textContent = count > 0 ? count + ' Alert' + (count > 1 ? 's' : '') : ''; }
        if (markReadBtn) { markReadBtn.classList.toggle('hidden', count === 0); }
    }

    // Render notification list.
    function renderNotifications(notifications, listEl, clearAllBtn) {
        if (!listEl) return;
        if (!notifications.length) {
            listEl.innerHTML = '<p class="p-6 text-sm text-on-surface-variant">No new notifications.</p>';
            return;
        }
        listEl.innerHTML = notifications.map(function (n, idx) {
            const isLast = idx === notifications.length - 1;
            const iconCls = iconClasses[n.iconStyle] || iconClasses.primary;
            const readOpacity = n.read ? 'opacity-60' : '';
            return '<div class="p-6 bg-white dark:bg-slate-900 hover:bg-slate-50/80 transition-colors ' + readOpacity + (isLast ? '' : ' border-b border-slate-100 dark:border-slate-700') + '">'
                + '<div class="flex gap-4">'
                + '<div class="flex-shrink-0 w-10 h-10 rounded-full ' + iconCls + ' flex items-center justify-center">'
                + '<span class="material-symbols-outlined text-xl">' + escHtml(n.icon || 'notifications') + '</span>'
                + '</div>'
                + '<div class="flex-1 space-y-1">'
                + '<div class="flex justify-between items-start">'
                + '<p class="text-sm font-bold text-slate-900 dark:text-slate-50">' + escHtml(n.title) + '</p>'
                + '<span class="text-[10px] text-on-surface-variant uppercase font-bold tracking-wider">' + escHtml(n.timeLabel) + '</span>'
                + '</div>'
                + '<p class="text-sm text-on-surface-variant leading-relaxed">' + escHtml(n.message) + '</p>'
                + '</div></div></div>';
        }).join('');
    }

    // Load notification data.
    function loadNotifications(mount, listEl, clearAllBtn) {
        return fetch('/api/notifications')
            .then(r => r.ok ? r.json() : null)
            .then(data => {
                if (data) {
                    renderNotifications(data, listEl, clearAllBtn);
                    const unread = data.filter(n => !n.read).length;
                    applyUnread(unread, mount);
                    if (clearAllBtn) {
                        clearAllBtn.classList.toggle('hidden', data.length === 0);
                    }
                }
            })
            .catch(() => {});
    }

    // Wire notification popover.
    function initNotifications(mount) {
        if (!mount) return;

        const trigger = mount.querySelector('#notifications-trigger');
        const popover = mount.querySelector('#notifications-popover');
        const overlay = mount.querySelector('#notifications-overlay');
        const listEl  = mount.querySelector('#notifications-list');
        const markReadBtn = mount.querySelector('#notifications-mark-read');
        const clearAllBtn = mount.querySelector('#notifications-clear-all');

        if (!trigger || !popover || !overlay) return;

        let isOpen = false;

        function togglePopover() {
            isOpen = !isOpen;
            popover.classList.toggle('hidden', !isOpen);
            overlay.classList.toggle('hidden', !isOpen);
        }

        function closePopover() {
            isOpen = false;
            popover.classList.add('hidden');
            overlay.classList.add('hidden');
        }

        trigger.addEventListener('click', togglePopover);

        loadNotifications(mount, listEl, clearAllBtn);

        if (markReadBtn && window.Auth) {
            markReadBtn.addEventListener('click', () => {
                fetch('/api/notifications/mark-read', { 
                    method: 'POST', 
                    headers: window.Auth.getCsrfHeaders() 
                })
                .then(() => {
                    applyUnread(0, mount);
                    if (listEl) {
                        listEl.querySelectorAll('div[class*="p-6"]').forEach(el => {
                            el.classList.add('opacity-60');
                        });
                    }
                })
                .catch(() => {});
            });
        }

        if (clearAllBtn && window.Auth) {
            clearAllBtn.addEventListener('click', () => {
                clearAllBtn.disabled = true;
                fetch('/api/notifications', { 
                    method: 'DELETE', 
                    headers: window.Auth.getCsrfHeaders() 
                })
                .then(r => {
                    if (!r.ok) {
                        throw new Error('Could not delete notifications');
                    }
                    applyUnread(0, mount);
                    if (listEl) {
                        listEl.innerHTML = '<p class="p-6 text-sm text-on-surface-variant">No new notifications.</p>';
                    }
                    clearAllBtn.classList.add('hidden');
                    return loadNotifications(mount, listEl, clearAllBtn);
                })
                .catch(() => {})
                .finally(() => {
                    clearAllBtn.disabled = false;
                });
            });
        }

        document.addEventListener('click', e => {
            if (!trigger.contains(e.target) && !popover.contains(e.target)) {
                closePopover();
            }
        });
    }

    return {
        initNotifications
    };
})();
