(function () {
    // Bootstrap shared layout.
    const theme = window.AppTheme;
    const auth = window.Auth;
    const notifications = window.LayoutNotifications;
    const activities = window.LayoutActivities;
    const navigation = window.LayoutNavigation;

    if (!theme || !auth || !notifications || !activities || !navigation) {
        console.error('Layout modules not loaded correctly.');
        return;
    }

    theme.initTheme();
    auth.loadUserProfile();

    const topnavMount = document.querySelector('.layout-topnav-slot');
    const sidebarMount = document.querySelector('.layout-sidebar-slot');

    if (topnavMount) {
        auth.wireLogoutButtons(topnavMount);
        notifications.initNotifications(topnavMount);
        activities.initActivityLog(topnavMount);
        navigation.initProfileMenu(topnavMount);
        navigation.initSearch(topnavMount);
    }

    if (sidebarMount || topnavMount) {
        navigation.initMobileSidebar();
    }
})();
