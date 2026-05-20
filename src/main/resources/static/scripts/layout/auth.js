window.Auth = (function () {
    let userProfileCache = null;

    function getCsrfToken() {
        const csrfMeta = document.querySelector('meta[name="_csrf"]');
        return csrfMeta ? csrfMeta.getAttribute('content') || '' : '';
    }

    function getCsrfHeaderName() {
        const hdr = document.querySelector('meta[name="_csrf_header"]');
        return hdr ? hdr.getAttribute('content') || '' : '';
    }

    function getCsrfHeaders() {
        const token = getCsrfToken();
        const header = getCsrfHeaderName();
        if (!token || !header) return {};
        const h = {};
        h[header] = token;
        return h;
    }

    // Submit logout form.
    function submitLogout() {
        const csrfToken = getCsrfToken();
        if (!csrfToken) return;

        const logoutForm = document.createElement('form');
        logoutForm.method = 'post';
        logoutForm.action = '/logout';
        logoutForm.style.display = 'none';

        const csrfInput = document.createElement('input');
        csrfInput.type = 'hidden';
        csrfInput.name = '_csrf';
        csrfInput.value = csrfToken;

        logoutForm.appendChild(csrfInput);
        document.body.appendChild(logoutForm);
        logoutForm.submit();
    }

    // Wire logout buttons.
    function wireLogoutButtons(mount) {
        if (!mount) return;
        const logoutButtons = mount.querySelectorAll('[data-logout-button]');
        logoutButtons.forEach(btn => {
            btn.addEventListener('click', e => {
                e.preventDefault();
                submitLogout();
            });
        });
    }

    // Resolve display name.
    function computeDisplayName(profile) {
        if (!profile) return 'Signed in User';
        if (profile.displayName && profile.displayName.trim()) {
            return profile.displayName.trim();
        }
        const firstName = profile.firstName ? profile.firstName.trim() : '';
        const lastName = profile.lastName ? profile.lastName.trim() : '';
        const fullName = (firstName + ' ' + lastName).trim();
        return fullName || profile.email || 'Signed in User';
    }

    // Apply profile labels.
    function applyUserProfile(profile, mount) {
        const root = mount || document;
        const displayName = computeDisplayName(profile);
        const email = (profile && profile.email) ? profile.email : 'user@example.com';

        root.querySelectorAll('[data-auth-user-name], [data-profile-name]').forEach(node => {
            node.textContent = displayName;
        });

        root.querySelectorAll('[data-auth-user-email], [data-profile-email]').forEach(node => {
            node.textContent = email;
        });
    }

    // Load authenticated user.
    async function loadUserProfile() {
        try {
            const response = await fetch('/api/auth/me', {
                headers: { 'Accept': 'application/json' },
                cache: 'no-store'
            });
            if (!response.ok) throw new Error('Unable to load user profile');
            const profile = await response.json();
            userProfileCache = profile || null;
            applyUserProfile(userProfileCache);

            if (userProfileCache && window.AppTheme) {
                const backendDarkMode = !!userProfileCache.darkMode;
                if (backendDarkMode !== window.AppTheme.isDarkMode()) {
                    window.AppTheme.setDarkMode(backendDarkMode);
                }
            }
        } catch {
            // Keep placeholders
        }
    }

    // Persist user settings.
    async function updateBackendSettings(settings) {
        try {
            const response = await fetch('/api/auth/settings', {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    ...getCsrfHeaders()
                },
                body: JSON.stringify(settings)
            });
            if (response.ok) {
                const updatedProfile = await response.json();
                userProfileCache = updatedProfile;
            }
        } catch (error) {
            console.error('Failed to update settings:', error);
        }
    }

    // Keep legacy global helpers available while newer modules use window.Auth directly.
    window.getUserProfile = () => userProfileCache;
    window.updateBackendSettings = updateBackendSettings;
    window.getCsrfHeaders = getCsrfHeaders;

    return {
        getCsrfToken,
        getCsrfHeaders,
        submitLogout,
        wireLogoutButtons,
        loadUserProfile,
        updateBackendSettings
    };
})();
