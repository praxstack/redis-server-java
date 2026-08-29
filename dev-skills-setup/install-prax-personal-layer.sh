#!/usr/bin/env bash
# Prax personal layer — skills-and-personas from praxstack/skills-and-personas
#
# Installs canonical new-skills/ portfolio + selected portable skills from skills/
# into ~/.cursor/skills/ and ~/.agents/skills/. Copies optional Cline rules as
# ~/.cursor/rules/prax-*.mdc. Does NOT duplicate gstack, pstack, or superpowers.
#
# Usage: ./install-prax-personal-layer.sh [--repo-dir /path/to/skills-and-personas]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="${SCRIPT_DIR}/skills-and-personas"
CURSOR_SKILLS="${HOME}/.cursor/skills"
AGENT_SKILLS="${HOME}/.agents/skills"
CURSOR_RULES="${HOME}/.cursor/rules"
TS="$(date +%Y%m%d-%H%M%S)"
BACKUP="${CURSOR_SKILLS}/_prax-backup-${TS}"

# Extra portable skills in skills/ (not duplicated in new-skills/)
EXTRA_SKILLS=(
  teach-pro-max
  superimprove
  coding-agent-leadership-principles
  cross-agent-handoff
)

log() { printf "[prax-personal] %s\n" "$*"; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo-dir) REPO_DIR="$2"; shift 2 ;;
    *) echo "Unknown arg: $1"; exit 1 ;;
  esac
done

clone_repo() {
  if [ -d "$REPO_DIR/.git" ]; then
    log "Updating skills-and-personas..."
    git -C "$REPO_DIR" pull --ff-only origin main 2>/dev/null || \
      git -C "$REPO_DIR" fetch --depth 1 origin main && git -C "$REPO_DIR" reset --hard origin/main
    return 0
  fi
  log "Cloning skills-and-personas (shallow)..."
  git clone --single-branch --depth 1 https://github.com/praxstack/skills-and-personas.git "$REPO_DIR"
}

install_skill_dir() {
  local src="$1"
  local name="$2"
  local dst_cursor="${CURSOR_SKILLS}/${name}"
  local dst_agent="${AGENT_SKILLS}/${name}"

  if [ ! -f "${src}/SKILL.md" ]; then
    log "SKIP (no SKILL.md): $name"
    return 0
  fi

  if [ -d "$dst_cursor" ] && [ ! -L "$dst_cursor" ]; then
    mkdir -p "$BACKUP"
    log "backup: $name"
    mv "$dst_cursor" "${BACKUP}/${name}-cursor"
  fi
  if [ -d "$dst_agent" ] && [ ! -L "$dst_agent" ]; then
    mkdir -p "$BACKUP"
    mv "$dst_agent" "${BACKUP}/${name}-agents" 2>/dev/null || true
  fi

  mkdir -p "$dst_cursor" "$dst_agent"
  cp -R "${src}/." "$dst_cursor/"
  cp -R "${src}/." "$dst_agent/"
  find "$dst_cursor" -name 'SKILL.md' -exec chmod 644 {} \; 2>/dev/null || true
  log "installed: $name"
}

install_new_skills() {
  local src_root="${REPO_DIR}/new-skills"
  local count=0

  if [ ! -d "$src_root" ]; then
    log "ERROR: missing $src_root"
    return 1
  fi

  log "Installing canonical new-skills/ portfolio..."
  for skill_dir in "$src_root"/*/; do
    local name
    name=$(basename "$skill_dir")
    [[ "$name" == _audit ]] && continue
    [[ "$name" == .* ]] && continue
    install_skill_dir "$skill_dir" "$name"
    count=$((count + 1))
  done
  log "new-skills installed: $count"
}

install_extra_skills() {
  local src_root="${REPO_DIR}/skills"
  log "Installing extra portable skills from skills/..."

  for name in "${EXTRA_SKILLS[@]}"; do
    local skill_dir="${src_root}/${name}"
    if [ -d "$skill_dir" ]; then
      install_skill_dir "$skill_dir" "$name"
    else
      log "SKIP (not in repo): $name"
    fi
  done
}

install_rules() {
  local rules_src="${REPO_DIR}/.clinerules"
  if [ ! -d "$rules_src" ]; then
    log "SKIP: no .clinerules in repo"
    return 0
  fi

  mkdir -p "$CURSOR_RULES"
  log "Installing Prax Cline rules as ~/.cursor/rules/prax-*.mdc..."

  for rule in "$rules_src"/*.md; do
    [ -f "$rule" ] || continue
    local base
    base=$(basename "$rule" .md)
    local dest="${CURSOR_RULES}/prax-${base}.mdc"
    {
      echo "---"
      echo "description: Prax personal layer — ${base}"
      echo "alwaysApply: false"
      echo "---"
      echo ""
      cat "$rule"
    } > "$dest"
    log "rule: prax-${base}.mdc"
  done

  local workflow="${rules_src}/workflows/constellation-team.md"
  if [ -f "$workflow" ]; then
  {
    echo "---"
    echo "description: Prax constellation-team workflow"
    echo "alwaysApply: false"
    echo "---"
    echo ""
    cat "$workflow"
  } > "${CURSOR_RULES}/prax-constellation-team-workflow.mdc"
    log "rule: prax-constellation-team-workflow.mdc"
  fi
}

main() {
  clone_repo
  mkdir -p "$CURSOR_SKILLS" "$AGENT_SKILLS"
  install_new_skills
  install_extra_skills
  install_rules

  log ""
  log "=== Prax personal layer complete ==="
  log "skills-and-personas: $REPO_DIR"
  if [ -d "$BACKUP" ]; then
    log "backup dir: $BACKUP"
  fi
  log "Cursor skills: $(ls "$CURSOR_SKILLS" | wc -l) total (includes gstack aliases)"
  log "Agent skills:  $(ls "$AGENT_SKILLS" | wc -l) total"
  log ""
  log "Sample slash commands:"
  log "  /kingmode          — architecture-first reasoning router"
  log "  /backend-pe-java   — Java backend principal engineer"
  log "  /constellation-team — cross-functional team orchestrator"
  log "  /teach-pro-max     — adaptive teaching system"
  log "  /superimprove      — bounded audit-fix-review loop"
  log ""
  log "Prompt workflows (paste, not slash): dev-skills-setup/skills-and-personas/prompts/high-end-operator/"
  log "Conflicts: does NOT install gstack/pstack/superpowers (see PRAXSTACK.md)"
}

main "$@"
