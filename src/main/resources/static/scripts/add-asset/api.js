window.AddAssetApi = (function () {
  // Search tradable assets.
  function searchAssets(query) {
    return fetch('/api/assets/search?q=' + encodeURIComponent(query))
      .then(response => response.ok ? response.json() : []);
  }

  // Load latest asset quote.
  function loadQuote(symbol) {
    return fetch('/api/assets/quote/' + encodeURIComponent(symbol))
      .then(response => response.ok ? response.json() : null);
  }

  // Submit purchase request.
  function buyAsset(symbol, quantity, expectedPrice, options) {
    return fetch('/api/assets/buy', {
      method: 'POST',
      headers: window.AddAssetUtils.getCsrfHeaders(),
      body: JSON.stringify({
        symbol,
        quantity,
        expectedPrice,
        ...(options || {})
      })
    });
  }

  return {
    searchAssets,
    loadQuote,
    buyAsset
  };
})();
