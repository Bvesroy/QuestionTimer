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

echo "4. Creating GitHub Release v1.6.0 with QuestionTimer.apk..."
RELEASE_APK="$DIR/release/QuestionTimer.apk"
if [ -f "$RELEASE_APK" ]; then
    gh release create v1.6.0 "$RELEASE_APK" \
        --title "QuestionTimer v1.6.0 — The Anti-Delusion Release" \
        --notes "### ⏱️ QuestionTimer v1.6.0

#### 🚀 What's New:
- **Invisible Two-Zone Interaction:** Tap top half to pause & mark ❌ Wrong (-1 / -2); tap bottom half to pause & mark ✅ Correct (+4).
- **Vibrant Click Feedback:** Red and green visual feedback overlays with customizable transitions (Fade, Ripple, Instant Pop) and durations (150ms - 1200ms).
- **JEE Main & JEE Advanced Scoring:** Automated estimated marks calculation based on standard marking schemes.
- **Pure AMOLED Black:** Minimalist distraction-free layout optimized for long study sessions.
- **100% Offline & Private:** Zero network permissions required." 2>/dev/null || echo "Release already exists or skipped."
fi

echo "=== Successfully published QuestionTimer to GitHub! ==="
