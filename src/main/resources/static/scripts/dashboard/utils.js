window.DashboardUtils = (function () {
  const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
  const rangeMap = { '1D': '1d', '1W': '1w', '1M': '1m', '3M': '3m', '1Y': '1y', '5Y': '5y' };

  function flashEl(el, direction) {
    const color = direction === 'up' ? '#4edea3' : '#ba1a1a';
    el.style.transition = '';
    el.style.color = color;
    setTimeout(function () {
      el.style.transition = 'color 1.2s ease-out';
      el.style.color = '';
    }, 100);
  }

  function formatTimestamp(iso, range) {
    const d     = new Date(iso);
    const day   = d.getDate();
    const month = MONTHS[d.getMonth()];
    const year  = d.getFullYear();
    const hh    = String(d.getHours()).padStart(2, '0');
    const mm    = String(d.getMinutes()).padStart(2, '0');

    if (range === '1d') {
      return hh + ':' + mm;
    }
    if (range === '1w') {
      return month.charAt(0) + month.slice(1).toLowerCase() + ' ' + day + ' ' + hh + ':' + mm;
    }
    if (range === '1m' || range === '3m') {
      return month.charAt(0) + month.slice(1).toLowerCase() + ' ' + (day < 10 ? '0' + day : day);
    }
    return month + ' ' + String(year).slice(2);
  }

  function fmtPct(v) {
    const sign = v >= 0 ? '+' : '';
    return sign + v.toLocaleString('de-DE', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + '%';
  }

  return {
    MONTHS,
    rangeMap,
    flashEl,
    formatTimestamp,
    fmtPct
  };
})();
