window.TransactionPagination = (function () {
  var els = null;
  var state = window.TransactionState;

  function initPagination(elements) {
    els = elements;
  }

  // Switch to another page.
  function goTo(page) {
    var filtered = window.TransactionTable.getFiltered();
    var totalPages = Math.max(1, Math.ceil(filtered.length / state.getPageSize()));
    state.setCurrentPage(Math.max(1, Math.min(page, totalPages)));
    window.TransactionTable.renderPage();
  }

  // Render pagination controls.
  function renderPagination(totalPages) {
    if (!els || !els.pagEl) return;
    if (totalPages <= 1) {
      els.pagEl.innerHTML = '';
      return;
    }

    var currentPage = state.getCurrentPage();
    var btnBase = 'w-8 h-8 flex items-center justify-center rounded text-xs font-bold transition-colors';
    var btnInactive = btnBase + ' border border-outline-variant/30 text-on-surface-variant hover:bg-surface-container-high';
    var btnActive = btnBase + ' bg-primary-container text-white';
    var btnDisabled = btnBase + ' border border-outline-variant/30 text-on-surface-variant opacity-30 cursor-default';

    var html = '<button id="tx-prev" class="' + (currentPage === 1 ? btnDisabled : btnInactive) + '"'
      + (currentPage === 1 ? ' disabled' : '') + '>'
      + '<span class="material-symbols-outlined text-sm" data-icon="chevron_left">chevron_left</span></button>';

    var start = Math.max(1, currentPage - 2);
    var end = Math.min(totalPages, start + 4);
    start = Math.max(1, end - 4);

    if (start > 1) {
      html += '<button class="' + btnInactive + '" data-page="1">1</button>';
      if (start > 2) html += '<span class="text-xs text-on-surface-variant px-1">...</span>';
    }

    for (var page = start; page <= end; page++) {
      html += '<button class="' + (page === currentPage ? btnActive : btnInactive) + '" data-page="' + page + '">' + page + '</button>';
    }

    if (end < totalPages) {
      if (end < totalPages - 1) html += '<span class="text-xs text-on-surface-variant px-1">...</span>';
      html += '<button class="' + btnInactive + '" data-page="' + totalPages + '">' + totalPages + '</button>';
    }

    html += '<button id="tx-next" class="' + (currentPage === totalPages ? btnDisabled : btnInactive) + '"'
      + (currentPage === totalPages ? ' disabled' : '') + '>'
      + '<span class="material-symbols-outlined text-sm" data-icon="chevron_right">chevron_right</span></button>';

    els.pagEl.innerHTML = html;

    var prevBtn = document.getElementById('tx-prev');
    var nextBtn = document.getElementById('tx-next');
    if (prevBtn) prevBtn.addEventListener('click', function () { goTo(currentPage - 1); });
    if (nextBtn) nextBtn.addEventListener('click', function () { goTo(currentPage + 1); });
    els.pagEl.querySelectorAll('[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        goTo(parseInt(btn.getAttribute('data-page'), 10));
      });
    });
  }

  return {
    initPagination: initPagination,
    goTo: goTo,
    renderPagination: renderPagination
  };
})();
