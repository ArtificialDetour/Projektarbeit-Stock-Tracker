window.PortfolioTable = (function () {
  var els = null;
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;

  // Store table elements.
  function initTable(elements) {
    els = elements;
    if (els.backHoldingsBtn) {
      els.backHoldingsBtn.addEventListener('click', clearSelection);
    }
  }

  // Render holding row markup.
  function renderHoldingRow(holding) {
    var positive = parseFloat(holding.performancePercent) >= 0;
    var perfClass = positive
      ? 'bg-tertiary-fixed text-on-tertiary-fixed'
      : 'bg-error-container text-on-error-container';
    var perfSign = positive ? '+' : '';

    return '<tr class="hover:bg-surface-container-low transition-colors group" '
      + 'data-holding-symbol="' + utils.esc(holding.symbol) + '" '
      + 'data-holding-name="' + utils.esc(holding.assetName) + '" '
      + 'role="button" tabindex="0" aria-label="Open holding history">'
      + '<td class="py-5 px-6"><p class="font-bold text-sm">' + utils.esc(holding.assetName) + '</p>'
      + '<p class="text-xs text-on-surface-variant font-mono">' + utils.esc(holding.symbol) + '</p></td>'
      + '<td class="py-5 px-6 text-sm text-on-surface-variant font-medium">' + utils.fmtDate(holding.firstPurchaseDate) + '</td>'
      + '<td class="py-5 px-6 text-sm font-bold text-right">' + utils.fmtQty(holding.quantity) + '</td>'
      + '<td class="py-5 px-6 text-sm font-medium text-right">' + utils.fmt(holding.avgCostBasis, 2) + '</td>'
      + '<td class="py-5 px-6 text-sm font-medium text-right">' + utils.fmt(holding.costBasisTotal, 2) + '</td>'
      + '<td class="py-5 px-6 text-sm font-black text-right">' + utils.fmt(holding.currentValue, 2) + '</td>'
      + '<td class="py-5 px-6 text-right"><span class="' + perfClass + ' px-2 py-0.5 rounded-full text-[10px] font-black">'
      + perfSign + utils.fmt(holding.performancePercent, 2) + '%'
      + '</span></td>'
      + '<td class="py-5 px-6 text-right">'
      + '<button class="sell-all-btn px-3 py-1 rounded-lg text-xs font-bold bg-error/10 text-error hover:bg-error hover:text-white transition-colors" '
      + 'data-sell-symbol="' + utils.esc(holding.symbol) + '" aria-label="Sell all ' + utils.esc(holding.assetName) + '">Sell All</button>'
      + '</td>'
      + '</tr>';
  }

  // Sort visible holdings.
  function sortedHoldings() {
    var sortState = state.getSortState();
    var holdings = state.getHoldings();
    if (!sortState.key) return holdings;

    return holdings.slice().sort(function (a, b) {
      var valA;
      var valB;
      if (sortState.key === 'name') {
        valA = (a.assetName || '').toLowerCase();
        valB = (b.assetName || '').toLowerCase();
        return sortState.dir === 'asc' ? valA.localeCompare(valB) : valB.localeCompare(valA);
      }
      if (sortState.key === 'perf') {
        valA = parseFloat(a.performancePercent);
        valB = parseFloat(b.performancePercent);
        return sortState.dir === 'asc' ? valA - valB : valB - valA;
      }
      return 0;
    });
  }

  function cssEscape(value) {
    // Symbols can contain selector-sensitive characters such as dots or slashes.
    if (window.CSS && typeof window.CSS.escape === 'function') {
      return window.CSS.escape(value);
    }
    return String(value).replace(/["\\]/g, '\\$&');
  }

  // Remove holding rows.
  function clearRows() {
    if (!els || !els.tbody) return;
    Array.prototype.slice.call(els.tbody.querySelectorAll('[data-holding-symbol], [data-lot-row]')).forEach(function (row) {
      row.parentNode.removeChild(row);
    });
  }

  // Wire row history clicks.
  function wireRowClicks() {
    var rows = Array.prototype.slice.call(els.tbody.querySelectorAll('[data-holding-symbol]'));
    rows.forEach(function (row) {
      var symbol = row.getAttribute('data-holding-symbol');
      row.addEventListener('click', function () {
        selectHolding(symbol);
      });
      row.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          selectHolding(symbol);
        }
      });
    });
  }

  function wireSellAll(button) {
    button.addEventListener('click', function (event) {
      event.stopPropagation();
      var symbol = button.getAttribute('data-sell-symbol');
      var holding = state.findHolding(symbol);
      if (!holding) return;
      var qty = parseFloat(holding.quantity);
      window.PortfolioSell.openSellModal(holding, qty, true);
    });
  }

  function wireSellButtons() {
    Array.prototype.slice.call(els.tbody.querySelectorAll('.sell-all-btn')).forEach(wireSellAll);
  }

  // Insert rendered rows.
  function insertRows(holdings) {
    var html = holdings.map(renderHoldingRow).join('');
    var tmp = document.createElement('tbody');
    tmp.innerHTML = html;
    while (tmp.firstChild) {
      els.tbody.insertBefore(tmp.firstChild, els.emptyRow);
    }
    wireRowClicks();
    wireSellButtons();
  }

  // Render holding table.
  function renderRows(holdings) {
    clearRows();
    if (state.getSelectedSymbol()) {
      renderSelectedHoldingDetails(state.getSelectedSymbol());
      return;
    }
    setOverviewMode();
    insertRows(holdings);
    window.PortfolioFilters.applyFilter();
  }

  function rerenderRows() {
    renderRows(sortedHoldings());
  }

  function setHeaderText(el, text) {
    if (!el) return;
    el.textContent = text;
  }

  function setOverviewMode() {
    if (els.tableTitleEl) els.tableTitleEl.textContent = 'Stock Details';
    if (els.backHoldingsBtn) els.backHoldingsBtn.classList.add('hidden');
    if (els.filterControlsEl) els.filterControlsEl.classList.remove('hidden');
    setHeaderText(els.colPrimaryEl, 'Asset Name');
    setHeaderText(els.colDateEl, 'First Purchase Date');
    setHeaderText(els.colQuantityEl, 'Quantity');
    setHeaderText(els.colPriceEl, 'Avg. Stock Price');
    setHeaderText(els.colBuyInEl, 'Buy-In Amount');
    setHeaderText(els.colValueEl, 'Current Value');
    setHeaderText(els.colPerformanceEl, 'Performance');
    setHeaderText(els.colActionsEl, 'Actions');
  }

  function setDetailMode(holding) {
    if (els.tableTitleEl) els.tableTitleEl.textContent = holding.assetName + ' Purchases';
    if (els.backHoldingsBtn) els.backHoldingsBtn.classList.remove('hidden');
    if (els.filterControlsEl) els.filterControlsEl.classList.add('hidden');
    setHeaderText(els.colPrimaryEl, 'Purchase Date');
    setHeaderText(els.colDateEl, 'Asset');
    setHeaderText(els.colQuantityEl, 'Quantity');
    setHeaderText(els.colPriceEl, 'Buy Price');
    setHeaderText(els.colBuyInEl, 'Buy-In Amount');
    setHeaderText(els.colValueEl, 'Current Value');
    setHeaderText(els.colPerformanceEl, 'Performance');
    setHeaderText(els.colActionsEl, 'Actions');
  }

  function selectHolding(symbol) {
    var holding = state.findHolding(symbol);
    if (!holding) return;
    state.setSelectedSymbol(symbol);
    clearRows();
    renderSelectedHoldingDetails(symbol);
    window.PortfolioChart.loadChart(symbol);
  }

  function clearSelection() {
    state.setSelectedSymbol(null);
    clearRows();
    setOverviewMode();
    renderRows(sortedHoldings());
    window.PortfolioChart.loadChart();
  }

  function getOpenLots(transactions) {
    var all = (transactions || []).slice().sort(function (a, b) {
      return new Date(a.timestamp) - new Date(b.timestamp);
    });
    var buys = all.filter(function (tx) { return tx.transactionType === 'BUY'; });
    var sells = all.filter(function (tx) { return tx.transactionType === 'SELL'; });
    var exactSells = sells.filter(function (tx) { return tx.relatedBuyId != null; });
    var genericSells = sells.filter(function (tx) { return tx.relatedBuyId == null; });
    var sellLeft = genericSells.reduce(function (sum, tx) {
      return sum + parseFloat(tx.quantity);
    }, 0);

    return buys.map(function (tx) {
      var originalQty = parseFloat(tx.quantity);
      var exactSold = exactSells
        .filter(function (sellTx) { return sellTx.relatedBuyId === tx.transactionId; })
        .reduce(function (sum, sellTx) { return sum + parseFloat(sellTx.quantity); }, 0);
      var remainingAfterExact = Math.max(0, originalQty - exactSold);

      if (sellLeft <= 0) return Object.assign({}, tx, { remainingQty: remainingAfterExact });
      if (sellLeft >= remainingAfterExact) {
        sellLeft -= remainingAfterExact;
        return null;
      }
      var remaining = remainingAfterExact - sellLeft;
      sellLeft = 0;
      return Object.assign({}, tx, { remainingQty: remaining });
    }).filter(function (tx) {
      return tx && tx.remainingQty > 0;
    }).reverse();
  }

  function renderLotRow(tx, holding) {
    var purchasePrice = parseFloat(tx.pricePerShare);
    var currentPrice = parseFloat(holding.currentPrice);
    var qty = tx.remainingQty;
    var currentValue = currentPrice * qty;
    var pct = purchasePrice > 0 ? (currentPrice - purchasePrice) / purchasePrice * 100 : 0;
    var absGain = (currentPrice - purchasePrice) * qty;
    var positive = pct >= 0;
    var sign = positive ? '+' : '';
    var color = positive ? 'text-on-tertiary-container' : 'text-error';

    return '<tr data-lot-row data-tx-qty="' + qty + '" data-tx-id="' + tx.transactionId + '" class="hover:bg-surface-container-low transition-colors">'
      + '<td class="py-5 px-6 text-sm font-medium text-on-surface-variant whitespace-nowrap">' + utils.esc(utils.fmtDateTime(tx.timestamp)) + '</td>'
      + '<td class="py-5 px-6"><p class="font-bold text-sm">' + utils.esc(holding.assetName) + '</p><p class="text-xs text-on-surface-variant font-mono">' + utils.esc(holding.symbol) + '</p></td>'
      + '<td class="py-5 px-6 text-sm font-bold text-right">' + utils.fmtQty(qty) + '</td>'
      + '<td class="py-5 px-6 text-sm font-medium text-right">' + utils.fmt(purchasePrice, 2) + '</td>'
      + '<td class="py-5 px-6 text-sm font-medium text-right">' + utils.fmt(purchasePrice * qty, 2) + '</td>'
      + '<td class="py-5 px-6 text-sm font-black text-right">' + utils.fmt(currentValue, 2) + '</td>'
      + '<td class="py-5 px-6 text-right"><p class="text-sm font-black ' + color + '">' + sign + utils.fmt(pct, 2) + '%</p>'
      + '<p class="text-[10px] text-on-surface-variant">' + sign + utils.fmt(absGain, 2) + ' \u20AC</p></td>'
      + '<td class="py-5 px-6 text-right"><button class="lot-row-sell px-3 py-1 rounded-lg text-xs font-bold bg-error/10 text-error hover:bg-error hover:text-white transition-colors">Sell</button></td>'
      + '</tr>';
  }

  function renderSelectedHoldingDetails(symbol) {
    var holding = state.findHolding(symbol);
    if (!holding) return;
    setDetailMode(holding);
    if (els.emptyRow) {
      els.emptyRow.style.display = '';
      els.emptyRow.querySelector('td').textContent = 'Loading purchases...';
    }

    fetch('/api/assets/transactions/' + encodeURIComponent(symbol))
      .then(function (response) { return response.ok ? response.json() : []; })
      .then(function (data) {
        if (state.getSelectedSymbol() !== symbol) return;
        clearRows();
        var lots = getOpenLots(data);
        if (!lots.length) {
          if (els.emptyRow) {
            els.emptyRow.style.display = '';
            els.emptyRow.querySelector('td').textContent = 'No active purchases for this holding.';
          }
          return;
        }

        if (els.emptyRow) els.emptyRow.style.display = 'none';
        var tmp = document.createElement('tbody');
        tmp.innerHTML = lots.map(function (tx) { return renderLotRow(tx, holding); }).join('');
        while (tmp.firstChild) {
          els.tbody.insertBefore(tmp.firstChild, els.emptyRow);
        }
        Array.prototype.slice.call(els.tbody.querySelectorAll('.lot-row-sell')).forEach(function (btn) {
          btn.addEventListener('click', function (event) {
            event.stopPropagation();
            var row = btn.closest('tr');
            var qty = row ? parseFloat(row.getAttribute('data-tx-qty')) : null;
            var txId = row ? parseInt(row.getAttribute('data-tx-id'), 10) : null;
            state.setPendingReopenSymbol(symbol);
            window.PortfolioSell.openSellModal(holding, qty, false, qty, txId);
          });
        });
      })
      .catch(function () {
        if (state.getSelectedSymbol() !== symbol) return;
        clearRows();
        if (els.emptyRow) {
          els.emptyRow.style.display = '';
          els.emptyRow.querySelector('td').textContent = 'Failed to load purchases.';
        }
      });
  }

  // Update live row values.
  function updateRow(holding) {
    var row = els.tbody.querySelector('[data-holding-symbol="' + cssEscape(holding.symbol) + '"]');
    if (!row) return;

    var cells = row.querySelectorAll('td');
    var newVal = parseFloat(holding.currentValue);
    var newPerf = parseFloat(holding.performancePercent);
    var prev = state.getPrevValues()[holding.symbol];

    if (prev) {
      var dir = newVal >= prev.currentValue ? 'up' : 'down';
      if (cells[5]) {
        cells[5].textContent = utils.fmt(holding.currentValue, 2);
        utils.flashEl(cells[5], dir);
      }
      if (cells[4]) cells[4].textContent = utils.fmt(holding.costBasisTotal, 2);
      if (cells[3]) cells[3].textContent = utils.fmt(holding.avgCostBasis, 2);
      if (cells[2]) cells[2].textContent = utils.fmtQty(holding.quantity);
      if (cells[6]) {
        var badge = cells[6].querySelector('span');
        var positive = newPerf >= 0;
        var perfSign = positive ? '+' : '';
        if (badge) {
          badge.textContent = perfSign + utils.fmt(holding.performancePercent, 2) + '%';
          badge.className = (positive
            ? 'bg-tertiary-fixed text-on-tertiary-fixed'
            : 'bg-error-container text-on-error-container')
            + ' px-2 py-0.5 rounded-full text-[10px] font-black';
        }
      }
    }

    state.getPrevValues()[holding.symbol] = {
      currentValue: newVal,
      performancePercent: newPerf
    };
  }

  // Update portfolio totals.
  function updateTotals(holdings, flashTotal) {
    var total = holdings.reduce(function (sum, holding) {
      return sum + parseFloat(holding.currentValue);
    }, 0);
    var totalCost = holdings.reduce(function (sum, holding) {
      return sum + parseFloat(holding.costBasisTotal);
    }, 0);

    if (els.totalValueEl) {
      els.totalValueEl.textContent = total.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '€';
      if (flashTotal && state.getPrevPortfolioTotal() !== null) {
        utils.flashEl(els.totalValueEl, total >= state.getPrevPortfolioTotal() ? 'up' : 'down');
      }
    }
    if (els.totalBuyInEl) {
      els.totalBuyInEl.textContent = totalCost.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
    }
    if (els.totalCurrentEl) {
      els.totalCurrentEl.textContent = total.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
    }
    if (els.totalReturnEl && totalCost > 0) {
      var ret = (total - totalCost) / totalCost * 100;
      var sign = ret >= 0 ? '+' : '';
      els.totalReturnEl.textContent = sign + ret.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '% Total Return';
    }

    state.setPrevPortfolioTotal(total);
  }

  function showEmptyState() {
    if (els.emptyRow) {
      els.emptyRow.style.display = '';
      els.emptyRow.querySelector('td').textContent = 'No holdings yet. Use "+ Add Asset" to get started.';
    }
    if (els.totalValueEl) els.totalValueEl.textContent = '0€';
    if (els.totalReturnEl) els.totalReturnEl.textContent = '0,00% Total Return';
  }

  function hideEmptyState() {
    if (els.emptyRow) els.emptyRow.style.display = 'none';
  }

  return {
    initTable: initTable,
    sortedHoldings: sortedHoldings,
    renderHoldingRow: renderHoldingRow,
    clearRows: clearRows,
    renderRows: renderRows,
    rerenderRows: rerenderRows,
    updateRow: updateRow,
    updateTotals: updateTotals,
    selectHolding: selectHolding,
    clearSelection: clearSelection,
    renderSelectedHoldingDetails: renderSelectedHoldingDetails,
    showEmptyState: showEmptyState,
    hideEmptyState: hideEmptyState
  };
})();
