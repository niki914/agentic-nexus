#!/bin/bash
# check_phase_gate.sh — 阶段门禁检查
# 用法: bash scripts/check_phase_gate.sh <project_dir> <target_phase>
# 示例: bash scripts/check_phase_gate.sh ./docs/.asc_task/comment_filter 1

PROJECT_DIR="${1:?用法: bash check_phase_gate.sh <项目目录> <目标阶段号>}"
TARGET_PHASE="${2:?请指定目标阶段号 (1/2/3)}"
ERRORS=0

echo "🔍 阶段门禁检查: 进入 Phase $TARGET_PHASE"
echo "📁 项目目录: $PROJECT_DIR"
echo "---"

if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ 项目目录不存在: $PROJECT_DIR"
    exit 1
fi

if [ ! -f "$PROJECT_DIR/progress.md" ]; then
    echo "❌ progress.md 不存在。MUST 先完成 Phase 0。"
    exit 1
fi

check_file_exists_and_nonempty() {
    local file="$PROJECT_DIR/$1"
    local desc="$2"
    if [ ! -f "$file" ]; then
        echo "❌ $desc 不存在: $file"
        ERRORS=$((ERRORS+1))
    elif [ ! -s "$file" ]; then
        echo "❌ $desc 为空文件: $file"
        ERRORS=$((ERRORS+1))
    else
        echo "✅ $desc 存在且非空"
    fi
}

case $TARGET_PHASE in
    1)
        check_file_exists_and_nonempty "tech_survey.md" "tech_survey.md"
        ;;
    2)
        check_file_exists_and_nonempty "tech_survey.md" "tech_survey.md"
        check_file_exists_and_nonempty "tech_design.md" "tech_design.md"
        ;;
    3)
        check_file_exists_and_nonempty "tech_survey.md" "tech_survey.md"
        check_file_exists_and_nonempty "tech_design.md" "tech_design.md"
        check_file_exists_and_nonempty "plan.md" "plan.md"
        ;;
    *)
        echo "⚠️ 未知阶段号: $TARGET_PHASE (应为 1/2/3)"
        exit 1
        ;;
esac

PREV_PHASE=$((TARGET_PHASE-1))
if grep -qiE "Phase $PREV_PHASE.*(Done|完成|\[Done\]|✅)" "$PROJECT_DIR/progress.md"; then
    echo "✅ progress.md 确认 Phase $PREV_PHASE 已完成"
else
    echo "⚠️ progress.md 中未找到 Phase $PREV_PHASE Done 标记（可能是格式差异，请人工确认）"
fi

echo "---"
if [ $ERRORS -eq 0 ]; then
    echo "✅ 门禁检查通过，可以进入 Phase $TARGET_PHASE"
else
    echo "❌ 门禁检查失败：$ERRORS 个前序文件问题。MUST 修复后才能进入 Phase $TARGET_PHASE。"
    exit 1
fi
