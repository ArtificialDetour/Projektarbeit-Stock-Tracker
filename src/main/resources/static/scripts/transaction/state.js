window.TransactionState = (function () {
  var allData = [];
  var activeType = 'ALL';
  var activePeriod = 'all';
  var currentPage = 1;
  var pageSize = 50;
  // The top navigation can pass a search term into the transaction page.
  var activeSearchTerm = new URLSearchParams(window.location.search).get('q') || '';
  activeSearchTerm = activeSearchTerm.trim().toLowerCase();

  function getAllData() {
    return allData;
  }

  function setAllData(data) {
    allData = data || [];
  }

  function getActiveType() {
    return activeType;
  }

  function setActiveType(type) {
    activeType = type;
  }

  function getActivePeriod() {
    return activePeriod;
  }

  function setActivePeriod(period) {
    activePeriod = period;
  }

  function getCurrentPage() {
    return currentPage;
  }

  function setCurrentPage(page) {
    currentPage = page;
  }

  function getPageSize() {
    return pageSize;
  }

  function setPageSize(size) {
    pageSize = size;
  }

  function getActiveSearchTerm() {
    return activeSearchTerm;
  }

  function setActiveSearchTerm(searchTerm) {
    activeSearchTerm = (searchTerm || '').trim().toLowerCase();
  }

  return {
    getAllData: getAllData,
    setAllData: setAllData,
    getActiveType: getActiveType,
    setActiveType: setActiveType,
    getActivePeriod: getActivePeriod,
    setActivePeriod: setActivePeriod,
    getCurrentPage: getCurrentPage,
    setCurrentPage: setCurrentPage,
    getPageSize: getPageSize,
    setPageSize: setPageSize,
    getActiveSearchTerm: getActiveSearchTerm,
    setActiveSearchTerm: setActiveSearchTerm
  };
})();
