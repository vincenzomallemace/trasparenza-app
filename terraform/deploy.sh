#!/usr/bin/env bash
# deploy.sh — Deploy trasparenza-backend to Railway
# Usage: RAILWAY_TOKEN=<your_token> ./deploy.sh
# Or:    ./deploy.sh            (after 'railway login')
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../backend" && pwd)"
PROJECT_NAME="trasparenza-app"
SERVICE_NAME="trasparenza-backend"

print_step() { echo ""; echo "▶ $1"; }

# ─── Check Railway CLI ────────────────────────────────────────────────────────
if ! command -v railway &>/dev/null; then
  echo "❌ Railway CLI not found. Install with: brew install railway"
  exit 1
fi

# ─── Auth check ───────────────────────────────────────────────────────────────
print_step "Checking Railway auth…"
if [[ -n "${RAILWAY_TOKEN:-}" ]]; then
  echo "Using RAILWAY_TOKEN from environment"
else
  railway whoami || { echo "❌ Not logged in. Run: railway login"; exit 1; }
fi

# ─── Create / link project ────────────────────────────────────────────────────
print_step "Creating/linking Railway project: $PROJECT_NAME"
cd "$BACKEND_DIR"

# Check if already linked
if railway status &>/dev/null 2>&1; then
  echo "Already linked to a Railway project"
else
  # Create new project
  railway init --name "$PROJECT_NAME" || true
fi

# ─── Deploy ───────────────────────────────────────────────────────────────────
print_step "Deploying backend to Railway…"
railway up --service "$SERVICE_NAME" --detach

# ─── Get URL ──────────────────────────────────────────────────────────────────
print_step "Fetching deployment URL…"
sleep 5
DOMAIN=$(railway domain 2>/dev/null || echo "")
if [[ -n "$DOMAIN" ]]; then
  echo ""
  echo "✅ Backend deployed!"
  echo "   🌍 URL: https://$DOMAIN"
  echo "   🏥 Health: https://$DOMAIN/health"
  echo ""
  echo "👉 Update android-app/app/build.gradle.kts:"
  echo "   buildConfigField(\"String\", \"API_BASE_URL\", \"\\\"https://$DOMAIN/api/\\\"\")"
else
  echo "✅ Deploy queued. Check Railway dashboard for URL."
fi

print_step "Done!"
