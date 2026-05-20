window.PortfolioAllocation = (function () {
  var utils = window.PortfolioUtils;

  // Update allocation donut.
  function updateAllocationChart(data) {
    var donut = document.getElementById('alloc-donut');
    var centerTotal = document.getElementById('alloc-center-total');
    var stocksPctEl = document.getElementById('alloc-stocks-pct');
    var cryptoPctEl = document.getElementById('alloc-crypto-pct');
    if (!donut) return;

    var totalVal = data.reduce(function (sum, holding) {
      return sum + parseFloat(holding.currentValue);
    }, 0);
    if (totalVal === 0) return;

    var stocksVal = data.filter(function (holding) {
      return !utils.isCrypto(holding.symbol);
    }).reduce(function (sum, holding) {
      return sum + parseFloat(holding.currentValue);
    }, 0);
    var cryptoVal = totalVal - stocksVal;

    var stocksPct = stocksVal / totalVal * 100;
    var cryptoPct = cryptoVal / totalVal * 100;
    var stocksDeg = (stocksPct / 100 * 360).toFixed(2);

    donut.style.background =
      'conic-gradient(#607cec 0deg ' + stocksDeg + 'deg, #4edea3 ' + stocksDeg + 'deg 360deg)';

    var fmtPct = function (value) {
      return value.toLocaleString('de-DE', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
    };

    if (centerTotal) centerTotal.textContent = totalVal.toLocaleString('de-DE', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + ' €';
    if (stocksPctEl) stocksPctEl.textContent = fmtPct(stocksPct);
    if (cryptoPctEl) cryptoPctEl.textContent = fmtPct(cryptoPct);
  }

  return {
    updateAllocationChart: updateAllocationChart
  };
})();
