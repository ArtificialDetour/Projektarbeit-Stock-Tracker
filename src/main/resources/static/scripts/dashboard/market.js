window.DashboardMarket = (function () {
  
  // Refresh market headline quotes.
  function updateMarketQuotes() {
    // MC.PA trades in Paris, so the quote can be displayed directly in EUR.
    fetch('/api/stocks/quote/' + encodeURIComponent('MC.PA'))
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        const el = document.getElementById('lvmh-current-price');
        if (!el || !data || !data.price || data.price === 0) return;
        const price = parseFloat(data.price);
        el.textContent = 'MC.PA: ' + price.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '\u20ac';
      })
      .catch(function () {});

    // NDX gives a broad market signal for the dashboard headline card.
    fetch('/api/stocks/quote/' + encodeURIComponent('^NDX'))
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        const el     = document.getElementById('nasdaq-change-text');
        const dateEl = document.getElementById('nasdaq-date');
        if (!el || !data || data.changePercent === undefined) return;
        const pct       = parseFloat(data.changePercent);
        const formatted = Math.abs(pct).toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '%';
        el.textContent = pct < 0
          ? 'NASDAQ 100 dropped -' + formatted
          : 'NASDAQ 100 gained +'  + formatted;
        if (dateEl) {
          const now = new Date();
          dateEl.textContent = now.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        }
      })
      .catch(function () {});
  }

  return {
    updateMarketQuotes
  };
})();
