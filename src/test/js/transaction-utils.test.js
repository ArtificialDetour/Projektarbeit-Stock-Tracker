const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

const utilsPath = 'src/main/resources/static/scripts/transaction/utils.js';

describe('TransactionUtils', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadBrowserScript(utilsPath);
  });

  test('escapes html-sensitive characters', () => {
    expect(window.TransactionUtils.esc('<Apple & "Co">')).toBe('&lt;Apple &amp; &quot;Co&quot;&gt;');
  });

  test('formats quantities and prices with German number separators', () => {
    expect(window.TransactionUtils.fmt(1234.5, 2)).toBe('1.234,50');
    expect(window.TransactionUtils.fmtQty('0.123456789')).toBe('0,123457');
  });

  test('builds searchable transaction text', () => {
    const text = window.TransactionUtils.buildSearchText({
      timestamp: '2026-05-01T10:15:00Z',
      assetName: 'Apple Inc',
      symbol: 'AAPL',
      transactionType: 'BUY',
      status: 'SETTLED'
    });

    expect(text).toContain('apple inc');
    expect(text).toContain('aapl');
    expect(text).toContain('buy');
    expect(text).toContain('settled');
    expect(text).toContain('may');
  });

  test('renders escaped row html with transaction type metadata', () => {
    const row = window.TransactionUtils.renderRow({
      timestamp: '2026-05-01T10:15:00Z',
      assetName: '<Bitcoin & Co>',
      symbol: 'BTC-USD',
      transactionType: 'BUY',
      status: 'SETTLED',
      quantity: 2,
      pricePerShare: 10000
    });

    expect(row).toContain('data-tx-type="BUY"');
    expect(row).toContain('&lt;Bitcoin &amp; Co&gt;');
    expect(row).not.toContain('<Bitcoin & Co>');
  });

  test('renders profit/loss only for sell transactions', () => {
    const sellProfit = window.TransactionUtils.profitLossCell({
      transactionType: 'SELL',
      realizedGain: 125.5
    });
    const sellLoss = window.TransactionUtils.profitLossCell({
      transactionType: 'SELL',
      realizedGain: -42
    });
    const buy = window.TransactionUtils.profitLossCell({
      transactionType: 'BUY',
      realizedGain: 125.5
    });

    expect(sellProfit).toContain('text-green-600');
    expect(sellProfit).toContain('+125,50');
    expect(sellLoss).toContain('text-red-600');
    expect(sellLoss).toContain('-42,00');
    expect(buy).toBe('');
  });
});
