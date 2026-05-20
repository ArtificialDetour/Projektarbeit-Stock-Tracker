window.SettingsToggles = (function () {
    // Update toggle appearance.
    function setToggleVisualState(toggle, isActive) {
        if (!toggle) return;

        const knob = toggle.querySelector('div');
        toggle.classList.toggle('bg-tertiary-fixed', isActive);
        toggle.classList.toggle('bg-outline-variant', !isActive);

        if (knob) {
            knob.classList.toggle('translate-x-5', isActive);
        }
    }

    // Wire dark mode toggle.
    function initDarkModeToggle() {
        const darkModeToggle = document.querySelector('[data-toggle-dark-mode]');
        if (!darkModeToggle) return;

        const initialState = window.AppTheme && typeof window.AppTheme.isDarkMode === 'function'
            ? window.AppTheme.isDarkMode()
            : document.documentElement.classList.contains('dark');

        setToggleVisualState(darkModeToggle, initialState);

        darkModeToggle.addEventListener('click', () => {
            let nextState;

            if (window.AppTheme && typeof window.AppTheme.toggleDarkMode === 'function') {
                nextState = window.AppTheme.toggleDarkMode();
            } else {
                document.documentElement.classList.toggle('dark');
                document.documentElement.classList.toggle('light', !document.documentElement.classList.contains('dark'));
                nextState = document.documentElement.classList.contains('dark');
            }

            setToggleVisualState(darkModeToggle, nextState);
            if (window.updateBackendSettings) {
                window.updateBackendSettings({ darkMode: nextState });
            }
        });
    }

    // Wire notification toggles.
    function initNotificationToggles() {
        const priceAlertsToggle = document.querySelector('[data-toggle-notifications]');
        const transactionsToggle = document.querySelector('[data-toggle-transactions]');

        if (priceAlertsToggle) {
            priceAlertsToggle.addEventListener('click', () => {
                const isCurrentlyActive = priceAlertsToggle.classList.contains('bg-tertiary-fixed');
                const nextState = !isCurrentlyActive;
                setToggleVisualState(priceAlertsToggle, nextState);
                if (window.updateBackendSettings) {
                    window.updateBackendSettings({ priceAlerts: nextState });
                }
            });
        }

        if (transactionsToggle) {
            transactionsToggle.addEventListener('click', () => {
                const isCurrentlyActive = transactionsToggle.classList.contains('bg-tertiary-fixed');
                const nextState = !isCurrentlyActive;
                setToggleVisualState(transactionsToggle, nextState);
                if (window.updateBackendSettings) {
                    window.updateBackendSettings({ transactionUpdates: nextState });
                }
            });
        }
    }

    // Apply saved toggle state.
    function applyProfileToggleState(profile) {
        const priceAlertsToggle = document.querySelector('[data-toggle-notifications]');
        const transactionsToggle = document.querySelector('[data-toggle-transactions]');

        if (priceAlertsToggle && profile.priceAlerts !== undefined) {
            setToggleVisualState(priceAlertsToggle, !!profile.priceAlerts);
        }
        if (transactionsToggle && profile.transactionUpdates !== undefined) {
            setToggleVisualState(transactionsToggle, !!profile.transactionUpdates);
        }
    }

    // Initialize settings toggles.
    function initToggles() {
        initDarkModeToggle();
        initNotificationToggles();
    }

    return {
        initToggles,
        setToggleVisualState,
        applyProfileToggleState
    };
})();
