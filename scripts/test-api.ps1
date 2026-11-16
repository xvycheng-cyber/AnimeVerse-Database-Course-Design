[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080/api'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'database-utils.ps1')
Import-DotEnv -Path (Join-Path $repoRoot '.env')

$MySqlExe = Resolve-MySqlClient
$dbHost = if ($env:DB_HOST) { $env:DB_HOST } else { 'localhost' }
$dbPort = if ($env:DB_PORT) { [int]$env:DB_PORT } else { 3306 }
$databaseName = if ($env:DB_NAME) { $env:DB_NAME } else { 'anime_platform' }

$results = [System.Collections.Generic.List[object]]::new()
$cleanup = [System.Collections.Generic.List[scriptblock]]::new()

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Token = '',
        [object]$Body = $null
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $request = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $headers
        ContentType = 'application/json; charset=utf-8'
        UseBasicParsing = $true
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $request.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }

    $response = Invoke-WebRequest @request
    if ($null -eq $response.RawContentStream -or $response.RawContentStream.Length -eq 0) {
        return $null
    }
    $bytes = $response.RawContentStream.ToArray()
    $text = [System.Text.Encoding]::UTF8.GetString($bytes)
    return $text | ConvertFrom-Json
}

function Get-HttpStatus {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = '',
        [object]$Body = $null
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }
    $request = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $headers
        UseBasicParsing = $true
        TimeoutSec = 20
    }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    try {
        $response = Invoke-WebRequest @request
        return [int]$response.StatusCode
    }
    catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function Add-Result {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Evidence
    )
    $results.Add([pscustomobject]@{
        Test = $Name
        Result = if ($Passed) { 'PASS' } else { 'FAIL' }
        Evidence = $Evidence
    })
}

function Assert-Api {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    try {
        $evidence = & $Action
        Add-Result -Name $Name -Passed $true -Evidence ([string]$evidence)
    }
    catch {
        Add-Result -Name $Name -Passed $false -Evidence $_.Exception.Message
    }
}

function Assert-Condition {
    param(
        [bool]$Condition,
        [string]$Message
    )
    if (-not $Condition) {
        throw $Message
    }
}

try {
    $adminToken = ''
    $userToken = ''
    $createdReviewId = $null
    $createdGenreId = $null
    $createdAnimeId = $null
    $createdRoleId = $null
    $disabledUserId = $null
    $createdUserId = $null

    Assert-Api '管理员登录与 ADMIN 角色' {
        $body = @{ username = 'admin'; password = 'Admin123!' }
        $response = Invoke-Api -Method POST -Path '/auth/login' -Body $body
        Assert-Condition ($response.code -eq 200) '登录响应失败'
        Assert-Condition ($response.data.user.roles -contains 'ADMIN') '未返回 ADMIN 角色'
        $script:adminToken = $response.data.token
        "token=$($response.data.token.Length) chars; roles=$($response.data.user.roles -join ',')"
    }

    Assert-Api '普通用户登录与 USER 角色' {
        $body = @{ username = 'sakura'; password = 'User123!' }
        $response = Invoke-Api -Method POST -Path '/auth/login' -Body $body
        Assert-Condition ($response.code -eq 200) '登录响应失败'
        Assert-Condition ($response.data.user.roles -contains 'USER') '未返回 USER 角色'
        $script:userToken = $response.data.token
        "token=$($response.data.token.Length) chars; roles=$($response.data.user.roles -join ',')"
    }

    Assert-Api 'Actuator 健康检查返回 UP' {
        $healthUrl = ($BaseUrl -replace '/api/?$', '') + '/actuator/health'
        $response = Invoke-WebRequest `
            -Uri $healthUrl `
            -UseBasicParsing `
            -TimeoutSec 20
        if ($response.Content -is [byte[]]) {
            $healthBody = [System.Text.Encoding]::UTF8.GetString($response.Content)
        }
        else {
            $healthBody = [string]$response.Content
        }
        Assert-Condition ($response.StatusCode -eq 200) "预期 HTTP 200，实际 $($response.StatusCode)"
        Assert-Condition ($healthBody -match '"status"\s*:\s*"UP"') '健康检查未返回 UP'
        "HTTP $($response.StatusCode); status=UP"
    }

    Assert-Api '注册短用户名被后端校验拒绝' {
        $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 8)
        $status = Get-HttpStatus `
            -Method POST `
            -Path '/auth/register' `
            -Body @{
                username = 'abc'
                password = 'Course123!'
                nickname = '短用户名测试'
                email = "short-$suffix@animeverse.local"
            }
        Assert-Condition ($status -eq 400) "预期 400，实际 $status"
        "HTTP $status"
    }

    Assert-Api '合法注册返回 USER 角色并可清理' {
        $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 8)
        $username = "apitest_$suffix"
        $response = Invoke-Api `
            -Method POST `
            -Path '/auth/register' `
            -Body @{
                username = $username
                password = 'Course123!'
                nickname = 'API注册测试'
                email = "$username@animeverse.local"
            }
        Assert-Condition ($response.code -in @(200, 201)) '注册响应失败'
        Assert-Condition ($response.data.user.roles -contains 'USER') '注册用户缺少 USER 角色'
        Assert-Condition (![string]::IsNullOrWhiteSpace($response.data.token)) '注册未返回访问令牌'
        $script:createdUserId = [long]$response.data.user.id
        "userId=$createdUserId; roles=USER; passwordHash=BCrypt(cost 12)"
    }

    Assert-Api '公开列表、详情、排行榜与 OpenAPI' {
        $list = Invoke-Api -Method GET -Path '/anime?page=1&size=5'
        Assert-Condition ($list.data.total -eq 30) "预期 30 部动漫，实际 $($list.data.total)"
        Assert-Condition ($list.data.records.Count -eq 5) '分页数量不正确'
        $detail = Invoke-Api -Method GET -Path '/anime/8'
        Assert-Condition ($detail.data.title -eq '葬送的芙莉莲') '动漫详情不一致'
        $ranking = Invoke-Api -Method GET -Path '/anime/ranking?limit=5'
        Assert-Condition ($ranking.data.Count -eq 5) '排行榜数量不正确'
        Assert-Condition (![string]::IsNullOrWhiteSpace($ranking.data[0].coverUrl)) '排行榜缺少封面字段'
        Assert-Condition ($null -ne $ranking.data[0].releaseYear) '排行榜缺少年份字段'
        Assert-Condition (![string]::IsNullOrWhiteSpace($ranking.data[0].status)) '排行榜缺少状态字段'
        $docs = Invoke-WebRequest `
            -Uri 'http://127.0.0.1:8080/v3/api-docs' `
            -UseBasicParsing `
            -TimeoutSec 20
        Assert-Condition ($docs.Content -match 'openapi') 'OpenAPI 文档不可用'
        "anime=30; ranking=5; detail=$($detail.data.title); openapi=$($docs.Content.Length) bytes"
    }

    Assert-Api '无令牌访问管理接口返回 401' {
        $status = Get-HttpStatus -Method GET -Path '/admin/statistics/overview'
        Assert-Condition ($status -eq 401) "预期 401，实际 $status"
        "HTTP $status"
    }

    Assert-Api '普通用户访问管理接口返回 403' {
        $status = Get-HttpStatus -Method GET -Path '/admin/statistics/overview' -Token $userToken
        Assert-Condition ($status -eq 403) "预期 403，实际 $status"
        "HTTP $status"
    }

    Assert-Api '普通用户访问数据库实验室返回 403' {
        $status = Get-HttpStatus -Method GET -Path '/lab/views/v_anime_detail' -Token $userToken
        Assert-Condition ($status -eq 403) "预期 403，实际 $status"
        "HTTP $status"
    }

    Assert-Api '公开接口不受无效 Bearer 令牌阻断' {
        $status = Get-HttpStatus -Method GET -Path '/anime?page=1&size=1' -Token 'not-a-valid-jwt'
        Assert-Condition ($status -eq 200) "预期 200，实际 $status"
        "HTTP $status"
    }

    Assert-Api '受保护接口拒绝无效 Bearer 令牌' {
        $status = Get-HttpStatus -Method GET -Path '/user/profile' -Token 'not-a-valid-jwt'
        Assert-Condition ($status -eq 401) "预期 401，实际 $status"
        "HTTP $status"
    }

    Assert-Api 'SQL 注入式关键词不扩大结果集' {
        $needle = [uri]::EscapeDataString("' OR 1=1 --")
        $response = Invoke-Api -Method GET -Path "/anime?page=1&size=10&keyword=$needle"
        Assert-Condition ($response.data.total -lt 30) "疑似注入导致返回全部数据: $($response.data.total)"
        "matched=$($response.data.total)"
    }

    Assert-Api '动漫列表前端排序值与兼容别名均返回数据库顺序' {
        $sortContracts = [ordered]@{
            title = 'title ASC, id DESC'
            rating = 'average_rating DESC, id DESC'
            favorites = 'favorite_count DESC, id DESC'
            latest = 'release_date DESC, id DESC'
            popular = 'favorite_count DESC, id DESC'
            score = 'average_rating DESC, id DESC'
        }
        $evidence = @()
        foreach ($sortName in $sortContracts.Keys) {
            $response = Invoke-Api -Method GET -Path "/anime?page=1&size=100&sort=$sortName"
            Assert-Condition ($response.data.total -eq 30) "$sortName 返回总数错误"
            $apiOrder = @($response.data.records | ForEach-Object { [long]$_.id }) -join ','
            $expectedResult = Invoke-MySqlText `
                -MySqlExe $MySqlExe `
                -UserName $env:DB_USER `
                -Password $env:DB_PASSWORD `
                -DbHost $dbHost `
                -Port $dbPort `
                -Database $databaseName `
                -Sql "SELECT GROUP_CONCAT(id ORDER BY $($sortContracts[$sortName]) SEPARATOR ',') FROM anime;"
            Assert-MySqlSuccess -Result $expectedResult -Context "expected sort $sortName"
            $expectedOrder = $expectedResult.Output.Trim()
            Assert-Condition ($apiOrder -eq $expectedOrder) "$sortName 的 API 顺序与数据库不一致"
            $evidence += "$sortName=ok"
        }
        $evidence -join '; '
    }

    Assert-Api '用户收藏新增与取消及触发器计数' {
        $favorites = Invoke-Api -Method GET -Path '/user/favorites' -Token $userToken
        $existing = @($favorites.data | ForEach-Object { [long]$_.animeId })
        $animeId = 1..30 | Where-Object { $_ -notin $existing } | Select-Object -First 1
        Assert-Condition ($null -ne $animeId) '找不到可测试动漫'
        $before = (Invoke-Api -Method GET -Path "/anime/$animeId").data.favoriteCount
        Invoke-Api -Method POST -Path "/user/favorites/$animeId" -Token $userToken | Out-Null
        $afterAdd = (Invoke-Api -Method GET -Path "/anime/$animeId").data.favoriteCount
        Invoke-Api -Method DELETE -Path "/user/favorites/$animeId" -Token $userToken | Out-Null
        $afterDelete = (Invoke-Api -Method GET -Path "/anime/$animeId").data.favoriteCount
        Assert-Condition ($afterAdd -eq $before + 1) "新增计数不一致: $before -> $afterAdd"
        Assert-Condition ($afterDelete -eq $before) "取消计数不一致: $afterAdd -> $afterDelete"
        "animeId=$animeId; count=$before->$afterAdd->$afterDelete"
    }

    Assert-Api '观看记录更新与恢复' {
        $history = Invoke-Api -Method GET -Path '/user/watch-history' -Token $userToken
        Assert-Condition ($history.data.Count -gt 0) '种子观看记录为空'
        $item = $history.data | Select-Object -First 1
        $episodeId = [long]$item.episodeId
        $original = if ($null -ne $item.progressSeconds) {
            [int]$item.progressSeconds
        }
        else {
            0
        }
        $temporary = if ($original -eq 120) { 121 } else { 120 }
        Invoke-Api `
            -Method POST `
            -Path '/user/watch-history' `
            -Token $userToken `
            -Body @{ episodeId = $episodeId; progressSeconds = $temporary } | Out-Null
        $updated = (Invoke-Api -Method GET -Path '/user/watch-history' -Token $userToken).data |
            Where-Object { [long]$_.episodeId -eq $episodeId } |
            Select-Object -First 1
        Assert-Condition ([int]$updated.progressSeconds -eq $temporary) '观看进度未更新'
        Invoke-Api `
            -Method POST `
            -Path '/user/watch-history' `
            -Token $userToken `
            -Body @{ episodeId = $episodeId; progressSeconds = $original } | Out-Null
        "episodeId=$episodeId; progress=$original->$temporary->$original"
    }

    Assert-Api '用户评论新增、修改与删除' {
        $mine = Invoke-Api -Method GET -Path '/user/reviews?page=1&size=100' -Token $userToken
        $reviewedAnimeIds = @($mine.data.records | ForEach-Object { [long]$_.animeId })
        $animeId = 1..30 | Where-Object { $_ -notin $reviewedAnimeIds } | Select-Object -First 1
        Assert-Condition ($null -ne $animeId) '找不到可评论动漫'
        $created = Invoke-Api `
            -Method POST `
            -Path "/anime/$animeId/reviews" `
            -Token $userToken `
            -Body @{ rating = 8; content = 'API 联调临时评论，测试后删除。' }
        $script:createdReviewId = [long]$created.data.id
        $updated = Invoke-Api `
            -Method PUT `
            -Path "/user/reviews/$createdReviewId" `
            -Token $userToken `
            -Body @{ rating = 9; content = 'API 联调更新后的临时评论。' }
        Assert-Condition ($updated.data.rating -eq 9) '评论更新失败'
        Invoke-Api -Method DELETE -Path "/user/reviews/$createdReviewId" -Token $userToken | Out-Null
        $script:createdReviewId = $null
        "animeId=$animeId; create/update/delete passed"
    }

    Assert-Api '管理员统计、日志与类型 CRUD' {
        $overview = Invoke-Api -Method GET -Path '/admin/statistics/overview' -Token $adminToken
        Assert-Condition ($overview.data.animeCount -eq 30) "统计动漫数错误: $($overview.data.animeCount)"
        $trend = Invoke-Api -Method GET -Path '/admin/statistics/trend?days=30' -Token $adminToken
        Assert-Condition ($trend.data.Count -eq 30) "趋势数据应返回 30 天，实际 $($trend.data.Count)"
        Assert-Condition ($null -ne $trend.data[0].userCount) '趋势数据缺少用户字段'
        $logs = Invoke-Api -Method GET -Path '/admin/logs?page=1&size=5' -Token $adminToken
        Assert-Condition ($null -ne $logs.data.total) '日志分页不可用'
        $genreName = "API测试类型$([DateTimeOffset]::UtcNow.ToUnixTimeSeconds())"
        $created = Invoke-Api `
            -Method POST `
            -Path '/admin/genres' `
            -Token $adminToken `
            -Body @{ name = $genreName; slug = "api-test-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"; description = '联调临时类型' }
        $script:createdGenreId = [long]$created.data.id
        Invoke-Api `
            -Method PUT `
            -Path "/admin/genres/$createdGenreId" `
            -Token $adminToken `
            -Body @{ name = "$genreName-改"; slug = "api-test-update-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"; description = '已更新' } | Out-Null
        Invoke-Api -Method DELETE -Path "/admin/genres/$createdGenreId" -Token $adminToken | Out-Null
        $script:createdGenreId = $null
        "animeCount=$($overview.data.animeCount); trend=30; logs=$($logs.data.total); CRUD=passed"
    }

    Assert-Api '管理员用户与角色读取' {
        $users = Invoke-Api -Method GET -Path '/admin/users?page=1&size=10' -Token $adminToken
        $roles = Invoke-Api -Method GET -Path '/admin/roles' -Token $adminToken
        Assert-Condition ($users.data.total -ge 10) '用户列表不完整'
        Assert-Condition ($roles.data.Count -ge 2) '角色列表不完整'
        "users=$($users.data.total); roles=$($roles.data.Count)"
    }

    Assert-Api '管理员角色新增、修改与删除' {
        $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 8).ToUpperInvariant()
        $roleCode = "APITEST$suffix"
        $created = Invoke-Api `
            -Method POST `
            -Path '/admin/roles' `
            -Token $adminToken `
            -Body @{
                roleCode = $roleCode
                roleName = "API临时角色$suffix"
                description = 'API 联调临时角色'
            }
        $script:createdRoleId = [long]$created.data.id
        $updated = Invoke-Api `
            -Method PUT `
            -Path "/admin/roles/$createdRoleId" `
            -Token $adminToken `
            -Body @{
                roleCode = $roleCode
                roleName = "API临时角色$suffix-改"
                description = 'API 联调已更新'
            }
        Assert-Condition ($updated.data.roleName -eq "API临时角色$suffix-改") '角色名称未更新'
        Invoke-Api -Method DELETE -Path "/admin/roles/$createdRoleId" -Token $adminToken | Out-Null
        $script:createdRoleId = $null
        "roleCode=$roleCode; create/update/delete passed"
    }

    Assert-Api '停用账户登录返回 403 并可恢复' {
        $users = Invoke-Api `
            -Method GET `
            -Path '/admin/users?page=1&size=10&keyword=sakura' `
            -Token $adminToken
        $target = $users.data.records |
            Where-Object { $_.username -eq 'sakura' } |
            Select-Object -First 1
        Assert-Condition ($null -ne $target) '找不到 sakura 测试账户'
        $script:disabledUserId = [long]$target.id
        Invoke-Api `
            -Method PUT `
            -Path "/admin/users/$disabledUserId/status" `
            -Token $adminToken `
            -Body @{ enabled = $false } | Out-Null
        $status = Get-HttpStatus `
            -Method POST `
            -Path '/auth/login' `
            -Body @{ username = 'sakura'; password = 'User123!' }
        Assert-Condition ($status -eq 403) "停用账户登录预期 403，实际 $status"
        Invoke-Api `
            -Method PUT `
            -Path "/admin/users/$disabledUserId/status" `
            -Token $adminToken `
            -Body @{ enabled = $true } | Out-Null
        $script:disabledUserId = $null
        "userId=$($target.id); disabledLogin=403; account restored"
    }

    Assert-Api '四个课程视图可查询' {
        $views = @('v_anime_detail', 'v_studio_statistics', 'v_anime_popularity', 'v_user_activity')
        $counts = @()
        foreach ($view in $views) {
            $response = Invoke-Api -Method GET -Path "/lab/views/$view" -Token $adminToken
            Assert-Condition ($response.data.Count -gt 0) "$view 无数据"
            $counts += "$view=$($response.data.Count)"
        }
        $counts -join '; '
    }

    Assert-Api '触发器收藏实验室与恢复' {
        $favorites = Invoke-Api -Method GET -Path '/user/favorites' -Token $adminToken
        $existing = @($favorites.data | ForEach-Object { [long]$_.animeId })
        $animeId = 1..30 | Where-Object { $_ -notin $existing } | Select-Object -First 1
        Assert-Condition ($null -ne $animeId) '管理员找不到可测试动漫'
        $enabled = Invoke-Api `
            -Method POST `
            -Path '/lab/trigger/favorite' `
            -Token $adminToken `
            -Body @{ animeId = $animeId; enable = $true }
        Assert-Condition ($enabled.data.afterCount -eq $enabled.data.beforeCount + 1) '触发器新增计数未增加'
        $disabled = Invoke-Api `
            -Method POST `
            -Path '/lab/trigger/favorite' `
            -Token $adminToken `
            -Body @{ animeId = $animeId; enable = $false }
        Assert-Condition ($disabled.data.afterCount -eq $disabled.data.beforeCount - 1) '触发器删除计数未减少'
        "animeId=$animeId; $($enabled.data.beforeCount)->$($enabled.data.afterCount)->$($disabled.data.afterCount)"
    }

    Assert-Api '存储过程统计、排行与新增动漫' {
        $studio = Invoke-Api -Method GET -Path '/lab/procedure/studio-statistics/1' -Token $adminToken
        Assert-Condition ($studio.data.studioId -eq 1) '厂商统计结果错误'
        $ranking = Invoke-Api -Method GET -Path '/lab/procedure/anime-ranking?limit=6' -Token $adminToken
        Assert-Condition ($ranking.data.Count -eq 6) '存储过程排行数量错误'
        $title = "API临时作品-$([Guid]::NewGuid().ToString('N').Substring(0, 8))"
        $created = Invoke-Api `
            -Method POST `
            -Path '/lab/procedure/add-anime' `
            -Token $adminToken `
            -Body @{
                title = $title
                originalTitle = $title
                studioId = 1
                synopsis = '存储过程联调临时数据'
                releaseYear = 2026
                status = 'COMPLETED'
            }
        $script:createdAnimeId = [long]$created.data.animeId
        Assert-Condition ($createdAnimeId -gt 0) 'sp_add_anime 未返回 ID'
        Invoke-Api -Method DELETE -Path "/lab/procedure/anime/$createdAnimeId" -Token $adminToken | Out-Null
        $script:createdAnimeId = $null
        "studio=$($studio.data.studioName); ranking=6; procedureId=$($created.data.animeId); auditedDelete=passed"
    }

    Assert-Api '实验室写操作生成审计记录' {
        $logs = Invoke-Api `
            -Method GET `
            -Path '/admin/logs?page=1&size=100&module=LAB' `
            -Token $adminToken
        $actions = @($logs.data.records | ForEach-Object { $_.actionType })
        foreach ($expected in @('LAB_TRIGGER_FAVORITE', 'LAB_PROCEDURE_ADD_ANIME', 'LAB_PROCEDURE_DELETE_ANIME')) {
            Assert-Condition ($actions -contains $expected) "缺少审计动作 $expected"
        }
        "LAB audit rows=$($actions.Count); actions=$((@('LAB_TRIGGER_FAVORITE', 'LAB_PROCEDURE_ADD_ANIME', 'LAB_PROCEDURE_DELETE_ANIME')) -join ',')"
    }

    Assert-Api '游标厂商报表遍历 8 家厂商' {
        $report = Invoke-Api -Method GET -Path '/lab/cursor/studio-report' -Token $adminToken
        Assert-Condition ($report.data.Count -eq 8) "预期 8 行，实际 $($report.data.Count)"
        "rows=$($report.data.Count); first=$($report.data[0].studioName)"
    }

    Assert-Api '事务提交、回滚与异常三种模式' {
        $commit = Invoke-Api -Method POST -Path '/lab/transaction/demo' -Token $adminToken -Body @{ mode = 'commit' }
        $rollback = Invoke-Api -Method POST -Path '/lab/transaction/demo' -Token $adminToken -Body @{ mode = 'rollback' }
        $exception = Invoke-Api -Method POST -Path '/lab/transaction/demo' -Token $adminToken -Body @{ mode = 'exception' }
        Assert-Condition ($commit.data.committed -eq $true -and $commit.data.visibleRows -eq 1) 'COMMIT 断言失败'
        Assert-Condition ($rollback.data.rolledBack -eq $true -and $rollback.data.visibleRows -eq 0) 'ROLLBACK 断言失败'
        Assert-Condition ($exception.data.rolledBack -eq $true -and $exception.data.visibleRows -eq 0) 'EXCEPTION 断言失败'
        'commit=1 row; rollback=0; exception=0'
    }

    Assert-Api '前端源码不存在 v-html 直接注入' {
        $matches = rg -n 'v-html' frontend/src 2>$null
        Assert-Condition ($LASTEXITCODE -eq 1 -or [string]::IsNullOrWhiteSpace(($matches -join ''))) '发现 v-html'
        'v-html matches=0'
    }
}
finally {
    if ($createdReviewId) {
        try {
            Invoke-Api -Method DELETE -Path "/user/reviews/$createdReviewId" -Token $userToken | Out-Null
        }
        catch {}
    }
    if ($createdGenreId) {
        try {
            Invoke-Api -Method DELETE -Path "/admin/genres/$createdGenreId" -Token $adminToken | Out-Null
        }
        catch {}
    }
    if ($createdAnimeId) {
        try {
            Invoke-Api -Method DELETE -Path "/admin/anime/$createdAnimeId" -Token $adminToken | Out-Null
        }
        catch {}
    }
    if ($createdRoleId) {
        try {
            Invoke-Api -Method DELETE -Path "/admin/roles/$createdRoleId" -Token $adminToken | Out-Null
        }
        catch {}
    }
    if ($createdUserId) {
        try {
            $cleanupResult = Invoke-MySqlText `
                -MySqlExe $MySqlExe `
                -UserName $env:DB_USER `
                -Password $env:DB_PASSWORD `
                -DbHost $dbHost `
                -Port $dbPort `
                -Database $databaseName `
                -Sql "DELETE FROM sys_user WHERE id = $createdUserId;"
            Assert-MySqlSuccess -Result $cleanupResult -Context 'registered API test user cleanup'
        }
        catch {}
    }
    if ($disabledUserId) {
        try {
            Invoke-Api `
                -Method PUT `
                -Path "/admin/users/$disabledUserId/status" `
                -Token $adminToken `
                -Body @{ enabled = $true } | Out-Null
        }
        catch {}
    }
}

$results | Format-Table -AutoSize
$failed = @($results | Where-Object Result -eq 'FAIL')
Write-Host "API test summary: $($results.Count - $failed.Count)/$($results.Count) passed."
if ($failed.Count -gt 0) {
    $failed | Format-List
    exit 1
}
