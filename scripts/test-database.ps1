[CmdletBinding()]
param(
    [string]$EnvFile = '',
    [string]$MySqlExe = '',
    [switch]$SkipQuerySmoke
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'database-utils.ps1')

if (-not $EnvFile) {
    $EnvFile = Join-Path $repoRoot '.env'
}
Import-DotEnv -Path $EnvFile

$MySqlExe = Resolve-MySqlClient -RequestedPath $MySqlExe
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { 'Chengzi' }
$dbPassword = $env:DB_PASSWORD
$adminUser = if ($env:MYSQL_ADMIN_USER) { $env:MYSQL_ADMIN_USER } else { 'root' }
$adminPassword = $env:MYSQL_ADMIN_PASSWORD
$databaseName = if ($env:DB_NAME) { $env:DB_NAME } else { 'anime_platform' }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }
$port = if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }

if (-not $dbPassword -or $dbPassword -eq 'change_me') {
    throw 'DB_PASSWORD must contain the real local application password for tests.'
}

$results = [System.Collections.Generic.List[object]]::new()

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )
    $results.Add([pscustomobject]@{
        Name = $Name
        Passed = $Passed
        Detail = $Detail
    })
    $status = if ($Passed) { 'PASS' } else { 'FAIL' }
    Write-Host "[$status] $Name - $Detail"
}

function Invoke-TestFile {
    param(
        [string]$Name,
        [string]$RelativePath,
        [switch]$CheckAssertionRows
    )

    $path = Join-Path $repoRoot $RelativePath
    $result = Invoke-MySqlFile `
        -MySqlExe $MySqlExe `
        -UserName $dbUser `
        -Password $dbPassword `
        -DbHost $dbHost `
        -Port $port `
        -Database $databaseName `
        -Path $path

    if ($result.ExitCode -ne 0) {
        Add-Result -Name $Name -Passed $false -Detail "exit=$($result.ExitCode)"
        Write-Host $result.Output
        return
    }

    if ($CheckAssertionRows) {
        $failedRows = @(
            $result.Output -split "`r?`n" |
                Where-Object { $_ -match '\tFAIL$' }
        )
        if ($failedRows.Count -gt 0) {
            Add-Result -Name $Name -Passed $false -Detail "$($failedRows.Count) assertion row(s) failed"
            Write-Host $result.Output
            return
        }
    }

    Add-Result -Name $Name -Passed $true -Detail 'SQL executed successfully'
}

function Invoke-ExpectedFailure {
    param(
        [string]$Name,
        [string]$RelativePath,
        [string[]]$ExpectedPatterns
    )

    $path = Join-Path $repoRoot $RelativePath
    $result = Invoke-MySqlFile `
        -MySqlExe $MySqlExe `
        -UserName $dbUser `
        -Password $dbPassword `
        -DbHost $dbHost `
        -Port $port `
        -Database $databaseName `
        -Path $path

    $matched = $result.ExitCode -ne 0
    foreach ($pattern in $ExpectedPatterns) {
        if ($result.Output -notmatch $pattern) {
            $matched = $false
        }
    }

    Add-Result `
        -Name $Name `
        -Passed $matched `
        -Detail "exit=$($result.ExitCode); output=$($result.Output.Trim())"
}

function Invoke-CommandResult {
    param([string]$Sql)
    return Invoke-MySqlText `
        -MySqlExe $MySqlExe `
        -UserName $dbUser `
        -Password $dbPassword `
        -DbHost $dbHost `
        -Port $port `
        -Database $databaseName `
        -Sql $Sql
}

Invoke-TestFile `
    -Name 'Schema and seed integrity assertions' `
    -RelativePath 'database/tests/01_integrity_checks.sql' `
    -CheckAssertionRows

if ($adminPassword) {
    $triggerInventory = Invoke-MySqlText `
        -MySqlExe $MySqlExe `
        -UserName $adminUser `
        -Password $adminPassword `
        -DbHost $dbHost `
        -Port $port `
        -Database $databaseName `
        -Sql @'
SELECT COUNT(*)
FROM information_schema.triggers
WHERE trigger_schema = 'anime_platform';
'@
    Add-Result `
        -Name 'Administrator sees all 12 triggers' `
        -Passed ($triggerInventory.ExitCode -eq 0 -and $triggerInventory.Output.Trim() -eq '12') `
        -Detail "count=$($triggerInventory.Output.Trim())"
}
else {
    Write-Host '[SKIP] Administrator trigger inventory - MYSQL_ADMIN_PASSWORD was not provided.'
}

Invoke-TestFile `
    -Name 'Trigger counter assertions' `
    -RelativePath 'database/tests/02_trigger_tests.sql' `
    -CheckAssertionRows

Invoke-TestFile `
    -Name 'Function, procedure, cursor and transaction assertions' `
    -RelativePath 'database/tests/03_object_tests.sql' `
    -CheckAssertionRows

Invoke-TestFile `
    -Name 'All core and supporting tables CRUD transaction' `
    -RelativePath 'database/crud/core_crud.sql'

Invoke-ExpectedFailure `
    -Name 'Duplicate favorite is rejected' `
    -RelativePath 'database/tests/expected_duplicate_favorite.sql' `
    -ExpectedPatterns @('1062')

Invoke-ExpectedFailure `
    -Name 'Duplicate review is rejected' `
    -RelativePath 'database/tests/expected_duplicate_review.sql' `
    -ExpectedPatterns @('1062')

Invoke-ExpectedFailure `
    -Name 'Rating above 10 is rejected by CHECK' `
    -RelativePath 'database/tests/expected_invalid_rating.sql' `
    -ExpectedPatterns @('3819')

Invoke-ExpectedFailure `
    -Name 'Duplicate title with NULL release date is rejected' `
    -RelativePath 'database/tests/expected_duplicate_null_release_title.sql' `
    -ExpectedPatterns @('1062')

$exceptionResult = Invoke-CommandResult -Sql "CALL sp_transaction_demo(1, 'exception', @tx_result);"
$exceptionPassed = $exceptionResult.ExitCode -ne 0 -and $exceptionResult.Output -match '1644|intentional transaction exception'
Add-Result `
    -Name 'Transaction exception rolls back and raises SIGNAL' `
    -Passed $exceptionPassed `
    -Detail "exit=$($exceptionResult.ExitCode)"

$exceptionLogCount = Invoke-CommandResult -Sql @'
SELECT COUNT(*)
FROM operation_log
WHERE action_type = 'TRANSACTION_DEMO_EXCEPTION';
'@
Add-Result `
    -Name 'Exception transaction left no log row' `
    -Passed ($exceptionLogCount.ExitCode -eq 0 -and $exceptionLogCount.Output.Trim() -eq '0') `
    -Detail "count=$($exceptionLogCount.Output.Trim())"

$token = Get-Date -Format 'yyyyMMddHHmmssfff'
$transientTitle = "DB_TEST_$token"
$addSql = @"
CALL sp_add_anime(
    '$transientTitle', NULL, NULL, 2,
    'transient automated test', NULL, '2099-01-01', 'UPCOMING', @test_anime_id
);
SELECT @test_anime_id;
"@
$addResult = Invoke-CommandResult -Sql $addSql
$transientId = $addResult.Output.Trim()
$addPassed = $addResult.ExitCode -eq 0 -and $transientId -match '^\d+$'
Add-Result `
    -Name 'sp_add_anime commits valid row' `
    -Passed $addPassed `
    -Detail "anime_id=$transientId"

if ($addPassed) {
    $deleteResult = Invoke-CommandResult -Sql "CALL sp_delete_anime($transientId, @deleted_rows); SELECT @deleted_rows;"
    Add-Result `
        -Name 'sp_delete_anime removes transient row' `
        -Passed ($deleteResult.ExitCode -eq 0 -and $deleteResult.Output.Trim() -eq '1') `
        -Detail "deleted=$($deleteResult.Output.Trim())"
}
else {
    Add-Result -Name 'sp_delete_anime removes transient row' -Passed $false -Detail 'skipped because add failed'
}

if (-not $SkipQuerySmoke) {
    Invoke-TestFile `
        -Name '18 complex queries execute' `
        -RelativePath 'database/queries/complex_queries.sql'
}

$dangerPatterns = @(
    'DROP\s+DATABASE',
    'DROP\s+SCHEMA'
)
$databaseAndScripts = @(
    Get-ChildItem -Path (Join-Path $repoRoot 'database') -Recurse -File -Filter '*.sql'
    Get-ChildItem -Path $PSScriptRoot -File -Filter '*.ps1' |
        Where-Object { $_.FullName -ne $PSCommandPath }
)
$dangerHits = @(
    $databaseAndScripts |
        Select-String -Pattern $dangerPatterns -AllMatches
)
Add-Result `
    -Name 'No DROP DATABASE/DROP SCHEMA in database scripts' `
    -Passed ($dangerHits.Count -eq 0) `
    -Detail "matches=$($dangerHits.Count)"

$plainPasswordHits = @(
    Get-ChildItem -Path (Join-Path $repoRoot 'database') -Recurse -File -Filter '*.sql' |
        Select-String -Pattern '(?i)(password|passwd)\s*=\s*[''"]?123456[''"]?'
)
Add-Result `
    -Name 'No hard-coded 123456 database password in SQL' `
    -Passed ($plainPasswordHits.Count -eq 0) `
    -Detail "matches=$($plainPasswordHits.Count)"

$syntaxErrors = [System.Collections.Generic.List[string]]::new()
foreach ($script in Get-ChildItem -Path $PSScriptRoot -File -Filter '*.ps1') {
    $tokens = $null
    $errors = $null
    [Management.Automation.Language.Parser]::ParseFile(
        $script.FullName,
        [ref]$tokens,
        [ref]$errors
    ) | Out-Null
    foreach ($error in $errors) {
        $syntaxErrors.Add("$($script.Name):$($error.Extent.StartLineNumber): $($error.Message)")
    }
}
Add-Result `
    -Name 'PowerShell syntax parse' `
    -Passed ($syntaxErrors.Count -eq 0) `
    -Detail "errors=$($syntaxErrors.Count)"
if ($syntaxErrors.Count -gt 0) {
    $syntaxErrors | ForEach-Object { Write-Host $_ }
}

$failed = @($results | Where-Object { -not $_.Passed })
Write-Host ''
Write-Host "Database test summary: $($results.Count - $failed.Count)/$($results.Count) passed."

if ($failed.Count -gt 0) {
    $failed | ForEach-Object { Write-Host "FAILED: $($_.Name) - $($_.Detail)" }
    exit 1
}

exit 0
