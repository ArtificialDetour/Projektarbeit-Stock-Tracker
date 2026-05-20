(function () {
  // Bootstrap add-asset modal.
  const utils = window.AddAssetUtils;
  const state = window.AddAssetState;
  const api = window.AddAssetApi;
  const ui = window.AddAssetUi;
  const form = window.AddAssetForm;

  if (!utils || !state || !api || !ui || !form) {
    console.error('Add asset modules not loaded correctly.');
    return;
  }

  const elements = {
    modal: document.getElementById('add-asset-modal'),
    backdrop: document.getElementById('add-asset-backdrop'),
    closeBtn: document.getElementById('add-asset-close'),
    searchEl: document.getElementById('add-asset-search'),
    resultsList: document.getElementById('add-asset-results'),
    infoBox: document.getElementById('add-asset-info'),
    nameEl: document.getElementById('add-asset-name'),
    badgeEl: document.getElementById('add-asset-symbol-badge'),
    priceEl: document.getElementById('add-asset-price'),
    currencyEl: document.getElementById('add-asset-currency'),
    qtyEl: document.getElementById('add-asset-qty'),
    totalEl: document.getElementById('add-asset-total'),
    editOptionsEl: document.getElementById('add-asset-edit-options'),
    editFieldsEl: document.getElementById('add-asset-edit-fields'),
    customPriceEl: document.getElementById('add-asset-custom-price'),
    customTimestampEl: document.getElementById('add-asset-custom-timestamp'),
    errorEl: document.getElementById('add-asset-error'),
    confirmBtn: document.getElementById('add-asset-confirm')
  };

  if (!elements.modal) return;

  ui.initUi(elements);
  form.initForm();
})();
