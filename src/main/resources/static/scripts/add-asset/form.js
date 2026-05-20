window.AddAssetForm = (function () {
  const state = window.AddAssetState;
  const api = window.AddAssetApi;
  const ui = window.AddAssetUi;
  const utils = window.AddAssetUtils;

  // Wire purchase form.
  function initForm() {
    const els = ui.getElements();

    document.addEventListener('click', event => {
      if (event.target.closest('[data-add-asset]')) {
        ui.openModal();
      }
    });

    els.closeBtn.addEventListener('click', ui.closeModal);
    els.backdrop.addEventListener('click', ui.closeModal);
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        ui.closeModal();
      }
    });

    els.searchEl.addEventListener('input', handleSearchInput);
    els.qtyEl.addEventListener('input', ui.updateTotal);
    if (els.editOptionsEl) {
      els.editOptionsEl.addEventListener('change', handleEditOptionsToggle);
    }
    if (els.customPriceEl) {
      els.customPriceEl.addEventListener('input', ui.updateTotal);
    }
    els.confirmBtn.addEventListener('click', confirmPurchase);
  }

  // Toggle manual purchase fields.
  function handleEditOptionsToggle() {
    const els = ui.getElements();
    const enabled = els.editOptionsEl.checked;
    ui.setEditOptionsEnabled(enabled);
    if (enabled && state.getCurrentPrice() > 0 && !els.customPriceEl.value) {
      els.customPriceEl.value = state.getCurrentPrice();
    }
    if (enabled && !els.customTimestampEl.value) {
      els.customTimestampEl.value = toLocalDateTimeInputValue(new Date());
    }
    ui.updateTotal();
  }

  // Handle debounced search.
  function handleSearchInput() {
    clearTimeout(state.getSearchTimer());
    const els = ui.getElements();
    const query = els.searchEl.value.trim();

    if (query.length < 2) {
      els.resultsList.classList.add('hidden');
      return;
    }

    state.setSearchTimer(setTimeout(() => {
      // Debounce remote search so typing does not create one request per keystroke.
      api.searchAssets(query)
        .then(ui.renderResults)
        .catch(() => {});
    }, 320));
  }

  // Select search result.
  function selectAsset(item) {
    const els = ui.getElements();
    state.setSelectedSymbol(item.symbol);
    state.setSelectedName(item.name || item.symbol);
    els.searchEl.value = item.symbol + (item.name ? ' - ' + item.name : '');
    els.resultsList.classList.add('hidden');
    loadQuote(item.symbol);
  }

  // Load selected quote.
  function loadQuote(symbol) {
    ui.setLoadingQuote();
    api.loadQuote(symbol)
      .then(data => {
        if (!data) return;
        ui.renderQuote(symbol, data);
        ui.updateTotal();
      })
      .catch(() => {});
  }

  // Confirm purchase flow.
  function confirmPurchase() {
    const els = ui.getElements();
    const qty = parseFloat(els.qtyEl.value);
    if (!state.getSelectedSymbol() || isNaN(qty) || qty <= 0) return;

    const editOptions = getEditOptions();
    if (editOptions.enabled && !editOptions.valid) {
      ui.showError(editOptions.error);
      return;
    }

    ui.setConfirmProcessing();
    ui.hideError();

    api.buyAsset(state.getSelectedSymbol(), qty, ui.getEffectivePrice(), editOptions.payload)
      .then(async response => {
        const data = await response.json().catch(() => null);
        if (response.status === 409 && data?.currentPrice) {
          // The backend rejects stale prices, so the user confirms against the refreshed quote.
          const newPrice = parseFloat(data.currentPrice);
          state.setCurrentPrice(newPrice);
          els.priceEl.textContent = utils.formatCurrency(newPrice);
          ui.updateTotal();
          ui.showError('Price has changed to ' + utils.formatCurrency(newPrice) + ' \u20AC. Please confirm again.');
        } else if (response.ok) {
          ui.closeModal();
          ui.showSuccessBanner(data);
          setTimeout(() => window.location.reload(), 1500);
        } else {
          ui.showError('Something went wrong. Please try again.');
          els.confirmBtn.disabled = false;
        }
      })
      .catch(() => {
        ui.showError('Network error. Please try again.');
        els.confirmBtn.disabled = false;
      })
      .finally(() => {
        ui.resetConfirmText();
      });
  }

  // Build optional purchase payload.
  function getEditOptions() {
    const els = ui.getElements();
    if (!ui.isEditOptionsEnabled()) {
      return { enabled: false, valid: true, payload: {} };
    }

    const customPrice = parseFloat(els.customPriceEl.value);
    if (isNaN(customPrice) || customPrice <= 0) {
      return { enabled: true, valid: false, error: 'Please enter a valid purchase price.' };
    }
    if (!els.customTimestampEl.value) {
      return { enabled: true, valid: false, error: 'Please enter a purchase time.' };
    }

    return {
      enabled: true,
      valid: true,
      payload: {
        customPrice,
        purchasedAt: els.customTimestampEl.value
      }
    };
  }

  function toLocalDateTimeInputValue(date) {
    // datetime-local expects local wall time without timezone information.
    const offsetMs = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
  }

  return {
    initForm,
    selectAsset,
    loadQuote,
    confirmPurchase
  };
})();
