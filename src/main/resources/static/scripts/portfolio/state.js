window.PortfolioState = (function () {
  var holdingsData = [];
  var prevValues = {};
  var prevPortfolioTotal = null;
  var activeCategory = 'ALL';
  var pollingStarted = false;
  var pendingReopenSymbol = null;
  var selectedSymbol = null;
  var sortState = { key: null, dir: 'asc' };

  var csrfToken = (document.querySelector('meta[name="_csrf"]') || {}).getAttribute
    ? document.querySelector('meta[name="_csrf"]').getAttribute('content')
    : '';
  var csrfHeader = (document.querySelector('meta[name="_csrf_header"]') || {}).getAttribute
    ? document.querySelector('meta[name="_csrf_header"]').getAttribute('content')
    : 'X-CSRF-TOKEN';

  function getHoldings() {
    return holdingsData;
  }

  function setHoldings(data) {
    holdingsData = data || [];
  }

  function findHolding(symbol) {
    return holdingsData.find(function (holding) {
      return holding.symbol === symbol;
    });
  }

  function getPrevValues() {
    return prevValues;
  }

  function getPrevPortfolioTotal() {
    return prevPortfolioTotal;
  }

  function setPrevPortfolioTotal(total) {
    prevPortfolioTotal = total;
  }

  function getActiveCategory() {
    return activeCategory;
  }

  function setActiveCategory(category) {
    activeCategory = category;
  }

  function hasPollingStarted() {
    return pollingStarted;
  }

  function setPollingStarted(started) {
    pollingStarted = started;
  }

  function getPendingReopenSymbol() {
    return pendingReopenSymbol;
  }

  function setPendingReopenSymbol(symbol) {
    pendingReopenSymbol = symbol;
  }

  function getSelectedSymbol() {
    return selectedSymbol;
  }

  function setSelectedSymbol(symbol) {
    selectedSymbol = symbol || null;
  }

  function getSortState() {
    return sortState;
  }

  function getCsrfHeaders() {
    var headers = { 'Content-Type': 'application/json' };
    headers[csrfHeader] = csrfToken;
    return headers;
  }

  return {
    getHoldings: getHoldings,
    setHoldings: setHoldings,
    findHolding: findHolding,
    getPrevValues: getPrevValues,
    getPrevPortfolioTotal: getPrevPortfolioTotal,
    setPrevPortfolioTotal: setPrevPortfolioTotal,
    getActiveCategory: getActiveCategory,
    setActiveCategory: setActiveCategory,
    hasPollingStarted: hasPollingStarted,
    setPollingStarted: setPollingStarted,
    getPendingReopenSymbol: getPendingReopenSymbol,
    setPendingReopenSymbol: setPendingReopenSymbol,
    getSelectedSymbol: getSelectedSymbol,
    setSelectedSymbol: setSelectedSymbol,
    getSortState: getSortState,
    getCsrfHeaders: getCsrfHeaders
  };
})();
