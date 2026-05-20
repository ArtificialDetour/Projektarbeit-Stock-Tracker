window.PortfolioSell = (function () {
  var els = null;
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;
  var sellState = { symbol: '', price: 0, maxQty: 0, isSellAll: false, relatedBuyId: null };

  // Wire sell modal.
  function initSell(elements) {
    els = elements;

    if (els.sellCloseBtn) els.sellCloseBtn.addEventListener('click', closeSellModal);
    if (els.sellBackdrop) els.sellBackdrop.addEventListener('click', closeSellModal);

    if (els.sellQtyInput) {
      els.sellQtyInput.addEventListener('input', updateSellTotalFromInput);
    }

    if (els.sellConfirmBtn) {
      els.sellConfirmBtn.addEventListener('click', confirmSale);
    }
  }

  // Open sell modal.
  function openSellModal(holding, prefillQty, isSellAll, maxLimit, relatedBuyId) {
    sellState.symbol = holding.symbol;
    sellState.price = parseFloat(holding.currentPrice);

    var holdingQty = parseFloat(holding.quantity);
    sellState.maxQty = maxLimit != null && !isNaN(maxLimit) && maxLimit < holdingQty ? maxLimit : holdingQty;
    sellState.isSellAll = !!isSellAll;
    sellState.relatedBuyId = relatedBuyId || null;

    els.sellNameEl.textContent = holding.assetName;
    els.sellBadgeEl.textContent = holding.symbol;
    els.sellPriceEl.textContent = sellState.price.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
    els.sellHoldingEl.textContent = utils.fmtQty(holding.quantity) + ' shares';
    els.sellQtyInput.max = sellState.maxQty;
    els.sellTotalEl.textContent = '\u2014';
    els.sellErrorEl.classList.add('hidden');
    els.sellConfirmBtn.disabled = true;

    if (els.sellTitleEl) {
      els.sellTitleEl.textContent = sellState.isSellAll ? 'Sell All' : 'Sell Asset';
    }
    if (els.sellQtyWrapper) {
      els.sellQtyWrapper.style.display = sellState.isSellAll ? 'none' : '';
    }

    if (sellState.isSellAll) {
      els.sellQtyInput.value = sellState.maxQty;
      els.sellTotalEl.textContent = (sellState.maxQty * sellState.price).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
      els.sellConfirmBtn.disabled = false;
    } else if (prefillQty != null && prefillQty > 0 && prefillQty <= sellState.maxQty) {
      els.sellQtyInput.value = prefillQty;
      els.sellTotalEl.textContent = (prefillQty * sellState.price).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
      els.sellConfirmBtn.disabled = false;
    } else {
      els.sellQtyInput.value = '';
    }

    els.sellModal.classList.remove('hidden');
  }

  // Close sell modal.
  function closeSellModal() {
    els.sellModal.classList.add('hidden');
  }

  // Recalculate sale total.
  function updateSellTotalFromInput() {
    var qty = parseFloat(els.sellQtyInput.value);
    if (isNaN(qty) || qty <= 0 || qty > sellState.maxQty) {
      els.sellTotalEl.textContent = '\u2014';
      els.sellConfirmBtn.disabled = true;
      return;
    }
    els.sellTotalEl.textContent = (qty * sellState.price).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
    els.sellConfirmBtn.disabled = false;
  }

  // Confirm sale flow.
  function confirmSale() {
    var qty;
    if (sellState.isSellAll) {
      qty = sellState.maxQty;
    } else {
      qty = parseFloat(els.sellQtyInput.value);
      if (isNaN(qty) || qty <= 0 || qty > sellState.maxQty) return;
    }

    els.sellConfirmBtn.disabled = true;
    els.sellErrorEl.classList.add('hidden');

    fetch('/api/assets/sell', {
      method: 'POST',
      headers: state.getCsrfHeaders(),
      body: JSON.stringify({
        symbol: sellState.symbol,
        quantity: qty,
        expectedPrice: sellState.price,
        relatedBuyId: sellState.relatedBuyId
      })
    })
      .then(function (response) {
        if (response.status === 409) {
          return response.json().then(function (data) {
            throw { conflict: true, price: data.currentPrice };
          });
        }
        if (!response.ok) {
          return response.json().then(function (data) {
            throw { error: data.error || 'Sale failed.' };
          });
        }
        return response.json();
      })
      .then(function () {
        closeSellModal();
        window.PortfolioData.loadHoldings();
      })
      .catch(function (error) {
        els.sellConfirmBtn.disabled = false;
        if (error && error.conflict) {
          // A changed quote requires a second confirmation with the updated sale price.
          sellState.price = parseFloat(error.price);
          els.sellPriceEl.textContent = sellState.price.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
          var qtyToSell = sellState.isSellAll ? sellState.maxQty : parseFloat(els.sellQtyInput.value);
          if (!isNaN(qtyToSell) && qtyToSell > 0) {
            els.sellTotalEl.textContent = (qtyToSell * sellState.price).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' \u20AC';
            els.sellConfirmBtn.disabled = false;
          }
          els.sellErrorEl.textContent = 'Price changed, new price shown above. Confirm again to proceed.';
        } else {
          els.sellErrorEl.textContent = (error && error.error) ? error.error : 'Sale failed. Please try again.';
        }
        els.sellErrorEl.classList.remove('hidden');
      });
  }

  return {
    initSell: initSell,
    openSellModal: openSellModal,
    closeSellModal: closeSellModal
  };
})();
