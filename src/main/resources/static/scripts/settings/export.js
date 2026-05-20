window.SettingsExport = (function () {
    function csvCell(value) {
        const cell = String(value == null ? '' : value);
        return cell.includes(',') || cell.includes('"') || cell.includes('\n')
            ? '"' + cell.replace(/"/g, '""') + '"'
            : cell;
    }

    // Download CSV content.
    function downloadCsv(lines, filename) {
        // Prefix the CSV with a BOM so Excel opens UTF-8 exports correctly.
        const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = filename;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
    }

    // Export current holdings.
    async function exportPortfolio() {
        const response = await fetch('/api/assets/holdings');
        if (!response.ok) return;

        const holdings = await response.json();
        if (!holdings || !holdings.length) return;

        const headers = ['Symbol', 'Asset Name', 'First Purchase', 'Quantity', 'Avg Cost Basis', 'Cost Basis Total', 'Current Price', 'Current Value', 'Performance %'];
        const lines = [headers.join(',')];

        holdings.forEach((holding) => {
            lines.push([
                csvCell(holding.symbol),
                csvCell(holding.assetName),
                csvCell(holding.firstPurchaseDate || ''),
                csvCell(parseFloat(holding.quantity)),
                csvCell(parseFloat(holding.avgCostBasis).toFixed(2)),
                csvCell(parseFloat(holding.costBasisTotal).toFixed(2)),
                csvCell(parseFloat(holding.currentPrice).toFixed(2)),
                csvCell(parseFloat(holding.currentValue).toFixed(2)),
                csvCell(parseFloat(holding.performancePercent).toFixed(2))
            ].join(','));
        });

        downloadCsv(lines, 'portfolio_' + new Date().toISOString().slice(0, 10) + '.csv');
    }

    // Export transaction history.
    async function exportTransactions() {
        const response = await fetch('/api/assets/transactions');
        if (!response.ok) return;

        const transactions = await response.json();
        if (!transactions || !transactions.length) return;

        const headers = ['Date', 'Time', 'Asset', 'Symbol', 'Type', 'Quantity', 'Price Per Share', 'Total Amount', 'Status'];
        const lines = [headers.join(',')];

        transactions.forEach((transaction) => {
            const date = new Date(transaction.timestamp);
            lines.push([
                csvCell(date.toLocaleDateString('de-DE')),
                csvCell(date.toLocaleTimeString('de-DE', { hour: '2-digit', minute: '2-digit' })),
                csvCell(transaction.assetName),
                csvCell(transaction.symbol),
                csvCell(transaction.transactionType),
                csvCell(parseFloat(transaction.quantity)),
                csvCell(parseFloat(transaction.pricePerShare).toFixed(2)),
                csvCell(parseFloat(transaction.totalCost).toFixed(2)),
                csvCell(transaction.status)
            ].join(','));
        });

        downloadCsv(lines, 'transactions_' + new Date().toISOString().slice(0, 10) + '.csv');
    }

    // Record export activity.
    async function logExportActivity() {
        await fetch('/api/activities/log-export', {
            method: 'POST',
            headers: { ...(window.getCsrfHeaders ? window.getCsrfHeaders() : {}) }
        });
    }

    // Wire export action.
    function initDataExport() {
        const exportDataBtn = document.getElementById('settings-export-data-button');
        if (!exportDataBtn) return;

        exportDataBtn.addEventListener('click', async () => {
            const btnTextContainer = exportDataBtn.querySelector('p.text-sm');
            const originalText = btnTextContainer ? btnTextContainer.textContent : '';

            if (btnTextContainer) {
                btnTextContainer.textContent = 'Exporting...';
            }
            exportDataBtn.style.pointerEvents = 'none';
            exportDataBtn.style.opacity = '0.7';

            try {
                await exportPortfolio();
                // A short pause prevents browsers from collapsing both downloads into one gesture.
                await new Promise((resolve) => setTimeout(resolve, 600));
                await exportTransactions();
                await logExportActivity();
            } catch (error) {
                console.error('Export failed', error);
            } finally {
                if (btnTextContainer) {
                    btnTextContainer.textContent = originalText;
                }
                exportDataBtn.style.pointerEvents = '';
                exportDataBtn.style.opacity = '';
            }
        });
    }

    return {
        initDataExport
    };
})();
