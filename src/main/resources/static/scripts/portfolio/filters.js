window.PortfolioFilters = (function () {
  var els = null;
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;

  // Wire category filters.
  function initFilters(elements) {
    els = elements;

    els.filterBtns.forEach(function (btn) {
      btn.addEventListener('click', function () {
        var category = btn.getAttribute('data-portfolio-filter');
        state.setActiveCategory(category);
        setActiveFilterBtn(category);
        applyFilter();
      });
    });
  }

  // Highlight active category.
  function setActiveFilterBtn(type) {
    if (!els || !els.filterBtns) return;

    els.filterBtns.forEach(function (btn) {
      var isActive = btn.getAttribute('data-portfolio-filter') === type;
      btn.classList.toggle('bg-surface-container-high', isActive);
      btn.classList.toggle('font-bold', isActive);
      btn.classList.toggle('text-on-surface', isActive);
      btn.classList.toggle('text-on-surface-variant', !isActive);
    });
  }

  // Apply category filter.
  function applyFilter() {
    if (!els || !els.tbody) return;

    var rows = Array.prototype.slice.call(els.tbody.querySelectorAll('tr[data-holding-symbol]'));
    var visible = 0;
    rows.forEach(function (row) {
      var symbol = row.getAttribute('data-holding-symbol') || '';
      var activeCategory = state.getActiveCategory();
      var show = activeCategory === 'ALL'
        || (activeCategory === 'CRYPTO' && utils.isCrypto(symbol))
        || (activeCategory === 'STOCKS' && !utils.isCrypto(symbol));
      row.style.display = show ? '' : 'none';
      if (show) visible++;
    });

    if (els.emptyRow) {
      els.emptyRow.style.display = visible === 0 ? '' : 'none';
    }
  }

  return {
    initFilters: initFilters,
    setActiveFilterBtn: setActiveFilterBtn,
    applyFilter: applyFilter
  };
})();
