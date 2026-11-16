Set-StrictMode -Version Latest

function Import-DotEnv {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith('#')) {
            continue
        }

        $separator = $trimmed.IndexOf('=')
        if ($separator -lt 1) {
            continue
        }

        $name = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        if (-not (Test-Path "Env:$name")) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

function Resolve-MySqlClient {
    param(
        [string]$RequestedPath
    )

    if ($RequestedPath) {
        if (-not (Test-Path -LiteralPath $RequestedPath)) {
            throw "MySQL client not found: $RequestedPath"
        }
        return (Resolve-Path -LiteralPath $RequestedPath).Path
    }

    if ($env:MYSQL_EXE -and (Test-Path -LiteralPath $env:MYSQL_EXE)) {
        return (Resolve-Path -LiteralPath $env:MYSQL_EXE).Path
    }

    $command = Get-Command mysql -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $commonPaths = @(
        'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
        'E:\mysql\install_directory\bin\mysql.exe'
    )
    foreach ($path in $commonPaths) {
        if (Test-Path -LiteralPath $path) {
            return $path
        }
    }

    throw 'mysql.exe was not found. Set MYSQL_EXE or add MySQL bin to PATH.'
}

function Invoke-MySqlText {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MySqlExe,
        [Parameter(Mandatory = $true)]
        [string]$UserName,
        [AllowEmptyString()]
        [string]$Password = '',
        [string]$DbHost = 'localhost',
        [int]$Port = 3306,
        [string]$Database = '',
        [Parameter(Mandatory = $true)]
        [string]$Sql
    )

    $previousPassword = $env:MYSQL_PWD
    $env:MYSQL_PWD = $Password
    try {
        $arguments = @(
            '--protocol=TCP',
            '-h', $DbHost,
            '-P', "$Port",
            '-u', $UserName,
            '-N',
            '-B',
            '--default-character-set=utf8mb4',
            '-e', $Sql
        )
        if ($Database) {
            $arguments = @('--database', $Database) + $arguments
        }

        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $output = & $MySqlExe @arguments 2>&1
        }
        finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        return [pscustomobject]@{
            ExitCode = $LASTEXITCODE
            Output = ($output | ForEach-Object { "$_" }) -join [Environment]::NewLine
        }
    }
    finally {
        if ($null -eq $previousPassword) {
            Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
        }
        else {
            $env:MYSQL_PWD = $previousPassword
        }
    }
}

function Invoke-MySqlFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$MySqlExe,
        [Parameter(Mandatory = $true)]
        [string]$UserName,
        [AllowEmptyString()]
        [string]$Password = '',
        [string]$DbHost = 'localhost',
        [int]$Port = 3306,
        [string]$Database = '',
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "SQL file not found: $Path"
    }

    $sourcePath = (Resolve-Path -LiteralPath $Path).Path.Replace('\', '/')
    return Invoke-MySqlText `
        -MySqlExe $MySqlExe `
        -UserName $UserName `
        -Password $Password `
        -DbHost $DbHost `
        -Port $Port `
        -Database $Database `
        -Sql "SOURCE $sourcePath"
}

function Assert-MySqlSuccess {
    param(
        [Parameter(Mandatory = $true)]
        [object]$Result,
        [Parameter(Mandatory = $true)]
        [string]$Context
    )

    if ($Result.ExitCode -ne 0) {
        throw "$Context failed with exit code $($Result.ExitCode).`n$($Result.Output)"
    }
}

function ConvertTo-MySqlStringLiteral {
    param(
        [AllowEmptyString()]
        [string]$Value
    )

    if ($Value -match "[`r`n`0]") {
        throw 'Credentials containing line breaks or NUL are not supported.'
    }

    $escaped = $Value.Replace('\', '\\').Replace("'", "''")
    return "'$escaped'"
}

function Get-PlainTextPassword {
    param(
        [string]$Prompt,
        [string]$CurrentValue
    )

    if ($CurrentValue) {
        return $CurrentValue
    }

    if ([Console]::IsInputRedirected) {
        throw "$Prompt is required through a parameter or environment variable."
    }

    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}
