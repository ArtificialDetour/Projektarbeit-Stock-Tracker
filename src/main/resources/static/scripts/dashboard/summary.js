window.DashboardSummary = (function () {
  let prevDashTotal = null;

  // Load dashboard portfolio totals.
  function loadPortfolioSummary() {
    const utils = window.DashboardUtils;
    fetch('/api/assets/holdings')
      .then(r => r.ok ? r.json() : [])
      .then(function (holdings) {
        if (!holdings || !holdings.length) return;
        const totalEl   = document.getElementById('portfolio-total-value');
        const returnEl  = document.getElementById('portfolio-total-return');

        const total     = holdings.reduce(function (s, h) { return s + parseFloat(h.currentValue);   }, 0);
        const totalCost = holdings.reduce(function (s, h) { return s + parseFloat(h.costBasisTotal); }, 0);

        if (totalEl) {
          totalEl.textContent = total.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
          if (prevDashTotal !== null && utils) {
            utils.flashEl(totalEl, total >= prevDashTotal ? 'up' : 'down');
          }
        }
        if (returnEl && totalCost > 0) {
          // Dashboard total return mirrors the portfolio header: current value versus cost basis.
          const totalReturn = (total - totalCost) / totalCost * 100;
          const sign = totalReturn >= 0 ? '+' : '';
          returnEl.textContent = sign + totalReturn.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '% Total Return';
        }
        prevDashTotal = total;

        updateAllocationChart(holdings);
      })
      .catch(function () {});
  }

  // Update allocation donut.
  function updateAllocationChart(data) {
    const donut       = document.getElementById('alloc-donut');
    const centerTotal = document.getElementById('alloc-center-total');
    const stocksPctEl = document.getElementById('alloc-stocks-pct');
    const cryptoPctEl = document.getElementById('alloc-crypto-pct');
    if (!donut) return;

    const CRYPTO_PATTERN = /-USD$|-EUR$|-BTC$|-ETH$|-GBP$|-AUD$|-CAD$/i;
    function isCrypto(symbol) { return CRYPTO_PATTERN.test(symbol); }

    const totalVal = data.reduce(function (s, h) { return s + parseFloat(h.currentValue); }, 0);
    if (totalVal === 0) return;

    const stocksVal = data.filter(function (h) { return !isCrypto(h.symbol); })
                        .reduce(function (s, h) { return s + parseFloat(h.currentValue); }, 0);
    const cryptoVal = totalVal - stocksVal;

    const stocksPct = stocksVal / totalVal * 100;
    const cryptoPct = cryptoVal / totalVal * 100;
    const stocksDeg = (stocksPct / 100 * 360).toFixed(2);

    donut.style.background =
      'conic-gradient(#607cec 0deg ' + stocksDeg + 'deg, #4edea3 ' + stocksDeg + 'deg 360deg)';

    const fmtPctLocal = function (v) {
      return v.toLocaleString('de-DE', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
    };

    if (centerTotal)  centerTotal.textContent  = totalVal.toLocaleString('de-DE', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + ' €';
    if (stocksPctEl)  stocksPctEl.textContent  = fmtPctLocal(stocksPct);
    if (cryptoPctEl)  cryptoPctEl.textContent  = fmtPctLocal(cryptoPct);
  }

  return {
    loadPortfolioSummary
  };
})();
