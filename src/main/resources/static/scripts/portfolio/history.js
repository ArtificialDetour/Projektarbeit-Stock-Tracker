window.PortfolioHistory = (function () {
  var els = null;
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;

  // Wire history modal.
  function initHistory(elements) {
    els = elements;

    if (els.historyCloseButton) {
      els.historyCloseButton.addEventListener('click', closeModal);
    }
    if (els.historyOverlay) {
      els.historyOverlay.addEventListener('click', closeModal);
    }

    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && els.historyModal && !els.historyModal.classList.contains('hidden')) {
        closeModal();
      }
    });
  }

  // Open purchase history.
  function openHistoryModal(symbol, assetName) {
    if (!els.historyModal || !els.historyOverlay) return;

    els.historyTitleEl.textContent = assetName + ' (' + symbol + ')';
    els.historySubtitleEl.textContent = 'Purchase history';
    els.historyBody.innerHTML = '<tr><td colspan="6" class="py-4 px-4 text-sm text-on-surface-variant text-center">Loading...</td></tr>';
    els.historyCountEl.textContent = '0';
    els.historyOverlay.classList.remove('hidden');
    els.historyModal.classList.remove('hidden');

    var currentHolding = state.findHolding(symbol);
    var currentPrice = currentHolding ? parseFloat(currentHolding.currentPrice) : null;

    fetch('/api/assets/transactions/' + encodeURIComponent(symbol))
      .then(function (response) {
        return response.ok ? response.json() : [];
      })
      .then(function (data) {
        var all = (data || []).slice().sort(function (a, b) {
          return new Date(a.timestamp) - new Date(b.timestamp);
        });
        var buys = all.filter(function (tx) {
          return tx.transactionType === 'BUY';
        });
        var sells = all.filter(function (tx) {
          return tx.transactionType === 'SELL';
        });
        var exactSells = sells.filter(function (tx) {
          return tx.relatedBuyId != null;
        });
        var genericSells = sells.filter(function (tx) {
          return tx.relatedBuyId == null;
        });
        // Generic sells are applied FIFO because they are not tied to one purchase lot.
        var sellLeft = genericSells.reduce(function (sum, tx) {
          return sum + parseFloat(tx.quantity);
        }, 0);

        var transactions = buys.map(function (tx) {
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

        els.historyCountEl.textContent = String(transactions.length);
        if (!transactions.length) {
          els.historyBody.innerHTML = '<tr><td colspan="6" class="py-4 px-4 text-sm text-on-surface-variant">No active holdings.</td></tr>';
          return;
        }

        els.historyBody.innerHTML = transactions.map(function (tx) {
          return renderHistoryRow(tx, currentPrice);
        }).join('');

        Array.prototype.slice.call(els.historyBody.querySelectorAll('.modal-row-sell')).forEach(function (btn) {
          btn.addEventListener('click', function (event) {
            event.stopPropagation();
            var row = btn.closest('tr');
            var qty = row ? parseFloat(row.getAttribute('data-tx-qty')) : null;
            var txId = row ? parseInt(row.getAttribute('data-tx-id'), 10) : null;
            var holding = state.findHolding(symbol);
            if (holding) {
              state.setPendingReopenSymbol(symbol);
              closeModal();
              window.PortfolioSell.openSellModal(holding, qty, false, qty, txId);
            }
          });
        });
      })
      .catch(function () {
        els.historyBody.innerHTML = '<tr><td colspan="6" class="py-4 px-4 text-sm text-on-surface-variant">Failed to load.</td></tr>';
      });
  }

  // Render purchase row.
  function renderHistoryRow(tx, currentPrice) {
    var purchasePrice = parseFloat(tx.pricePerShare);
    var qty = tx.remainingQty;
    var perfCell = '<td class="py-4 px-4 text-sm text-right text-on-surface-variant">\u2014</td>';

    if (currentPrice !== null) {
      var pct = (currentPrice - purchasePrice) / purchasePrice * 100;
      var absGain = (currentPrice - purchasePrice) * qty;
      var positive = pct >= 0;
      var sign = positive ? '+' : '';
      var color = positive ? 'text-on-tertiary-container' : 'text-error';
      perfCell = '<td class="py-4 px-4 text-right">'
        + '<p class="text-sm font-black ' + color + '">' + sign + utils.fmt(pct, 2) + '%</p>'
        + '<p class="text-[10px] text-on-surface-variant">' + sign + utils.fmt(absGain, 2) + ' \u20AC</p>'
        + '</td>';
    }

    return '<tr class="hover:bg-surface-container-low transition-colors" data-tx-qty="' + qty + '" data-tx-id="' + tx.transactionId + '">'
      + '<td class="py-4 px-4 text-sm font-medium text-on-surface-variant whitespace-nowrap">' + utils.esc(utils.fmtDateTime(tx.timestamp)) + '</td>'
      + '<td class="py-4 px-4 text-sm font-bold text-right">' + utils.fmtQty(qty) + '</td>'
      + '<td class="py-4 px-4 text-sm font-medium text-right">' + utils.fmt(purchasePrice, 2) + ' \u20AC</td>'
      + '<td class="py-4 px-4 text-sm font-black text-right">' + utils.fmt(purchasePrice * qty, 2) + ' \u20AC</td>'
      + perfCell
      + '<td class="py-4 px-4 text-right">'
      + '<button class="modal-row-sell px-2 py-1 rounded text-xs font-bold bg-error/10 text-error hover:bg-error hover:text-white transition-colors">Sell</button>'
      + '</td>'
      + '</tr>';
  }

  // Close history modal.
  function closeModal() {
    if (els.historyOverlay) els.historyOverlay.classList.add('hidden');
    if (els.historyModal) els.historyModal.classList.add('hidden');
  }

  return {
    initHistory: initHistory,
    openHistoryModal: openHistoryModal,
    closeModal: closeModal
  };
})();
