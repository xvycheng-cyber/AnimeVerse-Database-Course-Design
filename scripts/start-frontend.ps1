param(
  [int]$Port = 5173,
  [string]$NpmPath = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $repoRoot 'frontend'

if (-not (Test-Path -LiteralPath $frontendRoot)) {
  throw "未找到前端目录：$frontendRoot"
}

$npmCandidates = @(
  $NpmPath,
  (Get-Command npm.cmd -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
  (Get-Command npm -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue),
  'C:\Users\Chengzi\AppData\Local\OpenAI\Codex\runtimes\cua_node\e7fe122ad3cbcd58\bin\npm.cmd',
  'C:\Program Files\nodejs\npm.cmd'
) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }

$npmCommand = $npmCandidates | Select-Object -First 1
if (-not $npmCommand) {
  throw '未找到 npm。请通过 -NpmPath 参数提供 npm.cmd 路径。'
}

Push-Location $frontendRoot
try {
  if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules'))) {
    & $npmCommand install
    if ($LASTEXITCODE -ne 0) {
      throw "npm install 失败，退出码：$LASTEXITCODE"
    }
  }

  & $npmCommand run dev -- --host 0.0.0.0 --port $Port
  if ($LASTEXITCODE -ne 0) {
    throw "前端开发服务器退出，退出码：$LASTEXITCODE"
  }
}
finally {
  Pop-Location
}
