window.LayoutNavigation = (function () {

    // Wire profile menu.
    function initProfileMenu(mount) {
        if (!mount) return;
        const trigger = mount.querySelector('#profile-trigger');
        const popover = mount.querySelector('#profile-popover');
        const overlay = mount.querySelector('#profile-overlay');
        if (!trigger || !popover || !overlay) return;

        let isOpen = false;
        function toggle() {
            isOpen = !isOpen;
            popover.classList.toggle('hidden', !isOpen);
            overlay.classList.toggle('hidden', !isOpen);
        }
        function close() {
            isOpen = false;
            popover.classList.add('hidden');
            overlay.classList.add('hidden');
        }

        trigger.addEventListener('click', toggle);
        overlay.addEventListener('click', close);
        document.addEventListener('keydown', e => { if (e.key === 'Escape') close(); });
        document.addEventListener('click', e => {
            if (!trigger.contains(e.target) && !popover.contains(e.target)) close();
        });
    }

    // Wire mobile sidebar.
    function initMobileSidebar() {
        const sidebarMount = document.querySelector('.layout-sidebar-slot');
        const topnavMount = document.querySelector('.layout-topnav-slot');
        if (!sidebarMount || !topnavMount) return;

        const toggleButton = topnavMount.querySelector('#mobile-sidebar-toggle');
        const closeButton = sidebarMount.querySelector('#mobile-sidebar-close');
        if (!toggleButton) return;

        let backdrop = document.querySelector('.mobile-sidebar-backdrop');
        if (!backdrop) {
            backdrop = document.createElement('button');
            backdrop.type = 'button';
            backdrop.className = 'mobile-sidebar-backdrop';
            backdrop.setAttribute('aria-label', 'Close navigation menu');
            backdrop.setAttribute('aria-hidden', 'true');
            document.body.appendChild(backdrop);
        }

        const isDesktop = () => window.matchMedia('(min-width: 1024px)').matches;

        // Toggle sidebar state.
        function setSidebarOpen(open) {
            if (isDesktop()) {
                document.body.classList.remove('sidebar-open');
                toggleButton.setAttribute('aria-expanded', 'false');
                return;
            }
            const shouldOpen = Boolean(open);
            document.body.classList.toggle('sidebar-open', shouldOpen);
            toggleButton.setAttribute('aria-expanded', shouldOpen ? 'true' : 'false');
        }

        toggleButton.addEventListener('click', () => {
            const currentlyOpen = document.body.classList.contains('sidebar-open');
            setSidebarOpen(!currentlyOpen);
        });

        if (closeButton) closeButton.addEventListener('click', () => setSidebarOpen(false));

        sidebarMount.addEventListener('click', e => {
            if (e.target && e.target.closest('.sidebar-link')) setSidebarOpen(false);
        });

        backdrop.addEventListener('click', () => setSidebarOpen(false));
        window.addEventListener('resize', () => { if (isDesktop()) setSidebarOpen(false); });
        document.addEventListener('keydown', e => { if (e.key === 'Escape') setSidebarOpen(false); });
    }

    // Wire shared search.
    function initSearch(mount) {
        const input = mount.querySelector('#topnav-search');
        if (!input) return;

        const isTransactionPage = window.location.pathname === '/transaction';

        if (isTransactionPage) {
            // Preserve search handoff from other pages and return focus to the shared field.
            const params = new URLSearchParams(window.location.search);
            const q = params.get('q');
            if (q) input.value = q;
            if (params.get('focus') === 'true') {
                setTimeout(() => {
                    input.focus();
                    const len = input.value.length;
                    input.setSelectionRange(len, len);
                }, 10);
            }
        }

        input.addEventListener('input', () => {
            if (!isTransactionPage) {
                const term = input.value.trim();
                if (term) {
                    window.location.href = '/transaction?q=' + encodeURIComponent(term) + '&focus=true';
                }
            }
        });

        input.addEventListener('keydown', e => {
            if (e.key !== 'Enter') return;
            const term = input.value.trim();
            if (!isTransactionPage) {
                window.location.href = '/transaction' + (term ? '?q=' + encodeURIComponent(term) : '');
            }
        });
    }

    return {
        initProfileMenu,
        initMobileSidebar,
        initSearch
    };
})();
