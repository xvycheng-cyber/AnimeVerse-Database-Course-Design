[CmdletBinding()]
param(
    [string]$EnvFile = '',
    [string]$MySqlExe = '',
    [string]$AdminUser = '',
    [string]$AdminPassword = '',
    [string]$AppUser = '',
    [string]$AppPassword = '',
    [string]$DatabaseName = '',
    [string]$DbHost = '',
    [int]$Port = 0
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'database-utils.ps1')

if (-not $EnvFile) {
    $EnvFile = Join-Path $repoRoot '.env'
}
Import-DotEnv -Path $EnvFile

$MySqlExe = Resolve-MySqlClient -RequestedPath $MySqlExe
if (-not $AdminUser) {
    $AdminUser = if ($env:MYSQL_ADMIN_USER) { $env:MYSQL_ADMIN_USER } else { 'root' }
}
if (-not $AdminPassword) {
    $AdminPassword = $env:MYSQL_ADMIN_PASSWORD
}
if (-not $AppUser) {
    $AppUser = if ($env:DB_USER) { $env:DB_USER } else { 'Chengzi' }
}
if (-not $AppPassword) {
    $AppPassword = $env:DB_PASSWORD
}
if (-not $DatabaseName) {
    $DatabaseName = if ($env:DB_NAME) { $env:DB_NAME } else { 'anime_platform' }
}
if (-not $DbHost) {
    $DbHost = if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }
}
if ($Port -le 0) {
    $Port = if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }
}

$AppPassword = Get-PlainTextPassword `
    -Prompt "Password for $AppUser@localhost" `
    -CurrentValue $AppPassword

foreach ($identifier in @($AppUser, $DatabaseName)) {
    if ($identifier -notmatch '^[A-Za-z0-9_]+$') {
        throw "Unsafe MySQL identifier: $identifier"
    }
}

if ($AdminUser -notmatch '^[A-Za-z0-9_.-]+$') {
    throw "Unsafe administrator user name: $AdminUser"
}
if (-not $AppPassword) {
    throw 'Application password must not be empty.'
}

function Invoke-AdminScalar {
    param([string]$Sql)
    $result = Invoke-MySqlText `
        -MySqlExe $MySqlExe `
        -UserName $AdminUser `
        -Password $AdminPassword `
        -DbHost $DbHost `
        -Port $Port `
        -Sql $Sql
    Assert-MySqlSuccess -Result $result -Context 'administrator query'
    return $result.Output.Trim()
}

$databaseLiteral = ConvertTo-MySqlStringLiteral $DatabaseName
$databaseExists = Invoke-AdminScalar -Sql (
    "SELECT COUNT(*) FROM information_schema.SCHEMATA " +
    "WHERE SCHEMA_NAME = $databaseLiteral;"
)

if ($databaseExists -eq '0') {
    Invoke-AdminScalar -Sql (
        "CREATE DATABASE ``$DatabaseName`` " +
        "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
    ) | Out-Null
    Write-Host "Created database $DatabaseName."
}
else {
    Write-Host "Database $DatabaseName already exists; it will not be dropped or recreated."
}

$userLiteral = ConvertTo-MySqlStringLiteral $AppUser
$userExists = Invoke-AdminScalar -Sql (
    "SELECT COUNT(*) FROM mysql.user " +
    "WHERE User = $userLiteral AND Host = 'localhost';"
)
$passwordLiteral = ConvertTo-MySqlStringLiteral $AppPassword

if ($userExists -eq '0') {
    Invoke-AdminScalar -Sql (
        "CREATE USER '$AppUser'@'localhost' IDENTIFIED BY $passwordLiteral;"
    ) | Out-Null
    Write-Host "Created application account $AppUser@localhost."
}
else {
    Invoke-AdminScalar -Sql (
        "ALTER USER '$AppUser'@'localhost' IDENTIFIED BY $passwordLiteral;"
    ) | Out-Null
    Write-Host "Application account $AppUser@localhost already existed; password refreshed without changing its database grants."
}

Invoke-AdminScalar -Sql (
    "GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE, SHOW VIEW " +
    "ON ``$DatabaseName``.* TO '$AppUser'@'localhost';"
) | Out-Null

$grants = Invoke-AdminScalar -Sql "SHOW GRANTS FOR '$AppUser'@'localhost';"
Write-Host 'Current application grants:'
Write-Host $grants

$globalGrantCount = Invoke-AdminScalar -Sql (
    "SELECT COUNT(*) FROM mysql.user " +
    "WHERE User = $userLiteral AND Host = 'localhost' " +
    "AND (Super_priv = 'Y' OR Grant_priv = 'Y' OR Create_user_priv = 'Y' " +
    "OR Drop_priv = 'Y' OR File_priv = 'Y' OR Process_priv = 'Y');"
)
if ([int]$globalGrantCount -gt 0) {
    Write-Warning "$AppUser@localhost has global privileges outside this script's scope. They were intentionally not revoked."
}

$verify = Invoke-MySqlText `
    -MySqlExe $MySqlExe `
    -UserName $AppUser `
    -Password $AppPassword `
    -DbHost $DbHost `
    -Port $Port `
    -Database $DatabaseName `
    -Sql "SELECT CURRENT_USER(), DATABASE();"
Assert-MySqlSuccess -Result $verify -Context 'application account verification'
Write-Host "Application connection verified: $($verify.Output.Trim())"

