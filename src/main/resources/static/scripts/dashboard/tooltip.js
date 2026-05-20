window.DashboardTooltip = (function () {
  let _chartMoveHandler  = null;
  let _chartLeaveHandler = null;

  // Attach chart tooltip behavior.
  function initChartTooltip(history, assetPcts, ecbPcts, range, yMin, pctRange) {
    const utils = window.DashboardUtils;
    const wrap    = document.getElementById('chart-wrap');
    const tooltip = document.getElementById('chart-tooltip');
    const dateEl  = document.getElementById('chart-tooltip-date');
    const valEl   = document.getElementById('chart-tooltip-value');
    const chgEl   = document.getElementById('chart-tooltip-change');
    const hoverLine = document.getElementById('chart-hover-line');
    if (!wrap || !tooltip || !dateEl || !valEl || !chgEl) return;

    if (_chartMoveHandler)  wrap.removeEventListener('mousemove',  _chartMoveHandler);
    if (_chartLeaveHandler) wrap.removeEventListener('mouseleave', _chartLeaveHandler);

    const n = assetPcts.length;

    _chartMoveHandler = function (e) {
      const rect = wrap.getBoundingClientRect();
      const pct  = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
      const idx  = Math.round(pct * (n - 1));

      const assetVal = assetPcts[idx];
      const ecbVal   = ecbPcts[idx];
      const diff     = assetVal - ecbVal;

      const d       = new Date(history[idx].timestamp);
      const day     = d.getDate();
      const month   = utils.MONTHS[d.getMonth()];
      const year    = d.getFullYear();
      const hh      = String(d.getHours()).padStart(2, '0');
      const mmStr   = String(d.getMinutes()).padStart(2, '0');
      const dateStr = day + ' ' + month.charAt(0) + month.slice(1).toLowerCase() + ' ' + year + ', ' + hh + ':' + mmStr;

      dateEl.textContent = dateStr;
      valEl.textContent  = 'Asset ' + utils.fmtPct(assetVal) + '  |  ECB ' + utils.fmtPct(ecbVal);
      chgEl.textContent  = '\u0394 ' + utils.fmtPct(diff);
      chgEl.className    = 'text-xs font-bold ' + (diff >= 0 ? 'text-tertiary-fixed-dim' : 'text-error');

      const tipW = tooltip.offsetWidth;
      const tipH = tooltip.offsetHeight;

      let leftPx = (pct * rect.width) - tipW / 2;
      leftPx = Math.max(0, Math.min(rect.width - tipW, leftPx));

      const svgY  = 250 - ((assetVal - yMin) / (pctRange || 1)) * 200;
      let topPx = (svgY / 300) * rect.height - tipH / 2 - 65;
      topPx = Math.max(0, Math.min(rect.height - tipH, topPx));

      tooltip.style.left    = leftPx + 'px';
      tooltip.style.top     = topPx  + 'px';
      tooltip.style.opacity = '1';

      if (hoverLine) {
        const xSvg = pct * 1000;
        const tooltipBottomSvgY = ((topPx + tipH) / rect.height) * 300;
        
        hoverLine.setAttribute('x1', xSvg);
        hoverLine.setAttribute('x2', xSvg);
        hoverLine.setAttribute('y1', svgY);
        hoverLine.setAttribute('y2', tooltipBottomSvgY);
        hoverLine.style.opacity = '1';
      }
    };

    _chartLeaveHandler = function () { 
      tooltip.style.opacity = '0'; 
      if (hoverLine) hoverLine.style.opacity = '0';
    };

    wrap.addEventListener('mousemove',  _chartMoveHandler);
    wrap.addEventListener('mouseleave', _chartLeaveHandler);
  }

  return {
    initChartTooltip
  };
})();
