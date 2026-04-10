#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# start.sh — Customer Support Agent
# Starts PostgreSQL, Spring Boot backend, and React frontend.
# Usage:  ./start.sh
# ─────────────────────────────────────────────────────────────────
set -e

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; RESET='\033[0m'

info()    { echo -e "${CYAN}[info]${RESET} $1"; }
success() { echo -e "${GREEN}[ok]${RESET}   $1"; }
warn()    { echo -e "${YELLOW}[warn]${RESET} $1"; }
error()   { echo -e "${RED}[err]${RESET}  $1"; exit 1; }

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

echo ""
echo -e "${BOLD}  Customer Support Agent${RESET}"
echo -e "  Spring Boot (Gradle) + React + PostgreSQL + Claude"
echo ""

# ── 1. Check ANTHROPIC_API_KEY ────────────────────────────────────
if [ -z "$ANTHROPIC_API_KEY" ]; then
  error "ANTHROPIC_API_KEY is not set.\n  Run: export ANTHROPIC_API_KEY=sk-ant-..."
fi
success "ANTHROPIC_API_KEY found"

# ── 2. Check required tools ───────────────────────────────────────
check_cmd() {
  command -v "$1" >/dev/null 2>&1 || error "$1 not found. Install with: $2"
  success "$1 $(${1} --version 2>&1 | head -1)"
}
check_cmd java  "brew install openjdk@21"
check_cmd node  "brew install node"
check_cmd psql  "brew install postgresql@16"

# ── 3. Start PostgreSQL ───────────────────────────────────────────
info "Starting PostgreSQL..."
brew services start postgresql@16 2>/dev/null || true
sleep 2

# Verify connection
if psql -U support_user -d supportdb -c "SELECT 1" >/dev/null 2>&1; then
  success "PostgreSQL connected (supportdb)"
else
  warn "Could not connect to supportdb. Running first-time setup..."
  echo ""
  echo -e "${YELLOW}  Run these commands in a new terminal, then re-run ./start.sh:${RESET}"
  echo ""
  echo "    psql postgres"
  echo "    CREATE USER support_user WITH PASSWORD 'support_pass';"
  echo "    CREATE DATABASE supportdb OWNER support_user;"
  echo "    GRANT ALL PRIVILEGES ON DATABASE supportdb TO support_user;"
  echo "    \\q"
  echo ""
  exit 1
fi

# ── 4. Start backend ──────────────────────────────────────────────
info "Starting Spring Boot backend on port 8080..."
cd "$BACKEND_DIR"
./gradlew bootRun --quiet &
BACKEND_PID=$!

# Wait for backend to be ready (max 60s)
info "Waiting for backend to start..."
for i in $(seq 1 30); do
  if curl -sf http://localhost:8080/api/agent/health >/dev/null 2>&1; then
    success "Backend ready at http://localhost:8080"
    break
  fi
  if [ $i -eq 30 ]; then
    error "Backend did not start within 60s. Check logs above."
  fi
  sleep 2
done

# ── 5. Start frontend ─────────────────────────────────────────────
info "Installing frontend dependencies..."
cd "$FRONTEND_DIR"
npm install --silent

info "Starting React frontend on port 3000..."
npm run dev &
FRONTEND_PID=$!
sleep 3
success "Frontend ready at http://localhost:3000"

# ── 6. Summary ────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}  All services running${RESET}"
echo ""
echo -e "  Frontend  →  ${CYAN}http://localhost:3000${RESET}"
echo -e "  Backend   →  ${CYAN}http://localhost:8080${RESET}"
echo -e "  DB admin  →  ${CYAN}psql -U support_user -d supportdb${RESET}"
echo ""
echo -e "  ${YELLOW}Press Ctrl+C to stop all services${RESET}"
echo ""

# ── 7. Cleanup on exit ────────────────────────────────────────────
cleanup() {
  echo ""
  info "Shutting down..."
  kill $BACKEND_PID  2>/dev/null || true
  kill $FRONTEND_PID 2>/dev/null || true
  brew services stop postgresql@16 2>/dev/null || true
  success "All services stopped"
}
trap cleanup EXIT INT TERM

# Keep script alive
wait $BACKEND_PID
