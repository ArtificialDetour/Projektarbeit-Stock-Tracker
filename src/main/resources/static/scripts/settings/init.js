(function () {
    // Bootstrap settings page.
    const toggles = window.SettingsToggles;
    const profile = window.SettingsProfile;
    const dataExport = window.SettingsExport;
    const deleteAccount = window.SettingsDeleteAccount;

    if (!toggles || !profile || !dataExport || !deleteAccount) {
        console.error('Settings modules not loaded correctly.');
        return;
    }

    toggles.initToggles();
    profile.initProfileEditor();
    dataExport.initDataExport();
    deleteAccount.initDeleteAccount();

    profile.fetchCurrentProfile()
        .then((currentProfile) => {
            profile.applyProfileInUi(currentProfile);
        })
        .catch((error) => {
            console.error('Failed to initialize settings page', error);
        });
})();
