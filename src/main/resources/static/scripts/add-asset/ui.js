window.AddAssetUi = (function () {
  let els = null;
  const state = window.AddAssetState;
  const utils = window.AddAssetUtils;

  // Store modal elements.
  function initUi(elements) {
    els = elements;
  }

  // Open purchase modal.
  function openModal() {
    resetModal();
    els.modal.classList.remove('hidden');
    els.searchEl.focus();
    document.body.style.overflow = 'hidden';
  }

  function closeModal() {
    els.modal.classList.add('hidden');
    document.body.style.overflow = '';
  }

  // Reset purchase modal.
  function resetModal() {
    state.reset();
    els.searchEl.value = '';
    els.qtyEl.value = '';
    if (els.editOptionsEl) els.editOptionsEl.checked = false;
    if (els.editFieldsEl) els.editFieldsEl.classList.add('hidden');
    if (els.customPriceEl) els.customPriceEl.value = '';
    if (els.customTimestampEl) els.customTimestampEl.value = '';
    els.totalEl.textContent = '\u2014';
    els.infoBox.classList.add('hidden');
    els.resultsList.classList.add('hidden');
    els.resultsList.innerHTML = '';
    hideError();
    els.confirmBtn.disabled = true;
    els.confirmBtn.textContent = 'Confirm Purchase';
  }

  // Render search results.
  function renderResults(items) {
    els.resultsList.innerHTML = '';
    if (!items || items.length === 0) {
      els.resultsList.classList.add('hidden');
      return;
    }

    items.forEach(item => {
      const li = document.createElement('li');
      li.className = 'px-4 py-2.5 cursor-pointer hover:bg-surface-container flex items-center justify-between gap-2 text-sm';
      li.innerHTML =
        '<span class="text-on-surface font-medium truncate">' + utils.escHtml(item.name || item.symbol) + '</span>' +
        '<span class="text-xs text-on-surface-variant font-mono shrink-0">' + utils.escHtml(item.symbol) + '</span>';
      li.addEventListener('click', () => window.AddAssetForm.selectAsset(item));
      els.resultsList.appendChild(li);
    });

    els.resultsList.classList.remove('hidden');
  }

  // Render selected quote.
  function renderQuote(symbol, data) {
    const currentPrice = parseFloat(data.currentPrice) || 0;
    const selectedName = data.name || state.getSelectedName();

    state.setCurrentPrice(currentPrice);
    state.setCurrentCurrency(data.currency || 'USD');
    state.setSelectedName(selectedName);

    els.nameEl.textContent = selectedName;
    els.badgeEl.textContent = symbol;
    els.priceEl.textContent = utils.formatCurrency(currentPrice);
    els.currencyEl.textContent = 'EUR';
    els.infoBox.classList.remove('hidden');
  }

  function setLoadingQuote() {
    els.infoBox.classList.add('hidden');
    els.confirmBtn.disabled = true;
  }

  // Recalculate purchase total.
  function updateTotal() {
    const qty = parseFloat(els.qtyEl.value);
    const price = getEffectivePrice();
    if (!state.getSelectedSymbol() || isNaN(qty) || qty <= 0 || price <= 0) {
      els.totalEl.textContent = '\u2014';
      els.confirmBtn.disabled = true;
      return;
    }

    const total = qty * price;
    els.totalEl.textContent = utils.formatCurrency(total) + ' \u20AC';
    els.confirmBtn.disabled = false;
    hideError();
  }

  function getEffectivePrice() {
    if (isEditOptionsEnabled()) {
      const customPrice = parseFloat(els.customPriceEl.value);
      return isNaN(customPrice) ? 0 : customPrice;
    }
    return state.getCurrentPrice();
  }

  function isEditOptionsEnabled() {
    return !!(els.editOptionsEl && els.editOptionsEl.checked);
  }

  function setEditOptionsEnabled(enabled) {
    if (els.editFieldsEl) {
      els.editFieldsEl.classList.toggle('hidden', !enabled);
    }
  }

  function showError(message) {
    els.errorEl.textContent = message;
    els.errorEl.classList.remove('hidden');
  }

  function hideError() {
    els.errorEl.classList.add('hidden');
    els.errorEl.textContent = '';
  }

  function setConfirmProcessing() {
    els.confirmBtn.disabled = true;
    els.confirmBtn.textContent = 'Processing...';
  }

  function resetConfirmText() {
    els.confirmBtn.textContent = 'Confirm Purchase';
  }

  // Show purchase success.
  function showSuccessBanner(transaction) {
    const banner = document.createElement('div');
    banner.className = 'fixed bottom-6 right-6 z-50 bg-surface-container-lowest border border-outline-variant rounded-xl shadow-xl px-5 py-4 flex items-center gap-3 text-sm text-on-surface';
    const qty = transaction?.quantity ?? '';
    const symbol = transaction?.symbol ?? '';
    const total = transaction?.totalCost != null
      ? utils.formatCurrency(transaction.totalCost) + ' \u20AC'
      : '';

    banner.innerHTML =
      '<span class="material-symbols-outlined text-tertiary-fixed-dim">check_circle</span>' +
      '<span>Bought <strong>' + utils.escHtml(String(qty)) + ' \u00D7 ' + utils.escHtml(symbol) + '</strong>' +
      (total ? ' for <strong>' + utils.escHtml(total) + '</strong>' : '') + '</span>';
    document.body.appendChild(banner);
    setTimeout(() => banner.remove(), 4000);
  }

  function getElements() {
    return els;
  }

  return {
    initUi,
    openModal,
    closeModal,
    resetModal,
    renderResults,
    renderQuote,
    setLoadingQuote,
    updateTotal,
    getEffectivePrice,
    isEditOptionsEnabled,
    setEditOptionsEnabled,
    showError,
    hideError,
    setConfirmProcessing,
    resetConfirmText,
    showSuccessBanner,
    getElements
  };
})();
