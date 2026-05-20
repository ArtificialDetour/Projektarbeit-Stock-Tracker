window.PortfolioExport = (function () {
  var utils = window.PortfolioUtils;
  var state = window.PortfolioState;

  // Export current holdings.
  function initExport(elements) {
    if (!elements.exportBtn) return;

    elements.exportBtn.addEventListener('click', function () {
      var holdings = state.getHoldings();
      if (!holdings.length) return;

      var headers = ['Symbol', 'Asset Name', 'First Purchase', 'Quantity', 'Avg Cost Basis', 'Buy-In Amount', 'Current Price', 'Current Value', 'Performance %'];
      var lines = [headers.join(',')];

      holdings.forEach(function (holding) {
        lines.push([
          utils.csvCell(holding.symbol),
          utils.csvCell(holding.assetName),
          utils.csvCell(holding.firstPurchaseDate || ''),
          utils.csvCell(parseFloat(holding.quantity)),
          utils.csvCell(parseFloat(holding.avgCostBasis).toFixed(2)),
          utils.csvCell(parseFloat(holding.costBasisTotal).toFixed(2)),
          utils.csvCell(parseFloat(holding.currentPrice).toFixed(2)),
          utils.csvCell(parseFloat(holding.currentValue).toFixed(2)),
          utils.csvCell(parseFloat(holding.performancePercent).toFixed(2))
        ].join(','));
      });

      var blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
      var url = URL.createObjectURL(blob);
      var anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'portfolio_' + new Date().toISOString().slice(0, 10) + '.csv';
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      URL.revokeObjectURL(url);
    });
  }

  return {
    initExport: initExport
  };
})();
