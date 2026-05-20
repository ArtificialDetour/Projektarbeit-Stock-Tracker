window.DashboardActivities = (function () {
  
  function escHtml(value) {
    // Activity text comes from persisted backend data and must remain plain text.
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Load dashboard activity preview.
  function loadDashboardActivities() {
    const listEl = document.getElementById('dashboard-activity-list');
    if (!listEl) return;

    const iconMap = {
      'TRADE':  { icon: 'shopping_cart', color: 'text-primary-container' },
      'EXPORT': { icon: 'download',      color: 'text-tertiary-fixed-dim' },
      'ALERT':  { icon: 'warning',       color: 'text-error' },
      'SYSTEM': { icon: 'sync_alt',      color: 'text-on-secondary-container' }
    };

    fetch('/api/activities')
      .then(function (r) { return r.ok ? r.json() : []; })
      .then(function (activities) {
        if (!activities || !activities.length) {
          listEl.innerHTML = '<p class="text-sm text-on-surface-variant">No recent activities.</p>';
          return;
        }
        listEl.innerHTML = activities.slice(0, 4).map(function (a) {
          const cfg = iconMap[a.type] || { icon: 'history', color: 'text-on-surface-variant' };
          return '<div class="flex gap-4">'
            + '<div class="w-10 h-10 rounded-lg bg-surface-container-high dark:bg-slate-800 flex items-center justify-center flex-shrink-0">'
            + '<span class="material-symbols-outlined ' + cfg.color + '">' + cfg.icon + '</span>'
            + '</div>'
            + '<div>'
            + '<p class="text-sm font-bold text-on-surface dark:text-slate-200">' + escHtml(a.title) + '</p>'
            + '<p class="text-xs text-on-surface-variant dark:text-slate-400">' + escHtml(a.description) + '</p>'
            + '<p class="text-[10px] font-bold text-on-surface-variant uppercase mt-1">' + escHtml(a.timeLabel) + '</p>'
            + '</div></div>';
        }).join('');
      })
      .catch(function () {});
  }

  return {
    loadDashboardActivities
  };
})();
