#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# setup.sh — First-time local setup (run once)
# ─────────────────────────────────────────────────────────────────
set -e

GREEN='\033[0;32m'; CYAN='\033[0;36m'; BOLD='\033[1m'; RED='\033[0;31m'; RESET='\033[0m'

info()    { echo -e "${CYAN}[setup]${RESET} $1"; }
success() { echo -e "${GREEN}[ok]${RESET}   $1"; }
error()   { echo -e "${RED}[err]${RESET}  $1"; exit 1; }

echo ""
echo -e "${BOLD}  Support Agent — First-time setup${RESET}"
echo ""

# ── Install tooling via Homebrew ──────────────────────────────────
info "Checking Homebrew..."
command -v brew >/dev/null 2>&1 || error "Homebrew not found. Install from https://brew.sh"

info "Installing openjdk@21, node, postgresql@16..."
brew install openjdk@21 node postgresql@16 2>/dev/null || true

# Add PostgreSQL to PATH if needed
PG_BIN="/opt/homebrew/opt/postgresql@16/bin"
if ! command -v psql >/dev/null 2>&1; then
  echo "export PATH=\"$PG_BIN:\$PATH\"" >> ~/.zshrc
  export PATH="$PG_BIN:$PATH"
  success "Added PostgreSQL to PATH in ~/.zshrc"
fi

success "Java: $(java -version 2>&1 | head -1)"
success "Node: $(node -v)"
success "psql: $(psql --version)"

# ── API key ───────────────────────────────────────────────────────
echo ""
info "Checking ANTHROPIC_API_KEY..."
if [ -z "$ANTHROPIC_API_KEY" ]; then
  echo -e "${CYAN}  Enter your Anthropic API key (starts with sk-ant-):${RESET}"
  read -r -p "  > " API_KEY
  if [ -z "$API_KEY" ]; then
    error "No API key provided. Get one at https://console.anthropic.com"
  fi
  echo "export ANTHROPIC_API_KEY=$API_KEY" >> ~/.zshrc
  export ANTHROPIC_API_KEY="$API_KEY"
  success "API key saved to ~/.zshrc"
else
  success "ANTHROPIC_API_KEY already set"
fi

# ── PostgreSQL setup ──────────────────────────────────────────────
echo ""
info "Starting PostgreSQL..."
brew services start postgresql@16
sleep 2

info "Creating database user and database..."
psql postgres <<EOF
DO \$\$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'support_user') THEN
    CREATE USER support_user WITH PASSWORD 'support_pass';
    RAISE NOTICE 'Created user support_user';
  ELSE
    RAISE NOTICE 'User support_user already exists';
  END IF;
END
\$\$;

SELECT 'Database exists' AS status WHERE EXISTS (
  SELECT FROM pg_database WHERE datname = 'supportdb'
)
UNION ALL
SELECT 'Creating database' AS status WHERE NOT EXISTS (
  SELECT FROM pg_database WHERE datname = 'supportdb'
);
EOF

# Create database if it doesn't exist
psql postgres -tc "SELECT 1 FROM pg_database WHERE datname='supportdb'" | grep -q 1 \
  || psql postgres -c "CREATE DATABASE supportdb OWNER support_user;"

psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE supportdb TO support_user;" 2>/dev/null || true

# Verify
psql -U support_user -d supportdb -c "SELECT 'Connection OK' AS status;" \
  && success "PostgreSQL ready: supportdb"

# ── Frontend dependencies ─────────────────────────────────────────
echo ""
info "Installing frontend npm packages..."
cd "$(dirname "$0")/frontend"
npm install --silent
success "Frontend dependencies installed"

# ── Gradle wrapper ────────────────────────────────────────────────
echo ""
info "Making Gradle wrapper executable..."
chmod +x "$(dirname "$0")/backend/gradlew"
success "gradlew is executable"

# ── Done ──────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}  Setup complete!${RESET}"
echo ""
echo -e "  Now run:  ${CYAN}./start.sh${RESET}"
echo ""
