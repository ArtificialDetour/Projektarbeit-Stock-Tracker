window.PortfolioInsights = (function () {
  var utils = window.PortfolioUtils;

  // Update insight cards.
  function updateInsightCards(data) {
    if (!data.length) return;

    var sorted = data.slice().sort(function (a, b) {
      return parseFloat(b.performancePercent) - parseFloat(a.performancePercent);
    });
    var best = sorted[0];
    var worst = sorted[sorted.length - 1];

    var fmtSign = function (value) {
      return (value >= 0 ? '+' : '') + utils.fmt(value, 2) + '%';
    };
    var fmtAbs = function (holding) {
      var abs = parseFloat(holding.currentValue) - parseFloat(holding.costBasisTotal);
      return (abs >= 0 ? '+' : '') + utils.fmt(abs, 2) + ' \u20AC';
    };

    var bestCard = document.getElementById('card-best-performer');
    var worstCard = document.getElementById('card-worst-performer');
    var highlightCard = document.getElementById('card-highlights');

    if (bestCard) {
      document.getElementById('best-name').textContent = best.assetName + ' (' + best.symbol + ')';
      document.getElementById('best-pct').textContent = fmtSign(parseFloat(best.performancePercent));
      document.getElementById('best-abs').textContent = fmtAbs(best);
      bestCard.classList.remove('hidden');
    }

    if (worstCard) {
      document.getElementById('worst-name').textContent = worst.assetName + ' (' + worst.symbol + ')';
      document.getElementById('worst-pct').textContent = fmtSign(parseFloat(worst.performancePercent));
      document.getElementById('worst-abs').textContent = fmtAbs(worst);
      worstCard.classList.remove('hidden');
    }

    if (highlightCard) {
      var withWeekly = data.filter(function (holding) {
        return holding.weeklyChangePercent != null;
      });
      if (withWeekly.length) {
        var weeklySorted = withWeekly.slice().sort(function (a, b) {
          return parseFloat(b.weeklyChangePercent) - parseFloat(a.weeklyChangePercent);
        });
        var weeklyBest = weeklySorted[0];
        var weeklyWorst = weeklySorted[weeklySorted.length - 1];
        document.getElementById('highlight-best-name').textContent = weeklyBest.assetName;
        document.getElementById('highlight-best-pct').textContent = fmtSign(parseFloat(weeklyBest.weeklyChangePercent));
        document.getElementById('highlight-worst-name').textContent = weeklyWorst.assetName;
        document.getElementById('highlight-worst-pct').textContent = fmtSign(parseFloat(weeklyWorst.weeklyChangePercent));
        highlightCard.classList.remove('hidden');
      }
    }
  }

  return {
    updateInsightCards: updateInsightCards
  };
})();
