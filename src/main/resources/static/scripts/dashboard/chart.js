window.DashboardChart = (function () {
  
  // Render dashboard performance chart.
  function renderChart(history, range) {
    if (!history || history.length < 2) return;
    const utils = window.DashboardUtils;
    const tooltip = window.DashboardTooltip;

    const pathEl    = document.getElementById('chart-asset-path');
    const ecbPathEl = document.getElementById('chart-ecb-path');
    const labels    = [0,1,2,3,4].map(function (i) { return document.getElementById('chart-label-' + i); });
    if (!pathEl) return;

    const rawAssetPcts = history.map(function (d) { return parseFloat(d.assetPct); });
    const rawEcbPcts   = history.map(function (d) { return parseFloat(d.ecbPct);   });

    const assetPcts = rawAssetPcts.slice();
    const ecbPcts   = rawEcbPcts.slice();

    // Build a shared Y-axis so portfolio and ECB paths remain visually comparable.
    const combined = assetPcts.concat(ecbPcts);
    let yMin = Math.min.apply(null, combined);
    let yMax = Math.max.apply(null, combined);

    // Pick readable grid steps based on the largest absolute movement.
    const absMax = Math.max(Math.abs(yMin), Math.abs(yMax));
    let step = 1;
    if (absMax > 750) step = 500;
    else if (absMax > 150) step = 100;
    else if (absMax > 40)  step = 25;
    else if (absMax > 15)  step = 10;
    else if (absMax > 5)   step = 5;

    yMin = Math.floor(yMin / step) * step;
    yMax = Math.ceil(yMax / step) * step;

    const pctRange = yMax - yMin || 1;
    const topY = 50, bottomY = 250, totalWidth = 1000;
    const n    = assetPcts.length;

    // Render the portfolio performance path.
    const assetPoints = assetPcts.map(function (v, i) {
      const x = Math.round((i / (n - 1)) * totalWidth);
      const y = Math.round(bottomY - ((v - yMin) / pctRange) * (bottomY - topY));
      return x + ',' + y;
    });
    pathEl.setAttribute('d', 'M' + assetPoints.join(' L'));

    // Render the ECB comparison path on the same scale.
    if (ecbPathEl) {
      const ecbPoints = ecbPcts.map(function (v, i) {
        const x = Math.round((i / (n - 1)) * totalWidth);
        const y = Math.round(bottomY - ((v - yMin) / pctRange) * (bottomY - topY));
        return x + ',' + y;
      });
      ecbPathEl.setAttribute('d', 'M' + ecbPoints.join(' L'));
    }

    // Keep the 0% baseline visible when positive and negative values share the chart.
    const zeroLineEl = document.getElementById('chart-zero-line');
    if (zeroLineEl) {
      const zeroY = Math.round(bottomY - ((0 - yMin) / pctRange) * (bottomY - topY));
      zeroLineEl.setAttribute('y1', zeroY);
      zeroLineEl.setAttribute('y2', zeroY);
    }

    // Spread five timestamp labels evenly across the available candles.
    const steps = [0, Math.floor(n / 4), Math.floor(n / 2), Math.floor(3 * n / 4), n - 1];
    steps.forEach(function (idx, i) {
      if (!labels[i]) return;
      labels[i].textContent = utils.formatTimestamp(history[idx].timestamp, range);
    });

    const endAssetPct = assetPcts[n - 1];
    const endEcbPct   = ecbPcts[n - 1];
    const diffPct     = endAssetPct - endEcbPct;

    const annualEl    = document.getElementById('annualized-return');
    const periodEl    = document.getElementById('time-period-label');
    const targetEl    = document.getElementById('return-from-target');
    const ecbCompEl   = document.getElementById('ecb-comparison-value');
    const ecbLabelEl  = document.getElementById('ecb-rate-label');
    const periodNames = { '1d': '1 Day', '1w': '1 Week', '1m': '1 Month', '3m': '3 Months', '1y': '1 Year', '5y': '5 Years' };

    if (annualEl)  annualEl.textContent  = utils.fmtPct(endAssetPct);
    if (periodEl)  periodEl.textContent  = periodNames[range] || '5 Years';
    if (targetEl)  targetEl.textContent  = utils.fmtPct(endAssetPct) + ' from Target';
    if (ecbCompEl) ecbCompEl.textContent = utils.fmtPct(diffPct);
    if (ecbLabelEl) ecbLabelEl.textContent = 'Relative to ECB Benchmark (' + utils.fmtPct(endEcbPct) + ')';

    if (tooltip) {
      tooltip.initChartTooltip(history, assetPcts, ecbPcts, range, yMin, pctRange);
    }
    updateYAxis(yMin, yMax, pctRange, topY, bottomY);
  }

  // Rebuild chart axis labels.
  function updateYAxis(yMin, yMax, pctRange, topY, bottomY) {
    const axisEl = document.getElementById('chart-y-axis-left');
    const gridEl = document.getElementById('chart-grid-lines');
    if (!axisEl || !gridEl) return;
    axisEl.innerHTML = '';
    gridEl.innerHTML = '';

    const absMax = Math.max(Math.abs(yMin), Math.abs(yMax));
    let step = 1;
    if (absMax > 750) step = 500;
    else if (absMax > 150) step = 100;
    else if (absMax > 40)  step = 25;
    else if (absMax > 15)  step = 10;
    else if (absMax > 5)   step = 5;

    const start = Math.ceil(yMin / step) * step;
    const end   = Math.floor(yMax / step) * step;

    for (let val = start; val <= end; val += step) {
      if (val === 0) continue; 
      const ySvg   = bottomY - ((val - yMin) / pctRange) * (bottomY - topY);
      
      const topPct = (ySvg / 300) * 100;
      const span = document.createElement('span');
      span.className = 'absolute left-0 text-[10px] font-bold text-black font-mono px-1 rounded bg-slate-200/90 backdrop-blur-sm';
      span.style.top = 'calc(' + topPct.toFixed(2) + '% - 7px)';
      span.textContent = (val > 0 ? '+' : '') + val + '%';
      axisEl.appendChild(span);

      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
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

  // Render empty chart state.
  function renderFlatLine() {
    const pathEl    = document.getElementById('chart-asset-path');
    const ecbPathEl = document.getElementById('chart-ecb-path');
    if (pathEl)    pathEl.setAttribute('d',    'M0,150 L1000,150');
    if (ecbPathEl) ecbPathEl.setAttribute('d', 'M0,150 L1000,150');

    const zeroLineEl = document.getElementById('chart-zero-line');
    if (zeroLineEl) {
      zeroLineEl.setAttribute('y1', 150);
      zeroLineEl.setAttribute('y2', 150);
    }
    updateYAxis(-1, 1, 2, 50, 250);
  }

  // Load chart range data.
  function fetchChartData(range) {
    fetch('/api/assets/portfolio-chart?range=' + range)
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (!data || !data.length) { renderFlatLine(); return; }
        renderChart(data, range);
      })
      .catch(function () { renderFlatLine(); });
  }

  // Load default chart data.
  function initChart() {
    fetch('/api/assets/portfolio-chart?range=5y')
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (data) {
        if (data && data.length) {
          renderChart(data, '5y');
        } else {
          renderFlatLine();
        }
      })
      .catch(function () { renderFlatLine(); });
  }

  return {
    renderChart,
    fetchChartData,
    initChart,
    renderFlatLine
  };
})();
