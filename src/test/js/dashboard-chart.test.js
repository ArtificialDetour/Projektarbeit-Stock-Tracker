const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

describe('DashboardChart', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript('src/main/resources/static/scripts/dashboard/utils.js');
    loadBrowserScript('src/main/resources/static/scripts/dashboard/chart.js');
  });

  function mountChartDom() {
    document.body.innerHTML = `
      <div id="portfolio-total-return">+30,31% Total Return</div>
      <div id="annualized-return"></div>
      <div id="time-period-label"></div>
      <div id="ecb-comparison-value"></div>
      <div id="ecb-rate-label"></div>
      <path id="chart-asset-path"></path>
      <path id="chart-ecb-path"></path>
      <line id="chart-zero-line"></line>
      <div id="chart-y-axis-left"></div>
      <svg><g id="chart-grid-lines"></g></svg>
      <span id="chart-label-0"></span>
      <span id="chart-label-1"></span>
      <span id="chart-label-2"></span>
      <span id="chart-label-3"></span>
      <span id="chart-label-4"></span>
    `;
  }

  test('uses backend total return without renormalizing the selected range', () => {
    mountChartDom();
    const history = [
      { timestamp: '2023-05-06T18:35:00', assetPct: 42.0, ecbPct: 0 },
      { timestamp: '2026-05-01T00:00:00', assetPct: 97.0, ecbPct: 10.03 }
    ];

    window.DashboardChart.renderChart(history, '5y');

    expect(document.getElementById('portfolio-total-return').textContent).toBe('+30,31% Total Return');
    expect(document.getElementById('annualized-return').textContent).toBe('+97,00%');
    expect(document.getElementById('time-period-label').textContent).toBe('5 Years');
  });
});
