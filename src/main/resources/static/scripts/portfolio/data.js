window.PortfolioData = (function () {
  var state = window.PortfolioState;

  // Load portfolio holdings.
  function loadHoldings() {
    fetch('/api/assets/holdings')
      .then(function (response) {
        return response.ok ? response.json() : [];
      })
      .then(function (data) {
        var holdings = data || [];
        window.PortfolioTable.clearRows();
        state.setHoldings(holdings);

        if (!holdings.length) {
          window.PortfolioTable.showEmptyState();
          window.PortfolioAllocation.updateAllocationChart([]);
          window.PortfolioInsights.updateInsightCards([]);
          return;
        }

        window.PortfolioTable.hideEmptyState();
        window.PortfolioTable.renderRows(holdings);
        window.PortfolioAllocation.updateAllocationChart(holdings);
        window.PortfolioInsights.updateInsightCards(holdings);

        holdings.forEach(function (holding) {
          state.getPrevValues()[holding.symbol] = {
            currentValue: parseFloat(holding.currentValue),
            performancePercent: parseFloat(holding.performancePercent)
          };
        });

        window.PortfolioTable.updateTotals(holdings, false);

        if (!state.hasPollingStarted()) {
          // Start polling after the first successful load to avoid duplicate intervals.
          state.setPollingStarted(true);
          setInterval(pollHoldings, 30000);
        }

        if (state.getPendingReopenSymbol()) {
          // Reopen selected purchase details after a sale refreshes the holding snapshot.
          var symbol = state.getPendingReopenSymbol();
          state.setPendingReopenSymbol(null);
          var reopenedHolding = state.findHolding(symbol);
          if (reopenedHolding) {
            window.PortfolioTable.selectHolding(symbol);
          }
        }
      })
      .catch(function () {
        window.PortfolioTable.showEmptyState();
      });
  }

  // Refresh holding prices.
  function pollHoldings() {
    fetch('/api/assets/holdings')
      .then(function (response) {
        return response.ok ? response.json() : null;
      })
      .then(function (data) {
        if (!data || !data.length) return;

        state.setHoldings(data);
        if (state.getSelectedSymbol()) {
          window.PortfolioTable.renderSelectedHoldingDetails(state.getSelectedSymbol());
          window.PortfolioChart.loadChart(state.getSelectedSymbol());
        } else {
          data.forEach(window.PortfolioTable.updateRow);
        }
        window.PortfolioAllocation.updateAllocationChart(data);
        window.PortfolioInsights.updateInsightCards(data);
        window.PortfolioTable.updateTotals(data, true);
      })
      .catch(function () {});
  }

  return {
    loadHoldings: loadHoldings,
    pollHoldings: pollHoldings
  };
})();
