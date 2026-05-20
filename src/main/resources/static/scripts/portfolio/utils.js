window.PortfolioUtils = (function () {
  var CRYPTO_PATTERN = /-USD$|-EUR$|-BTC$|-ETH$|-GBP$|-AUD$|-CAD$/i;

  // Detect crypto symbols.
  function isCrypto(symbol) {
    return CRYPTO_PATTERN.test(symbol);
  }

  // Escape HTML text.
  function esc(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  // Format localized number.
  function fmt(val, decimals) {
    return parseFloat(val).toLocaleString('de-DE', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    });
  }

  // Format short date.
  function fmtDate(iso) {
    if (!iso) return '\u2014';
    return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });
  }

  // Format purchase timestamp.
  function fmtDateTime(iso) {
    if (!iso) return '\u2014';
    var date = new Date(iso.endsWith('Z') ? iso : iso + 'Z');
    return date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' })
      + ', ' + date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });
  }

  // Format holding quantity.
  function fmtQty(val) {
    var number = parseFloat(val);
    return number.toLocaleString('de-DE', { minimumFractionDigits: 0, maximumFractionDigits: 6 });
  }

  // Flash changed value.
  function flashEl(el, direction) {
    var flashColor = direction === 'up' ? '#4edea3' : '#ba1a1a';
    var naturalColor = window.getComputedStyle(el).color;
    el.style.transition = 'none';
    el.style.color = flashColor;
    void el.offsetWidth;
    el.style.transition = 'color 1.2s ease-out';
    el.style.color = naturalColor;
    setTimeout(function () {
      el.style.color = '';
      el.style.transition = '';
    }, 1400);
  }

  // Escape CSV cell.
  function csvCell(val) {
    var text = String(val == null ? '' : val);
    return text.includes(',') || text.includes('"') || text.includes('\n')
      ? '"' + text.replace(/"/g, '""') + '"'
      : text;
  }

  return {
    isCrypto: isCrypto,
    esc: esc,
    fmt: fmt,
    fmtDate: fmtDate,
    fmtDateTime: fmtDateTime,
    fmtQty: fmtQty,
    flashEl: flashEl,
    csvCell: csvCell
  };
})();
