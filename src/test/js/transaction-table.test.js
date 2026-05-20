const { resetBrowserScripts, loadBrowserScript } = require('./helpers/browser-script-loader');

const scripts = [
  'src/main/resources/static/scripts/transaction/utils.js',
  'src/main/resources/static/scripts/transaction/state.js',
  'src/main/resources/static/scripts/transaction/pagination.js',
  'src/main/resources/static/scripts/transaction/table.js'
];

function loadTransactionTable() {
  scripts.forEach(loadBrowserScript);
  document.body.innerHTML = `
    <table>
      <tbody id="transactions-tbody">
        <tr id="no-results" class="hidden"></tr>
      </tbody>
    </table>
    <div id="transaction-count"></div>
    <div id="transaction-pagination"></div>
  `;
  window.TransactionTable.initTable({
    tbody: document.getElementById('transactions-tbody'),
    noResult: document.getElementById('no-results'),
    countEl: document.getElementById('transaction-count')
  });
  window.TransactionPagination.initPagination({
    pagEl: document.getElementById('transaction-pagination')
  });
}

const transactions = [
  {
    timestamp: '2026-05-01T10:00:00Z',
    assetName: 'Apple Inc',
    symbol: 'AAPL',
    transactionType: 'BUY',
    status: 'SETTLED',
    quantity: 1,
    pricePerShare: 200,
    totalCost: 200
  },
  {
    timestamp: '2026-05-02T10:00:00Z',
    assetName: 'Microsoft',
    symbol: 'MSFT',
    transactionType: 'SELL',
    status: 'SETTLED',
    quantity: 2,
    pricePerShare: 300,
    totalCost: 600,
    realizedGain: -50
  },
  {
    timestamp: '2026-05-03T10:00:00Z',
    assetName: 'Bitcoin',
    symbol: 'BTC-USD',
    transactionType: 'BUY',
    status: 'PENDING',
    quantity: 0.5,
    pricePerShare: 10000,
    totalCost: 5000
  }
];

describe('TransactionTable', () => {
  beforeEach(() => {
    resetBrowserScripts();
    loadTransactionTable();
    window.TransactionState.setAllData(transactions);
  });

  test('filters transactions by type', () => {
    window.TransactionState.setActiveType('BUY');

    expect(window.TransactionTable.getFiltered().map((tx) => tx.symbol)).toEqual(['AAPL', 'BTC-USD']);
  });

  test('filters transactions by search term', () => {
    window.TransactionState.setActiveSearchTerm('micro');

    expect(window.TransactionTable.getFiltered().map((tx) => tx.symbol)).toEqual(['MSFT']);
  });

  test('renders current page and pagination from filtered rows', () => {
    window.TransactionState.setPageSize(2);
    window.TransactionTable.renderPage();

    expect(document.querySelectorAll('tr[data-tx-type]')).toHaveLength(2);
    expect(document.getElementById('transaction-count').textContent).toBe('Showing 1-2 of 3 transactions');
    expect(document.querySelectorAll('#transaction-pagination [data-page]')).toHaveLength(2);

    window.TransactionPagination.goTo(2);

    expect(window.TransactionState.getCurrentPage()).toBe(2);
    expect(document.querySelectorAll('tr[data-tx-type]')).toHaveLength(1);
    expect(document.getElementById('transaction-count').textContent).toBe('Showing 3-3 of 3 transactions');
  });
});
