[CmdletBinding()]
param(
    [string]$OutputPath = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Split-Path -Parent $PSScriptRoot)).Path
if (-not $OutputPath) {
    $OutputPath = Join-Path $repoRoot 'dist\AnimeVerse-Database-Course-Design.zip'
}
$OutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$outputDirectory = Split-Path -Parent $OutputPath
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$excludedDirectoryNames = [System.Collections.Generic.HashSet[string]]::new(
    [System.StringComparer]::OrdinalIgnoreCase
)
@(
    '.git',
    '.idea',
    '.vscode',
    '__pycache__',
    'node_modules',
    'target',
    'dist',
    '.vite',
    'tmp',
    'qa-output',
    'tools'
) | ForEach-Object { [void]$excludedDirectoryNames.Add($_) }

$excludedFileNames = [System.Collections.Generic.HashSet[string]]::new(
    [System.StringComparer]::OrdinalIgnoreCase
)
@(
    '.env',
    '.DS_Store',
    'Thumbs.db'
) | ForEach-Object { [void]$excludedFileNames.Add($_) }

function Test-ExcludedFile {
    param([System.IO.FileInfo]$File)
    if ($excludedFileNames.Contains($File.Name)) {
        return $true
    }
    if ($File.Name -like '*.pyc' -or $File.Name -like '*.log') {
        return $true
    }
    if ($File.Name -like '*.tmp.*') {
        return $true
    }
    return $false
}

function Get-IncludedFiles {
    param([string]$Directory)

    foreach ($file in Get-ChildItem -LiteralPath $Directory -File -Force) {
        if (-not (Test-ExcludedFile -File $file)) {
            $file
        }
    }
    foreach ($child in Get-ChildItem -LiteralPath $Directory -Directory -Force) {
        if (-not $excludedDirectoryNames.Contains($child.Name)) {
            Get-IncludedFiles -Directory $child.FullName
        }
    }
}

$rootName = 'AnimeVerse-Database-Course-Design'
$includedFiles = @(Get-IncludedFiles -Directory $repoRoot)
if ($includedFiles.Count -eq 0) {
    throw 'No files were selected for packaging.'
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
if (Test-Path -LiteralPath $OutputPath) {
    Remove-Item -LiteralPath $OutputPath -Force
}

$archive = [System.IO.Compression.ZipFile]::Open(
    $OutputPath,
    [System.IO.Compression.ZipArchiveMode]::Create
)
try {
    foreach ($file in $includedFiles) {
        $relativePath = $file.FullName.Substring($repoRoot.Length + 1)
        $entryName = "$rootName/$($relativePath.Replace('\', '/'))"
        [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $archive,
            $file.FullName,
            $entryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        )
    }
}
finally {
    $archive.Dispose()
}

$requiredEntries = @(
    "$rootName/README.md",
    "$rootName/.env.example",
    "$rootName/backend/pom.xml",
    "$rootName/frontend/package.json",
    "$rootName/database/schema/01_tables.sql",
    "$rootName/docs/reports/数据库课程设计报告.docx",
    "$rootName/docs/reports/AI交互报告.docx",
    "$rootName/docs/reports/final-independent-review.md",
    "$rootName/docs/reports/final-correction-report.md",
    "$rootName/docs/reports/final-acceptance-report.md",
    "$rootName/docs/diagrams/01-global-er.png",
    "$rootName/docs/screenshots/01-home.png"
)

$verificationArchive = [System.IO.Compression.ZipFile]::OpenRead($OutputPath)
try {
    $entryNames = @($verificationArchive.Entries | ForEach-Object { $_.FullName })
}
finally {
    $verificationArchive.Dispose()
}

$missingEntries = @($requiredEntries | Where-Object { $_ -notin $entryNames })
if ($missingEntries.Count -gt 0) {
    throw "ZIP is missing required entries: $($missingEntries -join ', ')"
}

$forbiddenPatterns = @(
    "(^|/)\.env$",
    "(^|/)node_modules/",
    "(^|/)target/",
    "(^|/)\.git/",
    "(^|/)frontend/dist/",
    "(^|/)__pycache__/",
    "(^|/)qa-output/"
)
$forbiddenEntries = @(
    $entryNames | Where-Object {
        $name = $_
        [bool]($forbiddenPatterns | Where-Object { $name -match $_ })
    }
)
if ($forbiddenEntries.Count -gt 0) {
    throw "ZIP contains excluded files: $($forbiddenEntries -join ', ')"
}

$temporaryRoot = [System.IO.Path]::GetFullPath([System.IO.Path]::GetTempPath())
$extractRoot = Join-Path $temporaryRoot "animeverse-package-check-$([Guid]::NewGuid().ToString('N'))"
$extractRoot = [System.IO.Path]::GetFullPath($extractRoot)
if (-not $extractRoot.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Unsafe extraction path: $extractRoot"
}

try {
    [System.IO.Compression.ZipFile]::ExtractToDirectory($OutputPath, $extractRoot)
    foreach ($relativePath in @(
        'README.md',
        '.env.example',
        'backend/pom.xml',
        'frontend/package.json',
        'database/schema/01_tables.sql',
        'docs/reports/数据库课程设计报告.docx',
        'docs/reports/AI交互报告.docx',
        'docs/reports/final-independent-review.md',
        'docs/reports/final-correction-report.md',
        'docs/reports/final-acceptance-report.md'
    )) {
        $candidate = Join-Path (Join-Path $extractRoot $rootName) $relativePath
        if (-not (Test-Path -LiteralPath $candidate)) {
            throw "Extracted ZIP is missing $relativePath"
        }
    }
}
finally {
    if (
        (Test-Path -LiteralPath $extractRoot) -and
        $extractRoot.StartsWith($temporaryRoot, [System.StringComparison]::OrdinalIgnoreCase)
    ) {
        Remove-Item -LiteralPath $extractRoot -Recurse -Force
    }
}

$archiveInfo = Get-Item -LiteralPath $OutputPath
Write-Host "Packaged $($entryNames.Count) files."
Write-Host "ZIP: $($archiveInfo.FullName)"
Write-Host "Size: $([Math]::Round($archiveInfo.Length / 1MB, 2)) MB"
