#!/bin/bash
# validate_tech_design.sh — 校验 tech_design.md 产物质量
# 用法: bash scripts/validate_tech_design.sh <path_to_tech_design.md>

FILE="${1:?用法: bash validate_tech_design.sh <tech_design.md路径>}"
ERRORS=0
WARNINGS=0

echo "🔍 校验 tech_design.md: $FILE"
echo "---"

if [ ! -f "$FILE" ]; then
    echo "❌ 文件不存在: $FILE"
    exit 1
fi

# 检查 TBD/TODO
TODOS=$(grep -inE 'TBD|TODO|待补充|待确认|to be determined' "$FILE")
if [ -n "$TODOS" ]; then
    echo "❌ [E01] 发现未完成项:"
    echo "$TODOS"
    ERRORS=$((ERRORS+1))
fi

# 检查模糊描述词（零模糊原则）
# 注意：使用词边界避免误匹配。"适当"/"合理"/"大概"等是独立词汇不会误匹配。
# "类似" 需要排除在代码块/表头中的使用。"相应的" 需要排除 "Properties" 等英文。
# 排除代码块内容、反面示例行、模板行
FUZZY=$(awk '
  /^```/{in_code=!in_code; next}
  in_code{next}
  /禁止写法|应该写成|No Placeholders|反模式/{next}
  /适当|合理地|大概|差不多|参考现有|as needed/{print NR": "$0}
' "$FILE")
if [ -n "$FUZZY" ]; then
    echo "❌ [E02] 发现模糊描述（违反零模糊原则）:"
    echo "$FUZZY"
    ERRORS=$((ERRORS+1))
fi

# 检查必填章节
MUST_HAVE_SECTIONS=(
    "架构特征分析"
    "审查发现|审查摘要|Review Findings"
    "设计决策|决策记录|Decision"
    "API 设计|API设计|详细 API|Detailed API"
    "目录结构|目录树|Directory|文件改动范围"
)
for section_variants in "${MUST_HAVE_SECTIONS[@]}"; do
    FOUND=0
    IFS='|' read -ra VARIANTS <<< "$section_variants"
    for variant in "${VARIANTS[@]}"; do
        if grep -qi "$variant" "$FILE"; then
            FOUND=1
            break
        fi
    done
    if [ "$FOUND" -eq 0 ]; then
        echo "❌ [E03] 缺少必填章节: ${VARIANTS[0]}（或其变体: $section_variants）"
        ERRORS=$((ERRORS+1))
    fi
done

# 检查 API 签名完整性
# 检查方法签名完整性：排除已有 → 返回类型说明的行，排除"→ 移除"标记
INCOMPLETE_SIGS=$(grep -nE '^- `[^`]*\([^)]*\)[^:]*`' "$FILE" | grep -vE '\->|→' | grep -vE ': ')
if [ -n "$INCOMPLETE_SIGS" ]; then
    echo "⚠️ [W01] 以下方法签名可能缺少返回类型:"
    echo "$INCOMPLETE_SIGS"
    WARNINGS=$((WARNINGS+1))
fi

# 检查隔离验证（What/How/Depends）
CLASS_COUNT=$(grep -c "### Class:" "$FILE" 2>/dev/null || true)
CLASS_COUNT=${CLASS_COUNT:-0}
INTERFACE_COUNT=$(grep -c "### Interface:" "$FILE" 2>/dev/null || true)
INTERFACE_COUNT=${INTERFACE_COUNT:-0}
TOTAL_DEFS=$((CLASS_COUNT + INTERFACE_COUNT))
ISOLATION_COUNT=$(grep -c "隔离验证" "$FILE" 2>/dev/null || true)
ISOLATION_COUNT=${ISOLATION_COUNT:-0}
if [ "$TOTAL_DEFS" -gt 0 ] && [ "$ISOLATION_COUNT" -lt "$TOTAL_DEFS" ]; then
    echo "❌ [E04] 部分模块缺少隔离验证: 定义了 $TOTAL_DEFS 个模块，但只有 $ISOLATION_COUNT 个隔离验证"
    ERRORS=$((ERRORS+1))
fi

if [ "$TOTAL_DEFS" -eq 0 ]; then
    echo "⚠️ [W02] 未找到 Class 或 Interface 定义（### Class: / ### Interface: 格式）"
    WARNINGS=$((WARNINGS+1))
fi

# 检查版本号
if ! grep -qE 'v[0-9]+\.[0-9]+' "$FILE"; then
    echo "⚠️ [W03] 未找到版本号"
    WARNINGS=$((WARNINGS+1))
fi

# 检查目录树
if ! grep -q '├──\|└──' "$FILE"; then
    echo "⚠️ [W04] 未找到目录树结构"
    WARNINGS=$((WARNINGS+1))
fi

# 汇总
echo "---"
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ 校验通过"
elif [ $ERRORS -eq 0 ]; then
    echo "✅ 校验通过（$WARNINGS 个警告）"
else
    echo "❌ 校验失败：$ERRORS 个错误，$WARNINGS 个警告"
    exit 1
fi
