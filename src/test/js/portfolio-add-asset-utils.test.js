const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

describe('PortfolioUtils', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript('src/main/resources/static/scripts/portfolio/utils.js');
  });

  test('formats values and detects crypto symbols', () => {
    expect(window.PortfolioUtils.fmt(1234.5, 2)).toBe('1.234,50');
    expect(window.PortfolioUtils.fmtQty('1.23456789')).toBe('1,234568');
    expect(window.PortfolioUtils.isCrypto('BTC-USD')).toBe(true);
    expect(window.PortfolioUtils.isCrypto('AAPL')).toBe(false);
  });

  test('escapes csv cells when needed', () => {
    expect(window.PortfolioUtils.csvCell('plain')).toBe('plain');
    expect(window.PortfolioUtils.csvCell('Apple, Inc')).toBe('"Apple, Inc"');
    expect(window.PortfolioUtils.csvCell('He said "buy"')).toBe('"He said ""buy"""');
  });
});

describe('AddAssetUtils', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript('src/main/resources/static/scripts/add-asset/utils.js');
  });

  test('formats currency and escapes html', () => {
    expect(window.AddAssetUtils.formatCurrency(1234.5)).toBe('1.234,50');
    expect(window.AddAssetUtils.escHtml('<BTC & "USD">')).toBe('&lt;BTC &amp; &quot;USD&quot;&gt;');
  });

  test('creates csrf headers with and without meta tags', () => {
    expect(window.AddAssetUtils.getCsrfHeaders()).toEqual({ 'Content-Type': 'application/json' });

    document.head.innerHTML = `
      <meta name="_csrf" content="csrf-token">
      <meta name="_csrf_header" content="X-CSRF-TOKEN">
    `;

    expect(window.AddAssetUtils.getCsrfHeaders()).toEqual({
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': 'csrf-token'
    });
  });
});
