#!/usr/bin/env bash
# PraxStack / Super-Pro developer skills installer for Cursor Cloud Agents
# Layered architecture: discover → interrogate/spec → plan → implement → review →
#   security → browser QA → ship → learn
#
# Usage: ./install-pro-skills.sh [--project-dir /path/to/repo]
#
# WARNING: Do NOT activate every methodology on every task. Pick ONE primary workflow
# (pstack, superpowers, gstack, or compound-engineering) and pull in other layers
# situationally. Running all auto-triggers simultaneously causes context rot and noise.
set -euo pipefail

SKILLS="npx skills@latest"
AGENT_BROWSER_PREFIX="${HOME}/.local"
PROJECT_DIR="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)}"

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
  local script_dir plugins_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  plugins_dir="${script_dir}/cursor-plugins"
  if [ ! -d "$plugins_dir/pstack/skills" ]; then
    mkdir -p "$plugins_dir"
    git clone --single-branch --depth 1 https://github.com/cursor/plugins.git "$plugins_dir"
  fi
  for skill in "$plugins_dir/pstack/skills"/*/; do
    local name
    name=$(basename "$skill")
    mkdir -p "$HOME/.cursor/skills/$name" "$HOME/.agents/skills/$name"
    cp -r "$skill"/* "$HOME/.cursor/skills/$name/"
    cp -r "$skill"/* "$HOME/.agents/skills/$name/"
  done
  echo "    Also run in Cursor: /add-plugin pstack (native plugin install)"
}

link_gstack_flat_cursor_skills() {
  # gstack --host cursor installs gstack-* prefixed dirs; Cursor slash commands
  # use the folder name under ~/.cursor/skills/, so create flat aliases (/qa not /gstack-qa).
  local cursor_skills="${HOME}/.cursor/skills"
  local gstack_skills_dir="$1/.cursor/skills"
  local created=0

  for skill_dir in "$gstack_skills_dir"/gstack-*/; do
    [ -d "$skill_dir" ] || continue
    local base flat target
    base=$(basename "$skill_dir")
    flat="${base#gstack-}"
    [ -z "$flat" ] || [ "$flat" = "$base" ] && continue
    target="$cursor_skills/$flat"
    if [ -e "$target" ] && [ ! -L "$target" ]; then
      echo "    SKIP flat alias (exists): $flat"
      continue
    fi
    ln -sfn "$skill_dir" "$target"
    created=$((created + 1))
  done

  # Drop prefixed symlinks so Cursor does not show duplicates (/qa and /gstack-qa).
  for item in "$cursor_skills"/gstack-*/; do
    [ -L "$item" ] || continue
    rm -f "$item"
  done

  find "$gstack_skills_dir" -name 'SKILL.md' -exec chmod 644 {} \; 2>/dev/null || true
  echo "    Linked $created flat gstack skills in ~/.cursor/skills"
}

install_gstack() {
  echo "==> Installing gstack (Garry Tan's engineering team)..."
  if ! command -v bun >/dev/null 2>&1; then
    curl -fsSL https://bun.sh/install | bash
  fi
  export PATH="${HOME}/.bun/bin:${PATH}"
  local script_dir gstack_dir
  script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  gstack_dir="${script_dir}/gstack"
  if [ ! -d "$gstack_dir/.git" ]; then
    git clone --single-branch --depth 1 https://github.com/garrytan/gstack.git "$gstack_dir"
  fi
  # Cursor caveat: gstack issue #2361 — ./setup --host cursor is required (not claude/codex)
  (cd "$gstack_dir" && ./setup --host cursor --no-prefix)
  link_gstack_flat_cursor_skills "$gstack_dir"
}

install_agent_browser_cli() {
  echo "==> Installing agent-browser CLI..."
  if command -v agent-browser >/dev/null 2>&1; then
    echo "    agent-browser already on PATH"
    agent-browser install || true
    return 0
  fi
  if npm install -g agent-browser --prefix "$AGENT_BROWSER_PREFIX"; then
    :
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
    echo "    Also run in Cursor: /add-plugin compound-engineering (native plugin install)"
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

install_mcp_context7() {
  echo "==> Configuring Context7 MCP..."
  local mcp_file="${HOME}/.cursor/mcp.json"
  mkdir -p "${HOME}/.cursor"
  if [ -f "$mcp_file" ]; then
    echo "    SKIP: $mcp_file already exists"
    return 0
  fi
  cat > "$mcp_file" <<'EOF'
{
  "mcpServers": {
    "context7": {
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp"]
    }
  }
}
EOF
  echo "    Wrote $mcp_file"
}

install_pstack_models() {
  echo "==> Writing pstack model configuration..."
  local rules_dir="${HOME}/.cursor/rules"
  mkdir -p "$rules_dir"
  # Cloud Agent VM models (Aug 2026). Re-run /setup-pstack to customize per machine.
  cat > "${rules_dir}/pstack-models.mdc" <<'EOF'
---
description: pstack per-role model choices (overrides skill defaults)
alwaysApply: true
---
# pstack model configuration. One line per role. Delete a line to fall back to the skill default.
# `inherit-parent` or `auto` as a value: the role runs on the parent chat model (omit Task `model`).
# Mapped to Cloud Agent VM available models (Aug 2026).

feature, refactoring: cursor-grok-4.6-high-fast
bug-fix: gpt-5.6-sol-xhigh
perf-issue: gpt-5.6-sol-xhigh
hillclimb: gpt-5.6-sol-xhigh
judgment and prose: claude-fable-5-thinking-xhigh
hardest tasks: claude-fable-5-thinking-xhigh
how explorer: cursor-grok-4.6-high-fast
how explainer: claude-fable-5-thinking-xhigh
how critics: claude-fable-5-thinking-xhigh, gpt-5.6-sol-xhigh, cursor-grok-4.6-high-fast, claude-opus-5-thinking-high
why investigators: cursor-grok-4.6-high-fast
why synthesizer: claude-fable-5-thinking-xhigh
reflect tooling: gpt-5.6-sol-xhigh
reflect judgment, divergent, synthesizer: claude-fable-5-thinking-xhigh
arena runners: claude-fable-5-thinking-xhigh, gpt-5.6-sol-xhigh, cursor-grok-4.6-high-fast, claude-opus-5-thinking-high
arena cross-judge pool: claude-fable-5-thinking-xhigh, gpt-5.6-sol-xhigh, cursor-grok-4.6-high-fast, claude-opus-5-thinking-high
swarm workers: cursor-grok-4.6-high-fast
architect runners: claude-fable-5-thinking-xhigh, gpt-5.6-sol-xhigh, cursor-grok-4.6-high-fast, claude-opus-5-thinking-high
interrogate reviewers: claude-fable-5-thinking-xhigh, gpt-5.6-sol-xhigh, cursor-grok-4.6-high-fast, claude-opus-5-thinking-high
EOF
  echo "    Wrote ${rules_dir}/pstack-models.mdc"
}

install_project_tools() {
  echo "==> Installing project-level tools in $PROJECT_DIR..."
  if command -v node >/dev/null 2>&1 || command -v npm >/dev/null 2>&1; then
    if (cd "$PROJECT_DIR" && npx -y @fission-ai/openspec init 2>/dev/null); then
      echo "    OK: OpenSpec initialized in $PROJECT_DIR"
    else
      echo "    SKIP: OpenSpec init failed or unavailable"
    fi
  else
    echo "    SKIP: node/npm not available for OpenSpec"
  fi

  export PATH="${HOME}/.local/bin:${PATH}"
  if command -v uv >/dev/null 2>&1 || curl -LsSf https://astral.sh/uv/install.sh | sh; then
    export PATH="${HOME}/.local/bin:${PATH}"
    if uv tool install graphifyy 2>/dev/null; then
      if graphify cursor install --project "$PROJECT_DIR" 2>/dev/null; then
        echo "    OK: graphify installed for $PROJECT_DIR"
      else
        echo "    WARN: graphify CLI installed but project setup failed"
      fi
    else
      echo "    SKIP: graphify install failed"
    fi
  else
    echo "    SKIP: uv not available for graphify"
  fi
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
install_skillpack anthropics/skills --skill mcp-builder --skill skill-creator --skill webapp-testing --skill frontend-design
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

# --- Layer 9: Research (2026 additions) ---
echo "=== Layer 9: Research layer ==="
install_skillpack mvanhorn/last30days-skill || true
install_skillpack 24601/agent-deep-research || true

# --- Layer 10: UI quality (2026 additions) ---
echo "=== Layer 10: UI quality ==="
install_skillpack nutlope/hallmark || install_skillpack Nutlope/hallmark || true

# --- Optional stack cartridges ---
echo "=== Optional stack cartridges ==="
install_skillpack supabase/agent-skills --skill '*' || true
install_skillpack cloudflare/skills --skill '*' || true
install_skillpack aws/agent-toolkit-for-aws --skill '*' || true
install_skillpack remotion-dev/skills --skill '*' || true
install_skillpack nvidia/skills --skill '*' || true

# --- MCP + pstack model config ---
echo "=== MCP and pstack configuration ==="
install_mcp_context7
install_pstack_models

# --- Project-level tools (OpenSpec, graphify) ---
install_project_tools

# --- microsoft/skills: DO NOT bulk install (context rot) ---
echo "=== microsoft/skills: SKIPPED (selective install only) ==="
echo "    Do NOT run: npx skills add microsoft/skills -s '*'"
echo "    Instead, cherry-pick ~10–15 high-value skills as needed, e.g.:"
echo "      azure-*, github-*, devops-*, security-*, testing-*"
echo "    Use find-skills to search: /find-skills microsoft azure"
echo "    See PRAXSTACK.md and WORKFLOW.md for curated recommendations."

# --- Summary ---
echo ""
echo "==> Installation complete."
echo "    Skills in ~/.agents/skills: $(ls "$HOME/.agents/skills" | wc -l)"
echo ""
$SKILLS list -g 2>/dev/null | head -50 || true
echo "..."
echo ""
echo "Next steps in Cursor:"
echo "  1. /setup-pstack               (reconfigure models if VM differs)"
echo "  2. /setup-matt-pocock-skills   (issue tracker, labels)"
echo "  3. /add-plugin pstack          (optional native plugin)"
echo "  4. /add-plugin superpowers     (optional native plugin)"
echo "  5. /add-plugin compound-engineering  (optional native plugin)"
echo "  6. Pick ONE primary workflow: /poteto-mode, superpowers, /office-hours, or /ce-work"
echo "  7. /find-skills <topic>        (discover more skills on demand)"
echo "  8. uv tool install specify-cli (Spec Kit — formal planning mode)"
echo "  9. /graphify .                 (build knowledge graph if graphify installed)"
echo ""
echo "See PRAXSTACK.md for architecture and WORKFLOW.md for daily workflows."
