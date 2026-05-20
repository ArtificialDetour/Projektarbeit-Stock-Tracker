const path = require('path');

const browserModules = [
  'TransactionUtils',
  'TransactionState',
  'TransactionTable',
  'TransactionPagination',
  'PortfolioUtils',
  'AddAssetUtils',
  'AddAssetState',
  'DashboardUtils',
  'DashboardSummary',
  'DashboardChart',
  'DashboardTooltip',
  'PortfolioChart'
];

function resetBrowserScripts(url = 'http://localhost/') {
  jest.resetModules();
  document.head.innerHTML = '';
  document.body.innerHTML = '';
  window.history.replaceState({}, '', url);

  browserModules.forEach((moduleName) => {
    delete window[moduleName];
  });
}

function loadBrowserScript(relativePath) {
  const absolutePath = path.join(process.cwd(), relativePath);
  delete require.cache[require.resolve(absolutePath)];
  require(absolutePath);
}

module.exports = {
  resetBrowserScripts,
  loadBrowserScript
};
