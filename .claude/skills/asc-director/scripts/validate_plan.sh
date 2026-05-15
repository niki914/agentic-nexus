#!/bin/bash
# validate_plan.sh — 校验 plan.md 产物质量（Feature + Batch + Task 三级结构）
# 用法: bash scripts/validate_plan.sh <path_to_plan.md>

FILE="${1:?用法: bash validate_plan.sh <plan.md路径>}"
ERRORS=0
WARNINGS=0

echo "🔍 校验 plan.md: $FILE"
echo "---"

if [ ! -f "$FILE" ]; then
    echo "❌ 文件不存在: $FILE"
    exit 1
fi

# 检查 TBD/TODO/Placeholder
PLACEHOLDERS=$(grep -inE 'TBD|TODO|待补充|待确认|参考现有实现|与 T-[0-9]+ 类似|其他类似处理|添加合适的|编写测试[^：:]*$' "$FILE")
if [ -n "$PLACEHOLDERS" ]; then
    echo "❌ [E01] 发现 Placeholder（No Placeholders 原则违反）:"
    echo "$PLACEHOLDERS"
    ERRORS=$((ERRORS+1))
fi

# 检查必填章节
REQUIRED_SECTIONS=("Feature 列表" "Batch 编排" "任务清单" "实施步骤")
for section in "${REQUIRED_SECTIONS[@]}"; do
    if ! grep -q "$section" "$FILE"; then
        echo "❌ [E02] 缺少必填章节: $section"
        ERRORS=$((ERRORS+1))
    fi
done

# 检查 Feature ID 存在性
FEATURE_IDS=$(grep -oE 'F-[0-9]+' "$FILE" | sort -u)
FEATURE_COUNT=$(echo "$FEATURE_IDS" | grep -c "F-" 2>/dev/null || echo "0")
if [ "$FEATURE_COUNT" -eq 0 ]; then
    echo "❌ [E03] 未找到任何 Feature ID（F-xx）"
    ERRORS=$((ERRORS+1))
else
    echo "📦 找到 $FEATURE_COUNT 个 Feature: $(echo $FEATURE_IDS | tr '\n' ' ')"
fi

# 检查 Batch ID 存在性
BATCH_IDS=$(grep -oE 'B-[0-9]+' "$FILE" | sort -u)
BATCH_COUNT=$(echo "$BATCH_IDS" | grep -c "B-" 2>/dev/null || echo "0")
if [ "$BATCH_COUNT" -eq 0 ]; then
    echo "❌ [E04] 未找到任何 Batch ID（B-xx）"
    ERRORS=$((ERRORS+1))
else
    echo "📋 找到 $BATCH_COUNT 个 Batch: $(echo $BATCH_IDS | tr '\n' ' ')"
fi

# 检查任务 ID 存在性
TASK_IDS=$(grep -oE 'T-[0-9]+' "$FILE" | sort -u)
TASK_COUNT=$(echo "$TASK_IDS" | grep -c "T-" 2>/dev/null || echo "0")
if [ "$TASK_COUNT" -eq 0 ]; then
    echo "❌ [E05] 未找到任何任务 ID（T-xx）"
    ERRORS=$((ERRORS+1))
else
    echo "📋 找到 $TASK_COUNT 个任务: $(echo $TASK_IDS | tr '\n' ' ')"
fi

# 检查 Feature 列表是否包含预估 LOC
if ! grep -qE 'LOC\|预估' "$FILE"; then
    echo "⚠️ [W01] Feature 列表未找到预估 LOC 标注"
    WARNINGS=$((WARNINGS+1))
fi

# 检查复合任务描述（"然后"/"同时"/"并且"/"和" 连接不同动作）
COMPOUND=$(grep -inE 'T-[0-9]+.*\|.*(然后|同时|并且|以及)' "$FILE")
if [ -n "$COMPOUND" ]; then
    echo "⚠️ [W02] 疑似复合任务（应拆分）:"
    echo "$COMPOUND"
    WARNINGS=$((WARNINGS+1))
fi

# 检查每个 Task 是否有验收标准
TASKS_WITHOUT_AC=0
while IFS= read -r task_id; do
    TASK_LINE=$(grep "$task_id" "$FILE" | head -1)
    if echo "$TASK_LINE" | grep -q "|"; then
        LAST_COL=$(echo "$TASK_LINE" | awk -F'|' '{print $(NF-1)}' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        if [ -z "$LAST_COL" ] || [ "$LAST_COL" = "-" ]; then
            echo "❌ [E06] $task_id 缺少验收标准 (AC)"
            TASKS_WITHOUT_AC=$((TASKS_WITHOUT_AC+1))
        fi
    fi
done <<< "$TASK_IDS"

if [ $TASKS_WITHOUT_AC -gt 0 ]; then
    ERRORS=$((ERRORS+1))
fi

# 检查实施步骤是否覆盖所有 Task
STEPS_SECTIONS=$(grep -cE '^### T-[0-9]+' "$FILE" 2>/dev/null || echo "0")
if [ "$STEPS_SECTIONS" -lt "$TASK_COUNT" ]; then
    echo "⚠️ [W03] 实施步骤章节数 ($STEPS_SECTIONS) 少于任务数 ($TASK_COUNT)"
    WARNINGS=$((WARNINGS+1))
fi

# 检查复杂度标记
if ! grep -qE '\|\s*[LMH]\s*\|' "$FILE"; then
    echo "ℹ️ [I01] 未找到复杂度标记（L/M/H）— 该列为可选"
fi

# 检查目标文件列
if ! grep -qE '`[^`]+\.[a-z]+`' "$FILE"; then
    echo "⚠️ [W04] 未找到明确的目标文件路径"
    WARNINGS=$((WARNINGS+1))
fi

# 检查版本号
if ! grep -qE 'v[0-9]+\.[0-9]+' "$FILE"; then
    echo "⚠️ [W05] 未找到版本号"
    WARNINGS=$((WARNINGS+1))
fi

# 检查审查修正记录
if ! grep -q "审查修正记录" "$FILE"; then
    echo "⚠️ [W06] 未找到审查修正记录章节"
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
