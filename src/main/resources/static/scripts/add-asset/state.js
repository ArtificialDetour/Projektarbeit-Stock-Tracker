window.AddAssetState = (function () {
  let selectedSymbol = null;
  let selectedName = '';
  let currentPrice = 0;
  let currentCurrency = 'USD';
  let searchTimer = null;

  // Reset modal state.
  function reset() {
    selectedSymbol = null;
    selectedName = '';
    currentPrice = 0;
    currentCurrency = 'USD';
    searchTimer = null;
  }

  function getSelectedSymbol() {
    return selectedSymbol;
  }

  function setSelectedSymbol(symbol) {
    selectedSymbol = symbol;
  }

  function getSelectedName() {
    return selectedName;
  }

  function setSelectedName(name) {
    selectedName = name;
  }

  function getCurrentPrice() {
    return currentPrice;
  }

  function setCurrentPrice(price) {
    currentPrice = price;
  }

  function getCurrentCurrency() {
    return currentCurrency;
  }

  function setCurrentCurrency(currency) {
    currentCurrency = currency;
  }

  function getSearchTimer() {
    return searchTimer;
  }

  function setSearchTimer(timer) {
    searchTimer = timer;
  }

  return {
    reset,
    getSelectedSymbol,
    setSelectedSymbol,
    getSelectedName,
    setSelectedName,
    getCurrentPrice,
    setCurrentPrice,
    getCurrentCurrency,
    setCurrentCurrency,
    getSearchTimer,
    setSearchTimer
  };
})();
