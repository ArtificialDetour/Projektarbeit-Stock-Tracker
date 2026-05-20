window.TransactionTable = (function () {
  var els = null;
  var utils = window.TransactionUtils;
  var state = window.TransactionState;

  function initTable(elements) {
    els = elements;
  }

  // Compute active date boundary.
  function getPeriodStart() {
    var now = new Date();
    if (state.getActivePeriod() === '30d') {
      return new Date(now.getFullYear(), now.getMonth(), now.getDate() - 30);
    }
    if (state.getActivePeriod() === 'ytd') {
      return new Date(now.getFullYear(), 0, 1);
    }
    return null;
  }

  // Filter current transaction data.
  function getFiltered() {
    var periodStart = getPeriodStart();
    var activeType = state.getActiveType();
    var activeSearchTerm = state.getActiveSearchTerm();

    return state.getAllData().filter(function (tx) {
      if (activeType !== 'ALL' && tx.transactionType !== activeType) return false;
      if (periodStart && new Date(tx.timestamp) < periodStart) return false;
      if (activeSearchTerm) {
        var searchText = utils.buildSearchText(tx);
        if (searchText.indexOf(activeSearchTerm) === -1) return false;
      }
      return true;
    });
  }

  function showNoResults() {
    if (!els || !els.noResult) return;
    els.noResult.style.display = '';
    els.noResult.classList.remove('hidden');
  }

  function hideNoResults() {
    if (!els || !els.noResult) return;
    els.noResult.style.display = 'none';
    els.noResult.classList.add('hidden');
  }

  function setCount(text) {
    if (els && els.countEl) {
      els.countEl.textContent = text;
    }
  }

  // Render the visible page.
  function renderPage() {
    if (!els || !els.tbody) return;

    var filtered = getFiltered();
    var total = filtered.length;
    var pageSize = state.getPageSize();
    var totalPages = Math.max(1, Math.ceil(total / pageSize));
    var currentPage = Math.min(state.getCurrentPage(), totalPages);
    state.setCurrentPage(currentPage);

    var start = (currentPage - 1) * pageSize;
    var pageData = filtered.slice(start, start + pageSize);

    els.tbody.querySelectorAll('tr[data-tx-type]').forEach(function (row) {
      row.remove();
    });

    if (pageData.length === 0) {
      showNoResults();
      setCount('0 transactions');
      window.TransactionPagination.renderPagination(0);
      return;
    }

    hideNoResults();

    var tmp = document.createElement('tbody');
    tmp.innerHTML = pageData.map(utils.renderRow).join('');
    while (tmp.firstChild) {
      els.tbody.insertBefore(tmp.firstChild, els.noResult);
    }

    var end = Math.min(start + pageSize, total);
    setCount('Showing ' + (start + 1) + '-' + end + ' of ' + total + ' transaction' + (total === 1 ? '' : 's'));

    window.TransactionPagination.renderPagination(totalPages);
  }

  return {
    initTable: initTable,
    getFiltered: getFiltered,
    renderPage: renderPage,
    showNoResults: showNoResults,
    hideNoResults: hideNoResults,
    setCount: setCount
  };
})();
