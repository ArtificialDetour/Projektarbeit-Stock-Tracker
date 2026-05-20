# Test Eurostat (European Commission) API Endpoints

# This script is for manually testing the Eurostat API connection.

# Start via "./test-eurostat-api.ps1" (windows only)

Write-Host "`n--- Testing Eurostat API: German HICP Inflation (monthly, since 2005) ---" -ForegroundColor Cyan
$HicpUrl = "https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_manr?geo=DE&coicop=CP00&freq=M&format=JSON&sinceTimePeriod=2005-01"
try {
    $Response = Invoke-RestMethod -Uri $HicpUrl -Method Get
    $TimePeriods = $Response.dimension.time.category.index.PSObject.Properties | Sort-Object { [int]$_.Value }
    $Count = ($TimePeriods | Measure-Object).Count
    $FirstPeriod = ($TimePeriods | Select-Object -First 1).Name
    $LastPeriod  = ($TimePeriods | Select-Object -Last  1).Name
    $LastIndex   = ($TimePeriods | Select-Object -Last  1).Value
    $LatestRate  = $Response.value.$LastIndex
    Write-Host "Success! Found $Count monthly data points from $FirstPeriod to $LastPeriod" -ForegroundColor Green
    Write-Host "Latest German HICP inflation rate: $LatestRate% (as of $LastPeriod)" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch Eurostat HICP data: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`n--- Testing Eurostat API: Verify query parameters (geo=DE, coicop=CP00, freq=M) ---" -ForegroundColor Cyan
$VerifyUrl = "https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_manr?geo=DE&coicop=CP00&freq=M&format=JSON&sinceTimePeriod=2024-01"
try {
    $Response = Invoke-RestMethod -Uri $VerifyUrl -Method Get
    $Geo    = $Response.dimension.geo.category.label.DE
    $Coicop = $Response.dimension.coicop.category.label.CP00
    Write-Host "Success! Dataset confirmed: geo='$Geo', index='$Coicop'" -ForegroundColor Green
    $Periods = $Response.dimension.time.category.index.PSObject.Properties | Sort-Object { [int]$_.Value }
    foreach ($Period in $Periods) {
        $Rate = $Response.value."$($Period.Value)"
        Write-Host "  $($Period.Name): $Rate%"
    }
} catch {
    Write-Host "Failed to verify Eurostat parameters: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
