window.SettingsProfile = (function () {
    function displayNameFromProfile(profile) {
        const fullName = `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim();
        return profile.displayName || fullName || profile.email || 'Signed in User';
    }

    // Apply profile text.
    function applyProfileInUi(profile) {
        const displayName = displayNameFromProfile(profile);
        const email = profile.email || 'user@example.com';

        document.querySelectorAll('[data-auth-user-name]').forEach((el) => {
            el.textContent = displayName;
        });
        document.querySelectorAll('[data-auth-user-email]').forEach((el) => {
            el.textContent = email;
        });
        document.querySelectorAll('[data-profile-name]').forEach((el) => {
            el.textContent = displayName;
        });
        document.querySelectorAll('[data-profile-email]').forEach((el) => {
            el.textContent = email;
        });

        if (window.SettingsToggles) {
            window.SettingsToggles.applyProfileToggleState(profile);
        }
    }

    function setMessage(message, text, isError) {
        if (!message) return;

        message.textContent = text;
        message.classList.toggle('hidden', !text);
        message.classList.remove('text-error', 'text-on-tertiary-fixed-variant');
        if (text) {
            message.classList.add(isError ? 'text-error' : 'text-on-tertiary-fixed-variant');
        }
    }

    // Toggle profile form.
    function setEditMode(elements, enabled) {
        elements.editFields?.classList.toggle('hidden', !enabled);
        elements.editButton?.classList.toggle('hidden', enabled);
        elements.saveButton?.classList.toggle('hidden', !enabled);
        elements.cancelButton?.classList.toggle('hidden', !enabled);
    }

    // Load current profile.
    async function fetchCurrentProfile() {
        const response = await fetch('/api/auth/me', {
            headers: { Accept: 'application/json' },
            cache: 'no-store'
        });
        if (!response.ok) {
            throw new Error('Could not load profile');
        }
        return response.json();
    }

    // Wire profile editor.
    function initProfileEditor() {
        const elements = {
            editButton: document.getElementById('profile-edit-button'),
            saveButton: document.getElementById('profile-save-button'),
            cancelButton: document.getElementById('profile-cancel-button'),
            editFields: document.getElementById('profile-edit-fields'),
            firstNameInput: document.getElementById('profile-first-name'),
            lastNameInput: document.getElementById('profile-last-name'),
            message: document.getElementById('profile-update-message')
        };

        if (!elements.editButton || !elements.saveButton || !elements.cancelButton || !elements.firstNameInput || !elements.lastNameInput) {
            return;
        }

        elements.editButton.addEventListener('click', async () => {
            setMessage(elements.message, '');
            try {
                const profile = await fetchCurrentProfile();
                elements.firstNameInput.value = profile.firstName || '';
                elements.lastNameInput.value = profile.lastName || '';
                setEditMode(elements, true);
                elements.firstNameInput.focus();
            } catch {
                setMessage(elements.message, 'Profile could not be loaded. Please try again.', true);
            }
        });

        elements.cancelButton.addEventListener('click', () => {
            setEditMode(elements, false);
            setMessage(elements.message, '');
        });

        elements.saveButton.addEventListener('click', async () => {
            const firstName = elements.firstNameInput.value.trim();
            const lastName = elements.lastNameInput.value.trim();

            if (!firstName || !lastName) {
                setMessage(elements.message, 'First name and last name are required.', true);
                return;
            }

            elements.saveButton.disabled = true;
            elements.cancelButton.disabled = true;
            setMessage(elements.message, 'Saving...');

            try {
                const response = await fetch('/api/auth/profile', {
                    method: 'PATCH',
                    headers: {
                        'Content-Type': 'application/json',
                        Accept: 'application/json',
                        ...(window.getCsrfHeaders ? window.getCsrfHeaders() : {})
                    },
                    body: JSON.stringify({ firstName, lastName })
                });

                if (!response.ok) {
                    throw new Error('Could not update profile');
                }

                const updatedProfile = await response.json();
                applyProfileInUi(updatedProfile);
                setEditMode(elements, false);
                setMessage(elements.message, 'Profile updated successfully.');
            } catch {
                setMessage(elements.message, 'Profile update failed. Please try again.', true);
            } finally {
                elements.saveButton.disabled = false;
                elements.cancelButton.disabled = false;
            }
        });
    }

    return {
        initProfileEditor,
        fetchCurrentProfile,
        applyProfileInUi
    };
})();
