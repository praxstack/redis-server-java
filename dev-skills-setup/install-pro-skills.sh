#!/usr/bin/env bash
# Super-pro developer skills stack installer for Cursor Cloud Agents
# Layered architecture: discover → interrogate/spec → plan → implement → review →
#   security → browser QA → ship → learn
#
# Usage: ./install-pro-skills.sh
#
# WARNING: Do NOT activate every methodology on every task. Pick ONE primary workflow
# (pstack, superpowers, gstack, or compound-engineering) and pull in other layers
# situationally. Running all auto-triggers simultaneously causes context rot and noise.
set -euo pipefail

SKILLS="npx skills@latest"
AGENT_BROWSER_PREFIX="${HOME}/.local"

install_skillpack() {
  local repo="$1"
  shift
  local args=()
  local label="$repo"

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --skill) args+=(-s "$2"); shift 2 ;;
      --label) label="$2"; shift 2 ;;
      *) echo "Unknown arg: $1"; return 1 ;;
    esac
  done

  echo "==> Installing: $label"
  if $SKILLS add "$repo" -g -y -a cursor "${args[@]}"; then
    echo "    OK: $label"
  else
    echo "    FAILED: $label (continuing)"
    return 0
  fi
}

install_pstack() {
  echo "==> Installing pstack (poteto's Cursor engineering skills)..."
  local plugins_dir="${TMPDIR:-/tmp}/cursor-plugins"
  if [ ! -d "$plugins_dir/pstack/skills" ]; then
    git clone --single-branch --depth 1 https://github.com/cursor/plugins.git "$plugins_dir"
  fi
  for skill in "$plugins_dir/pstack/skills"/*/; do
    local name
    name=$(basename "$skill")
    mkdir -p "$HOME/.cursor/skills/$name" "$HOME/.agents/skills/$name"
    cp -r "$skill"/* "$HOME/.cursor/skills/$name/"
    cp -r "$skill"/* "$HOME/.agents/skills/$name/"
  done
}

install_gstack() {
  echo "==> Installing gstack (Garry Tan's engineering team)..."
  if ! command -v bun >/dev/null 2>&1; then
    curl -fsSL https://bun.sh/install | bash
    export PATH="$HOME/.bun/bin:$PATH"
  fi
  local gstack_dir="${TMPDIR:-/tmp}/gstack"
  if [ ! -d "$gstack_dir/.git" ]; then
    git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git "$gstack_dir"
  fi
  # Cursor caveat: gstack issue #2361 — ./setup --host cursor is required (not claude/codex)
  (cd "$gstack_dir" && ./setup --host cursor)
}

install_agent_browser_cli() {
  echo "==> Installing agent-browser CLI..."
  if command -v agent-browser >/dev/null 2>&1; then
    echo "    agent-browser already on PATH"
    agent-browser install || true
    return 0
  fi
  if npm install -g agent-browser --prefix "$AGENT_BROWSER_PREFIX"; then
  elif npm install -g agent-browser 2>/dev/null; then
    :
  else
    echo "    WARN: global npm install failed; try: npx agent-browser install"
    return 0
  fi
  export PATH="$AGENT_BROWSER_PREFIX/bin:$PATH"
  agent-browser install || npx agent-browser install || true
}

install_compound_engineering() {
  echo "==> Installing compound-engineering..."
  if install_skillpack EveryInc/compound-engineering-plugin --skill '*'; then
    return 0
  fi
  echo "    npx failed — cloning repo and copying skills manually..."
  local ce_dir="${TMPDIR:-/tmp}/compound-engineering-plugin"
  git clone --single-branch --depth 1 https://github.com/EveryInc/compound-engineering-plugin.git "$ce_dir"
  for skill in "$ce_dir"/skills/*/ "$ce_dir"/packages/*/skills/*/; do
    if [ -f "$skill/SKILL.md" ]; then
      local name
      name=$(basename "$skill")
      mkdir -p "$HOME/.cursor/skills/$name" "$HOME/.agents/skills/$name"
      cp -r "$skill"/* "$HOME/.cursor/skills/$name/"
      cp -r "$skill"/* "$HOME/.agents/skills/$name/"
    fi
  done
  echo "    Also run in Cursor: /add-plugin compound-engineering (native plugin install)"
}

# --- Layer 0: Discovery (install FIRST) ---
echo "=== Layer 0: Discovery ==="
install_skillpack vercel-labs/skills --skill find-skills

# --- Layer 1–4: Core 10 engineering stack ---
echo "=== Layer 1–4: Core engineering stack ==="
install_skillpack shadcn/improve
install_skillpack mattpocock/skills --skill '*'
install_skillpack obra/superpowers --skill '*'
install_skillpack addyosmani/agent-skills --skill '*'
install_skillpack vercel-labs/agent-skills --skill '*'
install_skillpack anthropics/skills --skill mcp-builder --skill skill-creator --skill webapp-testing
install_pstack
install_gstack
install_skillpack vercel-labs/agent-browser --skill agent-browser
install_compound_engineering

# --- Layer 5–6: Security ---
echo "=== Layer 5–6: Security ==="
install_skillpack trailofbits/skills --skill '*'

# --- Layer 7: Browser QA ---
echo "=== Layer 7: Browser QA ==="
install_agent_browser_cli

# --- Layer 8: Community / extended discovery ---
echo "=== Layer 8: Community packs ==="
install_skillpack github/awesome-copilot --skill '*'

# --- Optional stack cartridges (install if repo exists) ---
echo "=== Optional stack cartridges ==="
install_skillpack supabase/agent-skills --skill '*' || true
install_skillpack cloudflare/skills --skill '*' || true
install_skillpack aws/agent-toolkit-for-aws --skill '*' || true

# --- microsoft/skills: DO NOT bulk install (context rot) ---
echo "=== microsoft/skills: SKIPPED (selective install only) ==="
echo "    Do NOT run: npx skills add microsoft/skills -s '*'"
echo "    Instead, cherry-pick ~10–15 high-value skills as needed, e.g.:"
echo "      azure-*, github-*, devops-*, security-*, testing-*"
echo "    Use find-skills to search: /find-skills microsoft azure"
echo "    See WORKFLOW.md for curated recommendations."

# --- Summary ---
echo ""
echo "==> Installation complete."
echo "    Skills in ~/.agents/skills: $(ls "$HOME/.agents/skills" | wc -l)"
echo ""
$SKILLS list -g 2>/dev/null | head -50 || true
echo "..."
echo ""
echo "Next steps in Cursor:"
echo "  1. /setup-pstack               (configure models — or use ~/.cursor/rules/pstack-models.mdc)"
echo "  2. /setup-matt-pocock-skills   (issue tracker, labels)"
echo "  3. /add-plugin compound-engineering  (optional native plugin)"
echo "  4. Pick ONE primary workflow: /poteto-mode, superpowers, /office-hours, or /ce-work"
echo "  5. /find-skills <topic>        (discover more skills on demand)"
echo "  6. uv tool install specify-cli (Spec Kit — formal planning mode)"
echo ""
echo "See WORKFLOW.md for the full 10-layer pipeline."
