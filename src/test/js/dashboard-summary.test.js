const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

describe('DashboardSummary', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript('src/main/resources/static/scripts/dashboard/utils.js');
    loadBrowserScript('src/main/resources/static/scripts/dashboard/summary.js');
  });

  test('calculates total return from holdings like portfolio page', async () => {
    document.body.innerHTML = `
      <div id="portfolio-total-value"></div>
      <div id="portfolio-total-return"></div>
      <div id="alloc-donut"></div>
      <div id="alloc-center-total"></div>
      <div id="alloc-stocks-pct"></div>
      <div id="alloc-crypto-pct"></div>
    `;
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([
        { symbol: 'AAPL', currentValue: '1303.10', costBasisTotal: '1000.00' },
        { symbol: 'MSFT', currentValue: '0.00', costBasisTotal: '0.00' }
      ])
    });

    window.DashboardSummary.loadPortfolioSummary();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(document.getElementById('portfolio-total-return').textContent).toBe('+30,31% Total Return');
  });
});
