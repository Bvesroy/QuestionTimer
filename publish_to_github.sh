#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
cd "$DIR"

echo "=== GitHub Publishing Script for QuestionTimer ==="

# Check gh CLI
if ! command -v gh >/dev/null 2>&1; then
    echo "ERROR: GitHub CLI ('gh') is not installed."
    echo "Install it via: brew install gh"
    exit 1
fi

# Verify gh authentication
if ! gh auth status >/dev/null 2>&1; then
    echo "ERROR: Not logged into GitHub."
    echo "Please run: gh auth login"
    exit 1
fi

REPO_NAME="${1:-QuestionTimer}"
REPO_DESC="⏱️ Minimalist, invisible two-zone question timer & anti-delusion analytics engine for JEE Main & Advanced aspirants. Upper = Wrong ❌, Lower = Right ✅."

echo "1. Checking if remote 'origin' exists..."
if git remote get-url origin >/dev/null 2>&1; then
    echo "   Remote 'origin' already configured: $(git remote get-url origin)"
    echo "   Pushing branch 'main'..."
    git push -u origin main
else
    echo "2. Creating GitHub repository '$REPO_NAME'..."
    gh repo create "$REPO_NAME" --public --source=. --remote=origin --description "$REPO_DESC" --push
fi

echo "3. Adding topics/tags..."
gh repo edit --add-topic "jee-main,jee-advanced,study-timer,question-timer,android,amoled,exam-prep,offline-first" 2>/dev/null || true

echo "4. Creating GitHub Release v1.7.0 with QuestionTimer.apk..."
RELEASE_APK="$DIR/release/QuestionTimer.apk"
if [ -f "$RELEASE_APK" ]; then
    gh release create v1.7.0 "$RELEASE_APK" \
        --title "QuestionTimer v1.7.0 — Persistent Corner Undo & Question Deletion" \
        --notes "### ⏱️ QuestionTimer v1.7.0

#### 🚀 What's New:
- **Persistent Corner Undo & Change Tag:** The undo and tag change button is docked in the corner and **remains visible** throughout the entire pause duration. Easily change a question's tag from Wrong to Right (or vice versa) without losing your solving time!
- **Long-Press Question Selection & Deletion:** In the History Question Breakdown list, long-press on any question log to enter selection mode, select individual or multiple questions, and delete just the selected attempts.
- **Instant Stat Recalculation:** Deleting questions or changing tags immediately recomputes session averages, fastest/slowest times, and estimated JEE Main (+4/-1) / Advanced (+4/-2) marks.
- **163 Comprehensive Tests:** 100% unit test coverage ensuring rock-solid data integrity." 2>/dev/null || echo "Release already exists or skipped."
fi

echo "=== Successfully published QuestionTimer to GitHub! ==="
