(function () {
  // Bootstrap transaction page.
  var utils = window.TransactionUtils;
  var state = window.TransactionState;
  var table = window.TransactionTable;
  var filters = window.TransactionFilters;
  var pagination = window.TransactionPagination;
  var txExport = window.TransactionExport;
  var data = window.TransactionData;

  if (!utils || !state || !table || !filters || !pagination || !txExport || !data) {
    console.error('Transaction modules not loaded correctly.');
    return;
  }

  var elements = {
    tbody: document.getElementById('transactions-tbody'),
    noResult: document.getElementById('tx-no-results'),
    countEl: document.getElementById('tx-count'),
    pagEl: document.getElementById('tx-pagination'),
    pageSizeEl: document.getElementById('tx-page-size'),
    dateFilterEl: document.getElementById('tx-date-filter'),
    filterBtns: document.querySelectorAll('[data-tx-filter]'),
    exportBtn: document.getElementById('tx-export-csv')
  };

  if (!elements.tbody) return;

  table.initTable(elements);
  pagination.initPagination(elements);
  filters.initFilters(elements);
  txExport.initExport(elements);
  data.loadTransactions();
})();
