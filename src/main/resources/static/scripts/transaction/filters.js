window.TransactionFilters = (function () {
  var els = null;
  var state = window.TransactionState;
  var table = window.TransactionTable;

  // Highlight active type filter.
  function setActiveFilterBtn(type) {
    if (!els || !els.filterBtns) return;

    els.filterBtns.forEach(function (btn) {
      var isActive = btn.getAttribute('data-tx-filter') === type;
      btn.classList.toggle('bg-surface-container-lowest', isActive);
      btn.classList.toggle('text-primary-container', isActive);
      btn.classList.toggle('font-bold', isActive);
      btn.classList.toggle('shadow-sm', isActive);
      btn.classList.toggle('text-on-surface-variant', !isActive);
      btn.classList.toggle('hover:text-on-surface', !isActive);
      btn.classList.toggle('hover:bg-surface-container-high', !isActive);
      btn.classList.toggle('font-semibold', !isActive);
    });
  }

  // Wire transaction filters.
  function initFilterButtons() {
    if (!els || !els.filterBtns) return;

    els.filterBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var type = btn.getAttribute('data-tx-filter');
        state.setActiveType(type);
        state.setCurrentPage(1);
        setActiveFilterBtn(type);
        table.renderPage();
      });
    });
  }

  // Wire page-size selector.
  function initPageSizeSelector() {
    if (!els || !els.pageSizeEl) return;

    els.pageSizeEl.addEventListener('change', function () {
      state.setPageSize(parseInt(els.pageSizeEl.value, 10));
      state.setCurrentPage(1);
      table.renderPage();
    });
  }

  // Wire period selector.
  function initDateFilter() {
    if (!els || !els.dateFilterEl) return;

    els.dateFilterEl.addEventListener('change', function () {
      state.setActivePeriod(els.dateFilterEl.value);
      state.setCurrentPage(1);
      table.renderPage();
    });
  }

  // Sync shared search input.
  function initTopnavSearch() {
    document.addEventListener('input', function (event) {
      if (event.target && event.target.id === 'topnav-search') {
        state.setActiveSearchTerm(event.target.value);
        state.setCurrentPage(1);
        table.renderPage();
      }
    });
  }

  // Initialize filter controls.
  function initFilters(elements) {
    els = elements;
    initTopnavSearch();
    initFilterButtons();
    initPageSizeSelector();
    initDateFilter();
  }

  return {
    initFilters: initFilters,
    setActiveFilterBtn: setActiveFilterBtn
  };
})();
