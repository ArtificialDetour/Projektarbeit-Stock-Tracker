window.PortfolioSort = (function () {
  var state = window.PortfolioState;

  // Wire table sorting.
  function initSort() {
    document.querySelectorAll('th[data-sort]').forEach(function (th) {
      th.addEventListener('click', function () {
        var key = th.getAttribute('data-sort');
        var sortState = state.getSortState();
        if (sortState.key === key) {
          sortState.dir = sortState.dir === 'asc' ? 'desc' : 'asc';
        } else {
          sortState.key = key;
          sortState.dir = 'asc';
        }
        updateSortArrows();
        window.PortfolioTable.rerenderRows();
      });
    });
  }

  // Update sort indicators.
  function updateSortArrows() {
    var sortState = state.getSortState();
    document.querySelectorAll('th[data-sort]').forEach(function (th) {
      var arrow = th.querySelector('.sort-arrow');
      if (!arrow) return;
      if (th.getAttribute('data-sort') !== sortState.key) {
        arrow.textContent = '\u2195';
        arrow.classList.add('opacity-40');
      } else {
        arrow.textContent = sortState.dir === 'asc' ? '\u2191' : '\u2193';
        arrow.classList.remove('opacity-40');
      }
    });
  }

  return {
    initSort: initSort,
    updateSortArrows: updateSortArrows
  };
})();
