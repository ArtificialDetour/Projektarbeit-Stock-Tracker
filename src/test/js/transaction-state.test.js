const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

// Load the production browser module so the tests cover the same global API used by the page.
const statePath = 'src/main/resources/static/scripts/transaction/state.js';

describe('TransactionState', () => {
  test('starts with default ui state', () => {
    resetBrowserScripts();
    loadBrowserScript(statePath);

    expect(window.TransactionState.getAllData()).toEqual([]);
    expect(window.TransactionState.getActiveType()).toBe('ALL');
    expect(window.TransactionState.getActivePeriod()).toBe('all');
    expect(window.TransactionState.getCurrentPage()).toBe(1);
    expect(window.TransactionState.getPageSize()).toBe(50);
    expect(window.TransactionState.getActiveSearchTerm()).toBe('');
  });

  test('reads initial search term from query string', () => {
    // The transaction page normalizes incoming search terms before storing them in state.
    resetBrowserScripts('http://localhost/transactions?q=%20AAPL%20');
    loadBrowserScript(statePath);

    expect(window.TransactionState.getActiveSearchTerm()).toBe('aapl');
  });

  test('updates data and ui state through setters', () => {
    resetBrowserScripts();
    loadBrowserScript(statePath);

    const data = [{ symbol: 'AAPL' }];
    window.TransactionState.setAllData(data);
    window.TransactionState.setActiveType('BUY');
    window.TransactionState.setActivePeriod('30d');
    window.TransactionState.setCurrentPage(3);
    window.TransactionState.setPageSize(25);
    // Search terms are trimmed and lowercased to make filtering case-insensitive.
    window.TransactionState.setActiveSearchTerm('  BitCoin  ');

    expect(window.TransactionState.getAllData()).toBe(data);
    expect(window.TransactionState.getActiveType()).toBe('BUY');
    expect(window.TransactionState.getActivePeriod()).toBe('30d');
    expect(window.TransactionState.getCurrentPage()).toBe(3);
    expect(window.TransactionState.getPageSize()).toBe(25);
    expect(window.TransactionState.getActiveSearchTerm()).toBe('bitcoin');
  });
});
