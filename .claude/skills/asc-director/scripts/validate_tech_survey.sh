#!/bin/bash
# validate_tech_survey.sh
FILE="${1:?用法: bash validate_tech_survey.sh <tech_survey.md路径>}"
ERRORS=0
WARNINGS=0
echo "🔍 校验 tech_survey.md: $FILE"
echo "---"
if [ ! -f "$FILE" ]; then
    echo "❌ 文件不存在: $FILE"
    exit 1
fi
# E01: 检查未完成项（排除"开放问题"章节中的"待确认"）
OPEN_Q_LINE=$(grep -n "^## .*开放问题" "$FILE" | head -1 | cut -d: -f1)
if [ -n "$OPEN_Q_LINE" ]; then
    TODOS=$(head -n "$((OPEN_Q_LINE-1))" "$FILE" | grep -inE 'TBD|TODO|待补充|待确认|to be determined|to be decided')
else
    TODOS=$(grep -inE 'TBD|TODO|待补充|待确认|to be determined|to be decided' "$FILE")
fi
if [ -n "$TODOS" ]; then
    echo "❌ [E01] 发现未完成项:"
    echo "$TODOS"
    ERRORS=$((ERRORS+1))
fi
REQUIRED_SECTIONS=("需求概要" "项目背景与目标" "核心功能清单" "约束条件" "验收标准" "需求澄清记录" "审查摘要" "现状映射表" "详细变更方案")
for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "$section" "$FILE"; then
        echo "❌ [E02] 缺少必填章节: $section"
        ERRORS=$((ERRORS+1))
    fi
done
for field in "选定方案" "核心思路" "YAGNI"; do
    if ! grep -q "$field" "$FILE"; then
        echo "❌ [E03] 方案概要缺少必填字段: $field"
        ERRORS=$((ERRORS+1))
    fi
done
FUNC_LINES=$(grep -c "| F-" "$FILE" 2>/dev/null || echo "0")
if [ "$FUNC_LINES" -eq 0 ]; then
    echo "❌ [E04] 核心功能清单为空"
    ERRORS=$((ERRORS+1))
fi
AC_LINES=$(grep -c "| AC-" "$FILE" 2>/dev/null || echo "0")
if [ "$AC_LINES" -eq 0 ]; then
    echo "❌ [E05] 验收标准为空"
    ERRORS=$((ERRORS+1))
fi
FUZZY_AC=$(grep -inE 'AC-.*\|(.*功能正常.*|.*实现了.*|.*可以用.*)' "$FILE")
if [ -n "$FUZZY_AC" ]; then
    echo "⚠️ [W01] 验收标准疑似模糊:"
    echo "$FUZZY_AC"
    WARNINGS=$((WARNINGS+1))
fi
SHOULD_SECTIONS=("选型对比表" "决策记录" "难点预判")
for section in "${SHOULD_SECTIONS[@]}"; do
    if ! grep -q "$section" "$FILE"; then
        echo "⚠️ [W02] 缺少建议章节: $section"
        WARNINGS=$((WARNINGS+1))
    fi
done
COMPARISON_LINES=$(grep -cE '^\|[^|]+\|.*\|.*\|.*\|.*\|.*\|' "$FILE" 2>/dev/null || true)
COMPARISON_LINES=${COMPARISON_LINES:-0}
if [ "$COMPARISON_LINES" -lt 3 ]; then
    echo "⚠️ [W03] 选型对比表数据行不足"
    WARNINGS=$((WARNINGS+1))
fi
for field in "PM" "架构师"; do
    if ! grep -q "$field" "$FILE"; then
        echo "❌ [E06] 审查摘要缺少: $field 视角审查"
        ERRORS=$((ERRORS+1))
    fi
done
if ! grep -qE '✅|⚠️|✨|复用|重构|新增' "$FILE"; then
    echo "⚠️ [W04] 现状映射表缺少匹配度标记"
    WARNINGS=$((WARNINGS+1))
fi
if ! grep -qE 'v[0-9]+\.[0-9]+' "$FILE"; then
    echo "⚠️ [W05] 未找到版本号"
    WARNINGS=$((WARNINGS+1))
fi
EMPTY_SECTIONS=$(awk '/^## /{if(prev_section && !has_content) print prev_section; prev_section=$0; has_content=0; next} /^[^#]/{if(NF>0) has_content=1} END{if(prev_section && !has_content) print prev_section}' "$FILE")
if [ -n "$EMPTY_SECTIONS" ]; then
    echo "⚠️ [W06] 疑似空章节:"
    echo "$EMPTY_SECTIONS"
    WARNINGS=$((WARNINGS+1))
fi
echo "---"
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ 校验通过"
elif [ $ERRORS -eq 0 ]; then
    echo "✅ 校验通过（$WARNINGS 个警告）"
else
    echo "❌ 校验失败：$ERRORS 个错误，$WARNINGS 个警告"
    exit 1
fi
