window.TransactionUtils = (function () {
  function esc(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function fmt(val, decimals) {
    return parseFloat(val).toLocaleString('de-DE', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    });
  }

  function fmtDate(iso) {
    return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });
  }

  function fmtTime(iso) {
    return new Date(iso).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  }

  function fmtQty(val) {
    var n = parseFloat(val);
    return n.toLocaleString('de-DE', { minimumFractionDigits: 0, maximumFractionDigits: 6 });
  }

  function typeBadge(type) {
    var label = type === 'BUY' ? 'Buy Order' : type === 'SELL' ? 'Sell Order' : type;
    var cls = type === 'BUY'
      ? 'bg-secondary-container text-on-secondary-container'
      : type === 'SELL'
        ? 'bg-error-container text-on-error-container'
        : 'bg-surface-container-high text-on-surface-variant';
    return '<span class="text-xs font-bold font-manrope px-2.5 py-1 rounded-full whitespace-nowrap ' + cls + '">' + esc(label) + '</span>';
  }

  function statusBadge(status) {
    var cls = status === 'SETTLED' ? 'bg-tertiary-fixed text-on-tertiary-fixed' : 'bg-secondary-fixed text-on-secondary-fixed';
    var label = status === 'SETTLED' ? 'Settled' : 'Pending';
    return '<span class="px-3 py-1 ' + cls + ' text-[10px] font-bold rounded-full uppercase tracking-tighter">' + label + '</span>';
  }

  function profitLossCell(tx) {
    if (tx.transactionType !== 'SELL' || tx.realizedGain === null || tx.realizedGain === undefined) {
      return '';
    }

    var gain = parseFloat(tx.realizedGain);
    if (isNaN(gain)) return '';

    var cls = gain >= 0 ? 'text-green-600' : 'text-red-600';
    var sign = gain > 0 ? '+' : '';
    return '<span class="text-sm font-extrabold ' + cls + '">' + sign + fmt(gain, 2) + '</span>';
  }

  function buildSearchText(tx) {
    return [fmtDate(tx.timestamp), esc(tx.assetName), tx.symbol, tx.transactionType, tx.status]
      .join(' ')
      .toLowerCase();
  }

  // Build transaction row markup.
  function renderRow(tx) {
    var isBuy = tx.transactionType === 'BUY';
    var totalClass = isBuy ? 'text-on-surface' : 'text-on-tertiary-container';

    return '<tr class="hover:bg-surface-container-low/30 transition-colors group"'
      + ' data-search="' + buildSearchText(tx) + '"'
      + ' data-tx-type="' + esc(tx.transactionType) + '">'
      + '<td class="px-6 py-5"><div class="flex flex-col">'
      + '<span class="text-sm font-bold text-on-surface">' + esc(fmtDate(tx.timestamp)) + '</span>'
      + '<span class="text-[10px] text-on-surface-variant uppercase font-medium">' + esc(fmtTime(tx.timestamp)) + '</span>'
      + '</div></td>'
      + '<td class="px-6 py-5"><div class="flex flex-col">'
      + '<span class="text-sm font-semibold">' + esc(tx.assetName) + '</span>'
      + '<span class="text-xs text-on-surface-variant font-mono">' + esc(tx.symbol) + '</span>'
      + '</div></td>'
      + '<td class="px-6 py-5">' + typeBadge(tx.transactionType) + '</td>'
      + '<td class="px-6 py-5 text-right font-body text-sm">' + fmtQty(tx.quantity) + '</td>'
      + '<td class="px-6 py-5 text-right font-body text-sm">' + fmt(tx.pricePerShare, 2) + '</td>'
      + '<td class="px-6 py-5 text-right"><span class="text-sm font-extrabold ' + totalClass + '">'
      + fmt(tx.quantity * tx.pricePerShare, 2)
      + '</span></td>'
      + '<td class="px-3 py-5 text-right">' + profitLossCell(tx) + '</td>'
      + '<td class="px-6 py-5"><div class="flex justify-center">' + statusBadge(tx.status) + '</div></td>'
      + '</tr>';
  }

  return {
    esc: esc,
    fmt: fmt,
    fmtDate: fmtDate,
    fmtTime: fmtTime,
    fmtQty: fmtQty,
    typeBadge: typeBadge,
    statusBadge: statusBadge,
    profitLossCell: profitLossCell,
    buildSearchText: buildSearchText,
    renderRow: renderRow
  };
})();
