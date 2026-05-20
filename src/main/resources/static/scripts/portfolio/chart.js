window.PortfolioChart = (function () {
  var MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];

  function formatTimestamp(iso) {
    var date = new Date(iso);
    return MONTHS[date.getMonth()] + ' ' + String(date.getFullYear()).slice(2);
  }

  function calcStep(absMax) {
    if (absMax > 750) return 500;
    if (absMax > 150) return 100;
    if (absMax > 40)  return 25;
    if (absMax > 15)  return 10;
    if (absMax > 5)   return 5;
    return 1;
  }

  // Rebuild chart axis.
  function updateYAxis(yMin, yMax, pctRange, topY, bottomY) {
    var axisEl = document.getElementById('pf-chart-y-axis-left');
    var gridEl = document.getElementById('pf-chart-grid-lines');
    if (!axisEl || !gridEl) return;
    axisEl.innerHTML = '';
    gridEl.innerHTML = '';

    var step  = calcStep(Math.max(Math.abs(yMin), Math.abs(yMax)));
    var start = Math.ceil(yMin / step) * step;
    var end   = Math.floor(yMax / step) * step;

    for (var val = start; val <= end; val += step) {
      if (val === 0) continue;
      var ySvg   = bottomY - ((val - yMin) / pctRange) * (bottomY - topY);
      var topPct = (ySvg / 300) * 100;

      var span = document.createElement('span');
      span.className = 'absolute left-0 text-[10px] font-bold text-black font-mono px-1 rounded bg-slate-200/90 backdrop-blur-sm';
      span.style.top = 'calc(' + topPct.toFixed(2) + '% - 7px)';
      span.textContent = (val > 0 ? '+' : '') + val + '%';
      axisEl.appendChild(span);

      var line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
      line.setAttribute('x1', '0');
      line.setAttribute('x2', '1000');
      line.setAttribute('y1', ySvg);
      line.setAttribute('y2', ySvg);
      line.setAttribute('stroke', 'currentColor');
      line.setAttribute('stroke-width', '1');
      line.setAttribute('class', 'text-outline-variant/20');
      gridEl.appendChild(line);
    }
  }

  // Render portfolio chart.
  function renderChart(history) {
    if (!history || history.length < 2) return;

    var pathEl        = document.getElementById('pf-chart-asset-path');
    var inflPathEl    = document.getElementById('pf-chart-inflation-path');
    var cumInflPathEl = document.getElementById('pf-chart-cumInfl-path');
    var labels        = [0,1,2,3,4].map(function (i) { return document.getElementById('pf-chart-label-' + i); });
    if (!pathEl) return;

    var rawAsset   = history.map(function (d) { return parseFloat(d.assetPct); });
    var rawReal    = history.map(function (d) { return parseFloat(d.realReturnPct != null ? d.realReturnPct : d.assetPct); });
    var rawCumInfl = history.map(function (d) { return parseFloat(d.inflationPct || 0); });

    var assetPcts   = rawAsset.slice();
    var inflPcts    = rawReal.slice();
    var cumInflPcts = rawCumInfl.slice();

    var combined = assetPcts.concat(inflPcts).concat(cumInflPcts);
    var yMin = Math.min.apply(null, combined);
    var yMax = Math.max.apply(null, combined);

    var step = calcStep(Math.max(Math.abs(yMin), Math.abs(yMax)));
    yMin = Math.floor(yMin / step) * step;
    yMax = Math.ceil(yMax / step)  * step;

    var pctRange = yMax - yMin || 1;
    var topY = 50, bottomY = 250, W = 1000, n = assetPcts.length;

    var toPoint = function (v, i) {
      return Math.round((i / (n - 1)) * W) + ',' +
             Math.round(bottomY - ((v - yMin) / pctRange) * (bottomY - topY));
    };

    pathEl.setAttribute('d', 'M' + assetPcts.map(toPoint).join(' L'));

    if (inflPathEl) {
      inflPathEl.setAttribute('d', 'M' + inflPcts.map(toPoint).join(' L'));
    }

    if (cumInflPathEl) {
      cumInflPathEl.setAttribute('d', 'M' + cumInflPcts.map(toPoint).join(' L'));
    }

    var zeroLineEl = document.getElementById('pf-chart-zero-line');
    if (zeroLineEl) {
      var zeroY = Math.round(bottomY - ((0 - yMin) / pctRange) * (bottomY - topY));
      zeroLineEl.setAttribute('y1', zeroY);
      zeroLineEl.setAttribute('y2', zeroY);
    }

    var steps = [0, Math.floor(n / 4), Math.floor(n / 2), Math.floor(3 * n / 4), n - 1];
    steps.forEach(function (idx, i) {
      if (labels[i]) labels[i].textContent = formatTimestamp(history[idx].timestamp);
    });

    updateYAxis(yMin, yMax, pctRange, topY, bottomY);
  }

  // Render empty chart state.
  function renderFlatLine() {
    var pathEl        = document.getElementById('pf-chart-asset-path');
    var inflPathEl    = document.getElementById('pf-chart-inflation-path');
    var cumInflPathEl = document.getElementById('pf-chart-cumInfl-path');
    if (pathEl)        pathEl.setAttribute('d',        'M0,150 L1000,150');
    if (inflPathEl)    inflPathEl.setAttribute('d',    'M0,150 L1000,150');
    if (cumInflPathEl) cumInflPathEl.setAttribute('d', 'M0,150 L1000,150');

    var zeroLineEl = document.getElementById('pf-chart-zero-line');
    if (zeroLineEl) { zeroLineEl.setAttribute('y1', 150); zeroLineEl.setAttribute('y2', 150); }

    updateYAxis(-1, 1, 2, 50, 250);
  }

  // Load portfolio chart.
  function loadChart(symbol) {
    var url = '/api/assets/portfolio-chart?range=5y';
    if (symbol) {
      url += '&symbol=' + encodeURIComponent(symbol);
    }

    return fetch(url, { cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (data && data.length) { renderChart(data); } else { renderFlatLine(); }
      })
      .catch(function () { renderFlatLine(); });
  }

  return { loadChart: loadChart, renderChart: renderChart, renderFlatLine: renderFlatLine };
})();
