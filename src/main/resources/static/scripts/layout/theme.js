window.AppTheme = (function () {
    const THEME_STORAGE_KEY = 'app-theme';

    // Apply page theme.
    function applyTheme(isDarkMode) {
        document.documentElement.classList.toggle('dark', isDarkMode);
        document.documentElement.classList.toggle('light', !isDarkMode);
    }

    // Read stored theme.
    function getStoredTheme() {
        try {
            return localStorage.getItem(THEME_STORAGE_KEY);
        } catch (error) {
            return null;
        }
    }

    // Store theme choice.
    function setStoredTheme(theme) {
        try {
            localStorage.setItem(THEME_STORAGE_KEY, theme);
        } catch (error) {
            // Ignore storage errors
        }
    }

    function isDarkModeEnabled() {
        return document.documentElement.classList.contains('dark');
    }

    // Set dark mode.
    function setDarkMode(enabled) {
        applyTheme(Boolean(enabled));
        setStoredTheme(enabled ? 'dark' : 'light');
    }

    // Toggle dark mode.
    function toggleDarkMode() {
        setDarkMode(!isDarkModeEnabled());
        return isDarkModeEnabled();
    }

    // Initialize page theme.
    function initTheme() {
        const storedTheme = getStoredTheme();
        const useDarkMode = storedTheme === 'dark';
        applyTheme(useDarkMode);
    }

    return {
        isDarkMode: isDarkModeEnabled,
        setDarkMode: setDarkMode,
        toggleDarkMode: toggleDarkMode,
        initTheme: initTheme
    };
})();
