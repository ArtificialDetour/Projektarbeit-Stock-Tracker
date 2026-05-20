# Test Yahoo Finance API Endpoints

# This script is for maunally testing the Yahoo Finance API connection.

# Start via "./test-yahoo-api.ps1" (windows only)

Write-Host "`n--- Testing Yahoo Finance Chart API ('BTC-USD') ---" -ForegroundColor Cyan
try {
    $Response = Invoke-RestMethod -Uri  'https://query2.finance.yahoo.com/v8/finance/chart/BTC-USD?period1=1609459200&period2=1609545600&interval=1d' -Method Get -Headers @{"User-Agent"="Mozilla/5.0"}
    $Price = $Response.chart.result[0].meta.regularMarketPrice
    $Currency = $Response.chart.result[0].meta.currency
    Write-Host "Success! Current Price for 'BTC-USD': $Price $Currency" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch chart data: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`n--- Testing Yahoo Finance Chart API ('BTC-EUR') ---" -ForegroundColor Cyan
try {
    $Response = Invoke-RestMethod -Uri  'https://query2.finance.yahoo.com/v8/finance/chart/BTC-EUR?period1=1609459200&period2=1609545600&interval=1d' -Method Get -Headers @{"User-Agent"="Mozilla/5.0"}
    $Price = $Response.chart.result[0].meta.regularMarketPrice
    $Currency = $Response.chart.result[0].meta.currency
    Write-Host "Success! Current Price for 'BTC-EUR': $Price $Currency" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch chart data: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`n--- Testing Yahoo Finance Chart API ('AAPL') ---" -ForegroundColor Cyan
try {
    $Response = Invoke-RestMethod -Uri 'https://query2.finance.yahoo.com/v8/finance/chart/AAPL?interval=1d&range=1d' -Method Get -Headers @{"User-Agent"="Mozilla/5.0"}
    $Price = $Response.chart.result[0].meta.regularMarketPrice
    $Currency = $Response.chart.result[0].meta.currency
    Write-Host "Success! Current Price for 'AAPL': $Price $Currency" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch chart data: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`n--- Testing Yahoo Finance Search API ('AAPL') ---" -ForegroundColor Cyan
$SearchUrl = "https://query1.finance.yahoo.com/v1/finance/search?q=AAPL&quotesCount=3"
try {
    $Response = Invoke-RestMethod -Uri $SearchUrl -Method Get -Headers @{"User-Agent"="Mozilla/5.0"}
    Write-Host "Found $($Response.quotes.Count) results:" -ForegroundColor Green
    foreach ($quote in $Response.quotes) {
        Write-Host "- $($quote.symbol): $($quote.longname)"
    }
} catch {
    Write-Host "Failed to search assets: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")



