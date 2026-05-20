window.TransactionExport = (function () {
  function csvCell(val) {
    var s = String(val == null ? '' : val);
    return s.includes(',') || s.includes('"') || s.includes('\n')
      ? '"' + s.replace(/"/g, '""') + '"'
      : s;
  }

  // Export filtered transactions.
  function initExport(elements) {
    var exportBtn = elements.exportBtn;
    if (!exportBtn) return;

    exportBtn.addEventListener('click', function () {
      var rows = window.TransactionTable.getFiltered();
      if (!rows.length) return;

      var headers = ['Date', 'Time', 'Asset', 'Symbol', 'Type', 'Quantity', 'Price Per Share', 'Total Amount', 'Status'];
      var lines = [headers.join(',')];

      rows.forEach(function (tx) {
        var date = new Date(tx.timestamp);
        lines.push([
          csvCell(date.toLocaleDateString('de-DE')),
          csvCell(date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })),
          csvCell(tx.assetName),
          csvCell(tx.symbol),
          csvCell(tx.transactionType),
          csvCell(parseFloat(tx.quantity)),
          csvCell(parseFloat(tx.pricePerShare).toFixed(2)),
          csvCell(parseFloat(tx.totalCost).toFixed(2)),
          csvCell(tx.status)
        ].join(','));
      });

      var blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
      var url = URL.createObjectURL(blob);
      var anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = 'transactions_' + new Date().toISOString().slice(0, 10) + '.csv';
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
