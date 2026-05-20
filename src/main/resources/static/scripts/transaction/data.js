window.TransactionData = (function () {
  // Load transaction list.
  function loadTransactions() {
    return fetch('/api/assets/transactions')
      .then(function (response) {
        return response.ok ? response.json() : [];
      })
      .then(function (data) {
        window.TransactionState.setAllData(data || []);

        if (window.TransactionState.getAllData().length === 0) {
          window.TransactionTable.showNoResults();
          window.TransactionTable.setCount('0 transactions');
          return;
        }

        window.TransactionTable.hideNoResults();
        window.TransactionTable.renderPage();
      })
      .catch(function () {
        window.TransactionTable.showNoResults();
      });
  }

  return {
    loadTransactions: loadTransactions
  };
})();
