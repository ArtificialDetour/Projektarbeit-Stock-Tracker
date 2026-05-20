# Test ECB (European Central Bank) API Endpoints

# This script is for manually testing the ECB API connection.

# Start via "./test-ecb-api.ps1" (windows only)

Write-Host "`n--- Testing ECB API: Current Deposit Facility Rate ---" -ForegroundColor Cyan
$CurrentRateUrl = "https://data-api.ecb.europa.eu/service/data/FM/D.U2.EUR.4F.KR.DFR.LEV?format=jsondata&detail=dataonly&lastNObservations=1"
try {
    $Response = Invoke-RestMethod -Uri $CurrentRateUrl -Method Get
    $Series = $Response.dataSets[0].series.'0:0:0:0:0:0:0'
    $Observations = $Series.observations
    $LatestKey = ($Observations.PSObject.Properties.Name | Sort-Object -Descending | Select-Object -First 1)
    $Rate = $Observations.$LatestKey[0]
    $Dates = $Response.structure.dimensions.observation[0].values
    $Date = $Dates[$LatestKey].id
    Write-Host "Success! ECB Deposit Facility Rate: $Rate% (as of $Date)" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch current ECB rate: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`n--- Testing ECB API: Historical DFR Series (full dataset) ---" -ForegroundColor Cyan
$HistoricalUrl = "https://data-api.ecb.europa.eu/service/data/FM/D.U2.EUR.4F.KR.DFR.LEV?format=jsondata"
try {
    $Response = Invoke-RestMethod -Uri $HistoricalUrl -Method Get
    $Observations = $Response.dataSets[0].series.'0:0:0:0:0:0:0'.observations
    $Count = ($Observations.PSObject.Properties | Measure-Object).Count
    $Dates = $Response.structure.dimensions.observation[0].values
    $FirstDate = $Dates[0].id
    $LastDate = $Dates[$Count - 1].id
    Write-Host "Success! Found $Count historical observations from $FirstDate to $LastDate" -ForegroundColor Green
} catch {
    Write-Host "Failed to fetch historical ECB data: $($_.Exception.Message)" -ForegroundColor Red
}


Write-Host "`nPress any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
