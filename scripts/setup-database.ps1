[CmdletBinding()]
param(
    [string]$EnvFile = '',
    [string]$MySqlExe = '',
    [switch]$SkipBootstrap,
    [switch]$AllowAdditionalTables
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'database-utils.ps1')

if (-not $EnvFile) {
    $EnvFile = Join-Path $repoRoot '.env'
}
Import-DotEnv -Path $EnvFile

$MySqlExe = Resolve-MySqlClient -RequestedPath $MySqlExe
$adminUser = if ($env:MYSQL_ADMIN_USER) { $env:MYSQL_ADMIN_USER } else { 'root' }
$adminPassword = $env:MYSQL_ADMIN_PASSWORD
$appUser = if ($env:DB_USER) { $env:DB_USER } else { 'Chengzi' }
$appPassword = $env:DB_PASSWORD
$databaseName = if ($env:DB_NAME) { $env:DB_NAME } else { 'anime_platform' }
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }
$port = if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }

if ($databaseName -notmatch '^[A-Za-z0-9_]+$') {
    throw "Unsafe database name: $databaseName"
}

$appPassword = Get-PlainTextPassword `
    -Prompt "Password for $appUser@localhost" `
    -CurrentValue $appPassword

if (-not $SkipBootstrap) {
    $bootstrap = Join-Path $PSScriptRoot 'bootstrap-database.ps1'
    & $bootstrap `
        -EnvFile $EnvFile `
        -MySqlExe $MySqlExe `
        -AdminUser $adminUser `
        -AdminPassword $adminPassword `
        -AppUser $appUser `
        -AppPassword $appPassword `
        -DatabaseName $databaseName `
        -DbHost $dbHost `
        -Port $port
    if ($LASTEXITCODE -ne 0) {
        throw "Database bootstrap failed with exit code $LASTEXITCODE."
    }
}

$expectedTables = @(
    'sys_user',
    'sys_role',
    'sys_user_role',
    'anime_studio',
    'anime',
    'anime_genre',
    'anime_genre_relation',
    'anime_episode',
    'anime_staff',
    'anime_staff_relation',
    'streaming_platform',
    'anime_platform_relation',
    'user_favorite',
    'user_watch_history',
    'anime_review',
    'operation_log',
    'sys_user_profile'
)

$preflight = Invoke-MySqlText `
    -MySqlExe $MySqlExe `
    -UserName $adminUser `
    -Password $adminPassword `
    -DbHost $dbHost `
    -Port $port `
    -Sql (
        "SELECT TABLE_NAME FROM information_schema.TABLES " +
        "WHERE TABLE_SCHEMA = '$databaseName' AND TABLE_TYPE = 'BASE TABLE' " +
        "ORDER BY TABLE_NAME;"
    )
Assert-MySqlSuccess -Result $preflight -Context 'existing database content preflight'

$existingTables = @(
    $preflight.Output -split "`r?`n" |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ }
)
$unexpectedTables = @(
    $existingTables |
        Where-Object { $_ -notin $expectedTables }
)

if ($existingTables.Count -gt 0) {
    Write-Host "Existing project tables detected: $($existingTables -join ', ')"
}
if ($unexpectedTables.Count -gt 0 -and -not $AllowAdditionalTables) {
    throw (
        "Database $databaseName contains unexpected tables: " +
        "$($unexpectedTables -join ', '). No files were applied. " +
        "Use -AllowAdditionalTables only after reviewing the existing schema."
    )
}

function Invoke-AdminFile {
    param([string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    Write-Host "Applying $RelativePath"
    $result = Invoke-MySqlFile `
        -MySqlExe $MySqlExe `
        -UserName $adminUser `
        -Password $adminPassword `
        -DbHost $dbHost `
        -Port $port `
        -Database $databaseName `
        -Path $path
    Assert-MySqlSuccess -Result $result -Context $RelativePath
}

$orderedFiles = @(
    'database/schema/00_create_database.sql',
    'database/schema/01_tables.sql',
    'database/data/01_reference_and_users.sql',
    'database/data/02_anime_content.sql',
    'database/data/03_interactions.sql',
    'database/data/05_cover_images.sql'
)

$objectGroups = @(
    'database/functions/*.sql',
    'database/views/*.sql',
    'database/cursors/*.sql',
    'database/procedures/sp_add_anime.sql',
    'database/procedures/sp_get_studio_statistics.sql',
    'database/procedures/sp_get_anime_ranking.sql',
    'database/procedures/sp_delete_anime.sql',
    'database/procedures/sp_transaction_demo.sql',
    'database/triggers/*.sql'
)

foreach ($pattern in $objectGroups) {
    Get-ChildItem -Path (Join-Path $repoRoot $pattern) |
        Sort-Object Name |
        ForEach-Object {
            $relativePath = $_.FullName.Substring($repoRoot.Length + 1).Replace('\', '/')
            $orderedFiles += $relativePath
        }
}

$orderedFiles += 'database/data/04_recalculate_counters.sql'

foreach ($relativePath in $orderedFiles) {
    Invoke-AdminFile -RelativePath $relativePath
}

$verification = Invoke-MySqlText `
    -MySqlExe $MySqlExe `
    -UserName $appUser `
    -Password $appPassword `
    -DbHost $dbHost `
    -Port $port `
    -Database $databaseName `
    -Sql @'
SELECT
    (SELECT COUNT(*) FROM anime_studio) AS studios,
    (SELECT COUNT(*) FROM anime) AS anime,
    (SELECT COUNT(*) FROM anime_episode) AS episodes,
    (SELECT COUNT(*) FROM anime_staff) AS staff,
    (SELECT COUNT(*) FROM streaming_platform) AS platforms,
    (SELECT COUNT(*) FROM sys_user) AS users,
    (SELECT COUNT(*) FROM user_favorite) AS favorites,
    (SELECT COUNT(*) FROM user_watch_history) AS watch_history,
    (SELECT COUNT(*) FROM anime_review) AS reviews;
'@
Assert-MySqlSuccess -Result $verification -Context 'seed verification as application account'

Write-Host 'Database initialization completed.'
Write-Host "Seed counts: $($verification.Output.Trim())"
