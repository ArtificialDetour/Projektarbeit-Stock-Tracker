(function () {
  // Bootstrap dashboard page.
  const utils = window.DashboardUtils;
  const summary = window.DashboardSummary;
  const chart = window.DashboardChart;
  const activities = window.DashboardActivities;
  const market = window.DashboardMarket;

  if (!utils || !summary || !chart || !activities || !market) {
    console.error('Dashboard modules not loaded correctly.');
    return;
  }

  // Load each dashboard panel once before enabling periodic refreshes.
  summary.loadPortfolioSummary();
  activities.loadDashboardActivities();
  chart.initChart();
  market.updateMarketQuotes();

  // Refresh high-level values often, while activity updates can be less frequent.
  setInterval(summary.loadPortfolioSummary, 30000);
  setInterval(activities.loadDashboardActivities, 60000);

  // Expose the activity refresh hook for layout-level modules.
  window.loadDashboardActivities = activities.loadDashboardActivities;

  // Time-range buttons request new chart data without reloading the page.
  const buttons = document.querySelectorAll('[data-time-range-group] [data-time-range]');
  buttons.forEach(function (btn) {
    btn.addEventListener('click', function () {
      const mapped = utils.rangeMap[btn.textContent.trim()] || '5y';
      if (mapped === '5y') {
        chart.initChart();
      } else {
        chart.fetchChartData(mapped);
      }
    });
  });

  // Keep the segmented control styling in sync with the selected range.
  const group = document.querySelector('[data-time-range-group]');
  if (group) {
    const btnList       = Array.from(group.querySelectorAll('[data-time-range]'));
    const activeClasses = ['bg-surface-container-lowest', 'shadow-sm', 'text-primary', 'rounded-md'];
    const inactiveClasses = ['text-on-surface-variant'];

    const setActiveButton = (activeButton) => {
      btnList.forEach((button) => {
        const isActive = button === activeButton;
        button.classList.toggle('hover:text-on-surface', !isActive);
        activeClasses.forEach((cls)   => button.classList.toggle(cls, isActive));
        inactiveClasses.forEach((cls) => button.classList.toggle(cls, !isActive));
      });
    };

    btnList.forEach((button) => {
      button.addEventListener('click', () => setActiveButton(button));
    });
  }
})();
