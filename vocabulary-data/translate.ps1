$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$selected = Get-Content -Raw -LiteralPath (Join-Path $root 'selected-source.json') | ConvertFrom-Json
$cachePath = Join-Path $root 'translation-cache.json'
$cache = if (Test-Path -LiteralPath $cachePath) { Get-Content -Raw -LiteralPath $cachePath | ConvertFrom-Json -AsHashtable } else { @{} }
$allTerms = @($selected.PSObject.Properties.Value | ForEach-Object { $_ } | ForEach-Object { if ($_.pos -eq 'verb') { 'to ' + $_.english } else { $_.english } } | Sort-Object -Unique)
$pending = @($allTerms | Where-Object { -not $cache.ContainsKey($_) })
for ($offset = 0; $offset -lt $pending.Count; $offset += 30) {
    $batch = @($pending[$offset..([Math]::Min($offset + 29, $pending.Count - 1))])
    $lines = for ($index = 0; $index -lt $batch.Count; $index++) { ($index + 1).ToString() + '. ' + $batch[$index] }
    $query = [uri]::EscapeDataString(($lines -join "`n"))
    $response = Invoke-RestMethod -Uri ('https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=uz&dt=t&q=' + $query) -TimeoutSec 30
    $translated = ($response[0] | ForEach-Object { $_[0] }) -join ''
    $matchesFound = [regex]::Matches($translated, '(?ms)^\s*(\d+)\s*[.)]\s*(.*?)(?=^\s*\d+\s*[.)]|\z)')
    if ($matchesFound.Count -ne $batch.Count) { throw ('Translation line count mismatch at ' + $offset) }
    foreach ($match in $matchesFound) { $cache[$batch[[int]$match.Groups[1].Value - 1]] = $match.Groups[2].Value.Trim() }
    $cache | ConvertTo-Json -Depth 3 | Set-Content -Encoding utf8 -LiteralPath $cachePath
    Write-Output ('Translated ' + $cache.Count + '/' + $allTerms.Count)
    Start-Sleep -Milliseconds 1200
}
