#!/usr/bin/env bash
# ============================================================================
# 脚手架新项目改名脚本（bash 版；与 scripts/new-project.ps1 逻辑严格一致）
#
# 用法：
#   ./scripts/new-project.sh -GroupId com.yourco -Name yourproj
#   ./scripts/new-project.sh -GroupId com.yourco.apps -Name yourproj -Dir /e/workspace/yourproj
#   （也兼容长选项 --group-id / --name / --dir）
#
# 功能：把整个脚手架复制到 -Dir 指定的新目录（源目录只读不动），然后仅对新目录
# 内的文本源文件做全局替换。替换规则（顺序执行）：
#   1) com.scaffold          -> <GroupId>
#   2) com/scaffold          -> <GroupId 的路径形式>
#   3) scaffold-             -> <Name>-          （构件名/服务名/文件名引用，
#                                                 spring.application.name: scaffold-xxx 一并覆盖）
#   4) "scaffold.            -> "<Name>.         （双引号配置前缀）
#   5) 'scaffold.            -> '<Name>.         （单引号配置前缀）
#   6) "scaffold:            -> "<Name>:         （Redis key 前缀）
#   7) scaffold_trace_id     -> <Name>_trace_id
#   8) scaffoldRedisTemplate -> <Name>RedisTemplate
#   9) scaffold.version      -> <Name>.version   （根 POM 版本属性）
#  10) ^scaffold:            -> <Name>:          （仅 yml/yaml 行首配置前缀）
#  11) Scaffold              -> <Name> 大驼峰     （应用主类 ScaffoldXxxApplication 等）
#
# 目录重命名：*/com/scaffold 包目录 -> */<GroupId 路径>（多段 GroupId 自动建级联目录）
# 文件重命名：scaffold-defaults.yml / scaffold-xxx-local.yml / ScaffoldXxxApplication.java 等
# 处理范围：java xml yml yaml vm sql properties imports md factories ps1 sh bat txt json proto
# 跳过：target/、.git/、logs/ 目录与 *.log 文件（复制阶段即剔除）
# 幂等与失败安全：先整树复制、后改名替换；目标目录必须不存在或为空，否则拒绝执行
# ============================================================================
set -euo pipefail

usage() {
    sed -n '2,25p' "$0" | grep -E '^#' | sed 's/^# \{0,1\}//'
    exit 1
}

GROUP=""; NAME=""; DIR=""
while [ $# -gt 0 ]; do
    case "$1" in
        -GroupId|--group-id) GROUP="${2:-}"; shift 2 ;;
        -Name|--name)        NAME="${2:-}";  shift 2 ;;
        -Dir|--dir)          DIR="${2:-}";   shift 2 ;;
        -h|--help)           usage ;;
        *) echo "未知参数：$1"; usage ;;
    esac
done

# ---------------- 0. 参数校验 ----------------
[ -n "$GROUP" ] || { echo "缺少 -GroupId"; usage; }
[ -n "$NAME" ]  || { echo "缺少 -Name";    usage; }

if ! echo "$GROUP" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$'; then
    echo "GroupId 格式不合法：'$GROUP'，应为点分小写形式，如 com.yourco（至少两段）" >&2
    exit 1
fi
if ! echo "$NAME" | grep -Eq '^[a-z][a-z0-9]*(-[a-z][a-z0-9]*)*$'; then
    echo "Name 格式不合法：'$NAME'，应为小写字母开头的小写短横线串，如 yourproj" >&2
    exit 1
fi

# 解析仓库根目录（本脚本位于 <root>/scripts/）
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [ -z "$DIR" ]; then DIR="$(dirname "$ROOT")/$NAME"; fi
DIR="$(mkdir -p "$DIR" 2>/dev/null && cd "$DIR" && pwd)" || exit 1

if [ -n "$(ls -A "$DIR")" ]; then
    echo "目标目录已存在且非空：$DIR —— 为幂等安全拒绝执行，请换目录或先清空" >&2
    exit 1
fi

# 派生形式
GROUPPATH="${GROUP//./\/}"                 # com/yourco
FIRSTSEG="${GROUP%%.*}"                    # com
PASCAL="$(echo "$NAME" | awk -F- '{for(i=1;i<=NF;i++) $i=toupper(substr($i,1,1)) substr($i,2); print $0}')"
PASCAL="${PASCAL// /}"                     # your-app -> YourApp

# ---------------- 1. 复制整树（跳过 target/.git/logs 与 *.log） ----------------
echo "[1/4] 复制 $ROOT -> $DIR ..."
cp -a "$ROOT/." "$DIR/"
# 剔除不需要的目录/文件
find "$DIR" -type d \( -name target -o -name .git -o -name logs \) -prune -exec rm -rf {} +
find "$DIR" -type f -name '*.log' -delete

# ---------------- 2. 文本内容替换 ----------------
echo "[2/4] 全局替换内容（com.scaffold=$GROUP, scaffold-=$NAME-）..."
CHANGED=0
while IFS= read -r -d '' f; do
    ext="${f##*.}"; ext="$(echo "$ext" | tr 'A-Z' 'a-z')"
    case "$ext" in
        java|xml|yml|yaml|vm|sql|properties|imports|md|factories|ps1|sh|bat|txt|json|proto) ;;
        *) continue ;;
    esac
    tmp="$(mktemp)"
    if [ "$ext" = "yml" ] || [ "$ext" = "yaml" ]; then
        sed -e "s/com\.scaffold/$GROUP/g" \
            -e "s|com/scaffold|$GROUPPATH|g" \
            -e "s/scaffold-/$NAME-/g" \
            -e "s/\"scaffold\./\"$NAME./g" \
            -e "s/'scaffold\./'$NAME./g" \
            -e "s/\"scaffold:/\"$NAME:/g" \
            -e "s/scaffold_trace_id/${NAME}_trace_id/g" \
            -e "s/scaffoldRedisTemplate/${NAME}RedisTemplate/g" \
            -e "s/scaffold\.version/$NAME.version/g" \
            -e "s/Scaffold/$PASCAL/g" \
            -e "s/^scaffold:/$NAME:/" \
            "$f" > "$tmp"
    else
        sed -e "s/com\.scaffold/$GROUP/g" \
            -e "s|com/scaffold|$GROUPPATH|g" \
            -e "s/scaffold-/$NAME-/g" \
            -e "s/\"scaffold\./\"$NAME./g" \
            -e "s/'scaffold\./'$NAME./g" \
            -e "s/\"scaffold:/\"$NAME:/g" \
            -e "s/scaffold_trace_id/${NAME}_trace_id/g" \
            -e "s/scaffoldRedisTemplate/${NAME}RedisTemplate/g" \
            -e "s/scaffold\.version/$NAME.version/g" \
            -e "s/Scaffold/$PASCAL/g" \
            "$f" > "$tmp"
    fi
    if ! cmp -s "$f" "$tmp"; then
        mv "$tmp" "$f"
        CHANGED=$((CHANGED + 1))
    else
        rm -f "$tmp"
    fi
done < <(find "$DIR" -type f -print0)
echo "      已更新 $CHANGED 个文本文件"

# ---------------- 3. 文件重命名 ----------------
echo "[3/4] 重命名文件（scaffold-* -> $NAME-*，Scaffold* -> $PASCAL*）..."
RENAMED=0
while IFS= read -r -d '' f; do
    d="$(dirname "$f")"; b="$(basename "$f")"
    new="${b/scaffold\-/$NAME-}"
    new="${new/Scaffold/$PASCAL}"
    mv "$f" "$d/$new"
    RENAMED=$((RENAMED + 1))
done < <(find "$DIR" -type f \( -name 'scaffold-*' -o -name 'Scaffold*' \) -print0)
echo "      已重命名 $RENAMED 个文件"

# ---------------- 4. 包目录重命名 com/scaffold -> <GroupId 路径> ----------------
echo "[4/4] 重命名包目录（com/scaffold -> $GROUPPATH）..."
COMDIRS=0
while IFS= read -r -d '' d; do
    parent="$(dirname "$d")"
    [ "$(basename "$parent")" = "com" ] || continue
    javaRoot="$(dirname "$parent")"                 # .../src/main/java
    targetRoot="$javaRoot/$GROUPPATH"
    mkdir -p "$targetRoot"
    # 含隐藏文件一并移动
    find "$d" -mindepth 1 -maxdepth 1 -exec mv {} "$targetRoot"/ \;
    rmdir "$d"
    COMDIRS=$((COMDIRS + 1))
    # GroupId 首段不是 com 时，清理遗留的空 com 目录
    if [ "$FIRSTSEG" != "com" ]; then
        rmdir "$parent" 2>/dev/null || true
    fi
done < <(find "$DIR" -type d -name scaffold -print0)
echo "      已迁移 $COMDIRS 个包根目录"

echo ""
echo "完成！新项目已生成：$DIR"
echo "后续步骤："
echo "  1. cd \"$DIR\" && mvn clean install -DskipTests 验证构建"
echo "  2. sql/ 目录中大写 SCAFFOLD_ 前缀为占位符，请按项目名手工改名（本脚本只处理小写 token）"
echo "  3. 检查 docker/.env（JWT_SECRET 等密钥）与 Nacos 上的 dataId（$NAME-xxx-dev.yaml）"
echo "  4. 全局搜索残留的 'scaffold' 字样做最后确认（README 散文等允许保留）"
