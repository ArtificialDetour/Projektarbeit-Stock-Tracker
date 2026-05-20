(function () {
  // Bootstrap portfolio page.
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;
  var allocation = window.PortfolioAllocation;
  var insights = window.PortfolioInsights;
  var table = window.PortfolioTable;
  var filters = window.PortfolioFilters;
  var sort = window.PortfolioSort;
  var history = window.PortfolioHistory;
  var sell = window.PortfolioSell;
  var data = window.PortfolioData;
  var chart = window.PortfolioChart;
  var csvExport = window.PortfolioExport;

  if (!utils || !state || !allocation || !insights || !table || !filters || !sort || !history || !sell || !data || !chart || !csvExport) {
    console.error('Portfolio modules not loaded correctly.');
    return;
  }

  var elements = {
    tbody: document.getElementById('holdings-tbody'),
    emptyRow: document.getElementById('holdings-empty'),
    totalValueEl: document.getElementById('portfolio-total-value'),
    totalReturnEl: document.getElementById('portfolio-total-return'),
    totalBuyInEl: document.getElementById('holdings-total-buyin'),
    totalCurrentEl: document.getElementById('holdings-total-value'),
    tableTitleEl: document.getElementById('portfolio-table-title'),
    backHoldingsBtn: document.getElementById('portfolio-back-holdings'),
    filterControlsEl: document.getElementById('portfolio-filter-controls'),
    colPrimaryEl: document.getElementById('portfolio-col-primary'),
    colDateEl: document.getElementById('portfolio-col-date'),
    colQuantityEl: document.getElementById('portfolio-col-quantity'),
    colPriceEl: document.getElementById('portfolio-col-price'),
    colBuyInEl: document.getElementById('portfolio-col-buyin'),
    colValueEl: document.getElementById('portfolio-col-value'),
    colPerformanceEl: document.getElementById('portfolio-col-performance'),
    colActionsEl: document.getElementById('portfolio-col-actions'),
    filterBtns: document.querySelectorAll('[data-portfolio-filter]'),
    exportBtn: document.getElementById('portfolio-export-csv'),
    historyOverlay: document.getElementById('holding-history-overlay'),
    historyModal: document.getElementById('holding-history-modal'),
    historyCloseButton: document.getElementById('holding-history-close'),
    historyTitleEl: document.getElementById('holding-history-title'),
    historySubtitleEl: document.getElementById('holding-history-subtitle'),
    historyCountEl: document.getElementById('holding-history-count'),
    historyBody: document.getElementById('holding-history-body'),
    sellModal: document.getElementById('sell-asset-modal'),
    sellBackdrop: document.getElementById('sell-asset-backdrop'),
    sellCloseBtn: document.getElementById('sell-asset-close'),
    sellNameEl: document.getElementById('sell-asset-name'),
    sellBadgeEl: document.getElementById('sell-asset-symbol-badge'),
    sellPriceEl: document.getElementById('sell-asset-price'),
    sellHoldingEl: document.getElementById('sell-asset-holding'),
    sellQtyInput: document.getElementById('sell-asset-qty'),
    sellTotalEl: document.getElementById('sell-asset-total'),
    sellErrorEl: document.getElementById('sell-asset-error'),
    sellConfirmBtn: document.getElementById('sell-asset-confirm'),
    sellTitleEl: document.getElementById('sell-asset-title'),
    sellQtyWrapper: document.getElementById('sell-asset-qty-wrapper')
  };

  if (!elements.tbody) return;

  table.initTable(elements);
  filters.initFilters(elements);
  sort.initSort();
  history.initHistory(elements);
  sell.initSell(elements);
  csvExport.initExport(elements);

  data.loadHoldings();
  chart.loadChart();
})();
