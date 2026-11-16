param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$backendDir = Join-Path $rootDir "backend"
$envFile = Join-Path $rootDir ".env"

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed.Split("=", 2)
        if ($parts.Count -ne 2) {
            continue
        }
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($value.Length -ge 2) {
            $first = $value.Substring(0, 1)
            $last = $value.Substring($value.Length - 1, 1)
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        if ($name -notmatch "^[A-Za-z_][A-Za-z0-9_]*$") {
            continue
        }
        $existing = [Environment]::GetEnvironmentVariable($name, "Process")
        if ([string]::IsNullOrWhiteSpace($existing)) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

Import-DotEnv -Path $envFile

$required = @("DB_HOST", "DB_PORT", "DB_NAME", "DB_USER", "DB_PASSWORD", "JWT_SECRET")
$missing = @()
foreach ($name in $required) {
    $value = [Environment]::GetEnvironmentVariable($name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        $missing += $name
    }
}
if ($missing.Count -gt 0) {
    throw "Missing required environment variables: $($missing -join ', '). Configure the repository .env file."
}

if ([Text.Encoding]::UTF8.GetByteCount($env:JWT_SECRET) -lt 32) {
    throw "JWT_SECRET must contain at least 32 UTF-8 bytes."
}

$maven = $env:MAVEN_CMD
if ([string]::IsNullOrWhiteSpace($maven)) {
    $localMaven = Join-Path $env:USERPROFILE '.codex\tools\apache-maven-3.9.11\bin\mvn.cmd'
    if (Test-Path -LiteralPath $localMaven) {
        $maven = $localMaven
    }
}
if ([string]::IsNullOrWhiteSpace($maven)) {
    $mavenCommand = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($null -eq $mavenCommand) {
        $mavenCommand = Get-Command "mvn" -ErrorAction SilentlyContinue
    }
    if ($null -ne $mavenCommand) {
        $maven = $mavenCommand.Source
    }
}
if ([string]::IsNullOrWhiteSpace($maven)) {
    throw "Maven was not found. Set MAVEN_CMD to mvn.cmd or add Maven to PATH."
}

function Resolve-Java17 {
    $candidates = [System.Collections.Generic.List[string]]::new()
    if ($env:JAVA_CMD) {
        $candidates.Add($env:JAVA_CMD)
    }
    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME "bin\java.exe"))
    }
    Get-ChildItem `
        -Path (Join-Path $env:USERPROFILE ".codex\tools") `
        -Directory `
        -Filter "jdk17-*" `
        -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        ForEach-Object {
            Get-ChildItem `
                -Path $_.FullName `
                -Recurse `
                -File `
                -Filter "java.exe" `
                -ErrorAction SilentlyContinue |
                Where-Object { $_.Directory.Name -eq "bin" } |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    Get-ChildItem `
        -Path "C:\Program Files\Java" `
        -Directory `
        -Filter "jdk-17*" `
        -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        ForEach-Object { $candidates.Add((Join-Path $_.FullName "bin\java.exe")) }
    $pathJava = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($pathJava) {
        $candidates.Add($pathJava.Source)
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $candidate)) {
            continue
        }
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $versionText = (& $candidate -version 2>&1) -join "`n"
        $versionExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousErrorActionPreference
        if ($versionExitCode -eq 0 -and $versionText -match 'version "17(\.|")') {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw "Java 17 was not found. Set JAVA_HOME or JAVA_CMD to a JDK 17 installation."
}

$java = Resolve-Java17
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)

Push-Location $backendDir
try {
    if (-not $SkipBuild) {
        & $maven -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Backend build failed with exit code $LASTEXITCODE."
        }
    }

    $jar = Join-Path $backendDir "target\animeverse-backend-1.0.0.jar"
    if (-not (Test-Path -LiteralPath $jar)) {
        throw "Backend JAR not found: $jar"
    }

    & $java -jar $jar
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
