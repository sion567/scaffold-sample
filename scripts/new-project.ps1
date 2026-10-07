<#
.SYNOPSIS
  脚手架新项目改名脚本（PowerShell 版；与 scripts/new-project.sh 逻辑严格一致）

.DESCRIPTION
  把整个脚手架复制到 -Dir 指定的新目录（源目录只读不动），然后仅对新目录内
  的文本源文件做全局替换。替换规则（顺序执行，前后端两版脚本一致）：

    1) com.scaffold          -> <GroupId>            Java 包名/配置前缀命名空间
    2) com/scaffold          -> <GroupId 的路径形式>  注释中的路径写法
    3) scaffold-             -> <Name>-              构件名/服务名/文件名引用
                                                       （spring.application.name: scaffold-xxx 一并覆盖）
    4) "scaffold.            -> "<Name>.             双引号配置前缀（@ConfigurationProperties 等）
    5) 'scaffold.            -> '<Name>.             单引号配置前缀
    6) "scaffold:            -> "<Name>:             Redis key 前缀（Java 常量字符串）
    7) scaffold_trace_id     -> <Name>_trace_id      链路追踪 key
    8) scaffoldRedisTemplate -> <Name>RedisTemplate  RedisTemplate Bean 名
    9) scaffold.version      -> <Name>.version       根 POM 版本属性
   10) ^scaffold:            -> <Name>:              yml 行首配置前缀（仅 yml/yaml）
   11) Scaffold              -> <Name> 大驼峰        应用主类 ScaffoldXxxApplication 等

  目录重命名：所有 */com/scaffold 包目录移动到 */<GroupId 路径>（多段 GroupId 自动建级联目录）；
  文件重命名：scaffold-defaults.yml / scaffold-xxx-local.yml / ScaffoldXxxApplication.java 等同步改名。
  处理范围（扩展名白名单）：java xml yml yaml vm sql properties imports md factories ps1 sh bat txt json proto
  跳过：target/、.git/、logs/ 目录与 *.log 文件（复制阶段即剔除）。

  幂等与失败安全：先整树复制、后改名替换；目标目录必须不存在或为空，否则拒绝执行；
  源目录自始至终不被修改。

.EXAMPLE
  # 复制到默认位置（脚手架同级目录）：<脚手架父目录>\yourproj
  .\scripts\new-project.ps1 -GroupId com.yourco -Name yourproj

  # 指定目标目录
  .\scripts\new-project.ps1 -GroupId com.yourco.apps -Name yourproj -Dir E:\workspace\yourproj
#>
param(
    [Parameter(Mandatory = $true)][string]$GroupId,   # 新项目 groupId，如 com.yourco
    [Parameter(Mandatory = $true)][string]$Name,      # 新项目短名（构件/服务名前缀），如 yourproj
    [string]$Dir                                      # 目标目录；缺省为脚手架父目录\<Name>
)

$ErrorActionPreference = 'Stop'

# ---------------- 0. 参数校验 ----------------
if ($GroupId -notmatch '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$') {
    Write-Error "GroupId 格式不合法：'$GroupId'，应为点分小写形式，如 com.yourco（至少两段）"
    exit 1
}
if ($Name -notmatch '^[a-z][a-z0-9]*(-[a-z][a-z0-9]*)*$') {
    Write-Error "Name 格式不合法：'$Name'，应为小写字母开头的小写短横线串，如 yourproj"
    exit 1
}

# 解析仓库根目录（本脚本位于 <root>/scripts/）
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

if (-not $Dir) { $Dir = Join-Path (Split-Path -Parent $Root) $Name }
$Dir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Dir)

if (Test-Path $Dir) {
    if ((Get-ChildItem -Force $Dir | Measure-Object).Count -gt 0) {
        Write-Error "目标目录已存在且非空：$Dir —— 为幂等安全拒绝执行，请换目录或先清空"
        exit 1
    }
} else {
    New-Item -ItemType Directory -Path $Dir -Force | Out-Null
}

# 派生形式
$GroupIdPath = $GroupId -replace '\.', '/'      # com/yourco
$FirstSeg    = ($GroupId -split '\.')[0]        # com
$Parts       = $Name -split '-'
$PascalName  = ($Parts | ForEach-Object { if ($_.Length) { $_.Substring(0, 1).ToUpper() + $_.Substring(1) } }) -join ''

# ---------------- 1. 复制整树（跳过 target/.git/logs 与 *.log） ----------------
Write-Host "[1/4] 复制 $Root -> $Dir ..." -ForegroundColor Cyan
Copy-Item -Path (Join-Path $Root '*') -Destination $Dir -Recurse -Force

# 剔除不需要的目录（先收集再删，深路径优先）
$junk = @(Get-ChildItem -Path $Dir -Recurse -Directory -Force |
    Where-Object { $_.Name -in @('target', '.git', 'logs') } |
    Sort-Object { $_.FullName.Length } -Descending)
foreach ($j in $junk) {
    if (Test-Path $j.FullName) { Remove-Item $j.FullName -Recurse -Force }
}
Get-ChildItem -Path $Dir -Recurse -File -Force -Filter *.log |
    ForEach-Object { Remove-Item $_.FullName -Force }

# ---------------- 2. 文本内容替换 ----------------
Write-Host "[2/4] 全局替换内容（com.scaffold=$GroupId, scaffold-=$Name-）..." -ForegroundColor Cyan
$TextExts = @('.java', '.xml', '.yml', '.yaml', '.vm', '.sql', '.properties', '.imports',
              '.md', '.factories', '.ps1', '.sh', '.bat', '.txt', '.json', '.proto')
$Utf8NoBom  = [System.Text.UTF8Encoding]::new($false)
$Utf8WithBom = [System.Text.UTF8Encoding]::new($true)
$changed = 0

function Update-TextFile {
    param([string]$Path, [bool]$IsYaml)
    $text = [System.IO.File]::ReadAllText($Path)   # 自动识别 BOM
    $orig = $text

    # 顺序即语义：先长 token 后短 token，避免互相污染
    $text = $text.Replace('com.scaffold', $GroupId)
    $text = $text.Replace('com/scaffold', $GroupIdPath)
    $text = $text.Replace('scaffold-', "$Name-")
    $text = $text.Replace('"scaffold.', "`"$Name.")
    $text = $text.Replace("'scaffold.", "'$Name.")
    $text = $text.Replace('"scaffold:', "`"${Name}:")
    $text = $text.Replace('scaffold_trace_id', "${Name}_trace_id")
    $text = $text.Replace('scaffoldRedisTemplate', "${Name}RedisTemplate")
    $text = $text.Replace('scaffold.version', "$Name.version")
    $text = $text.Replace('Scaffold', $PascalName)
    if ($IsYaml) {
        $text = [regex]::Replace($text, '(?m)^scaffold:', "${Name}:")
    }

    if ($text -ceq $orig) { return $false }

    # 保持原 BOM 状态写回（UTF-8）
    $hasBom = $false
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) { $hasBom = $true }
    [System.IO.File]::WriteAllText($Path, $text, ($(if ($hasBom) { $Utf8WithBom } else { $Utf8NoBom })))
    return $true
}

Get-ChildItem -Path $Dir -Recurse -File -Force | ForEach-Object {
    if ($TextExts -notcontains $_.Extension.ToLower()) { return }
    $isYaml = ($_.Extension.ToLower() -in @('.yml', '.yaml'))
    if (Update-TextFile -Path $_.FullName -IsYaml $isYaml) { $changed++ }
}
Write-Host "      已更新 $changed 个文本文件" -ForegroundColor Green

# ---------------- 3. 文件重命名 ----------------
Write-Host "[3/4] 重命名文件（scaffold-* -> $Name-*，Scaffold* -> $PascalName*）..." -ForegroundColor Cyan
$renamed = 0
$filesToRename = @(Get-ChildItem -Path $Dir -Recurse -File -Force |
    Where-Object { $_.Name -like 'scaffold-*' -or $_.Name -like 'Scaffold*' })
foreach ($f in $filesToRename) {
    $new = $f.Name -replace '^scaffold-', "$Name-"
    $new = $new -replace '^Scaffold', $PascalName
    Rename-Item -Path $f.FullName -NewName $new
    $renamed++
}
Write-Host "      已重命名 $renamed 个文件" -ForegroundColor Green

# ---------------- 4. 包目录重命名 com/scaffold -> <GroupId 路径> ----------------
Write-Host "[4/4] 重命名包目录（com/scaffold -> $GroupIdPath）..." -ForegroundColor Cyan
$comPkgDirs = @(Get-ChildItem -Path $Dir -Recurse -Directory -Force |
    Where-Object { $_.Name -eq 'scaffold' -and $_.Parent.Name -eq 'com' })
foreach ($d in $comPkgDirs) {
    $javaRoot   = Split-Path -Parent $d.Parent.FullName                     # .../src/main/java
    $targetRoot = Join-Path $javaRoot ($GroupIdPath -replace '/', '\')
    New-Item -ItemType Directory -Force -Path $targetRoot | Out-Null
    Get-ChildItem -Force $d.FullName | Move-Item -Destination $targetRoot -Force
    Remove-Item $d.FullName -Force
    # GroupId 首段不是 com 时，清理遗留的空 com 目录
    if ($FirstSeg -ne 'com') {
        $comDir = $d.Parent.FullName
        if (-not (Get-ChildItem -Force $comDir)) { Remove-Item $comDir -Force }
    }
}
Write-Host "      已迁移 $($comPkgDirs.Count) 个包根目录" -ForegroundColor Green

Write-Host ""
Write-Host "完成！新项目已生成：$Dir" -ForegroundColor Green
Write-Host "后续步骤："
Write-Host "  1. cd `"$Dir`" && mvn clean install -DskipTests 验证构建"
Write-Host "  2. sql/ 目录中大写 SCAFFOLD_ 前缀为占位符，请按项目名手工改名（本脚本只处理小写 token）"
Write-Host "  3. 检查 docker/.env（JWT_SECRET 等密钥）与 Nacos 上的 dataId（$Name-xxx-dev.yaml）"
Write-Host "  4. 全局搜索残留的 'scaffold' 字样做最后确认（README 散文等允许保留）"
