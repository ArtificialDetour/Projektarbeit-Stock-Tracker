const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

describe('PortfolioChart', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript('src/main/resources/static/scripts/portfolio/chart.js');
  });

  afterEach(() => {
    delete global.fetch;
  });

  test('uses backend return values without renormalizing the chart range', () => {
    document.body.innerHTML = `
      <path id="pf-chart-asset-path"></path>
      <path id="pf-chart-inflation-path"></path>
      <path id="pf-chart-cumInfl-path"></path>
      <line id="pf-chart-zero-line"></line>
      <div id="pf-chart-y-axis-left"></div>
      <svg><g id="pf-chart-grid-lines"></g></svg>
      <span id="pf-chart-label-0"></span>
      <span id="pf-chart-label-1"></span>
      <span id="pf-chart-label-2"></span>
      <span id="pf-chart-label-3"></span>
      <span id="pf-chart-label-4"></span>
    `;

    window.PortfolioChart.renderChart([
      { timestamp: '2023-05-06T18:35:00', assetPct: 42.0, realReturnPct: 40.0, inflationPct: 0 },
      { timestamp: '2026-05-01T00:00:00', assetPct: 97.0, realReturnPct: 91.0, inflationPct: 6 }
    ]);

    expect(document.getElementById('pf-chart-asset-path').getAttribute('d')).toBe('M0,166 L1000,56');
  });

  test('loads selected holding chart without using browser cache', async () => {
    document.body.innerHTML = `
      <path id="pf-chart-asset-path"></path>
      <path id="pf-chart-inflation-path"></path>
      <path id="pf-chart-cumInfl-path"></path>
      <line id="pf-chart-zero-line"></line>
      <div id="pf-chart-y-axis-left"></div>
      <svg><g id="pf-chart-grid-lines"></g></svg>
      <span id="pf-chart-label-0"></span>
      <span id="pf-chart-label-1"></span>
      <span id="pf-chart-label-2"></span>
      <span id="pf-chart-label-3"></span>
      <span id="pf-chart-label-4"></span>
    `;
    const history = [
      { timestamp: '2026-01-01T00:00:00', assetPct: 0, realReturnPct: 0, inflationPct: 0 },
      { timestamp: '2026-05-01T00:00:00', assetPct: 12, realReturnPct: 10, inflationPct: 2 }
    ];
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue(history)
    });

    await window.PortfolioChart.loadChart('BTC-USD');

    expect(global.fetch).toHaveBeenCalledWith(
      '/api/assets/portfolio-chart?range=5y&symbol=BTC-USD',
      { cache: 'no-store' }
    );
    expect(document.getElementById('pf-chart-asset-path').getAttribute('d')).toBeTruthy();
  });
});
