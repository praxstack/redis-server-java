# PraxStack Architecture

PraxStack is Prax's layered agent skills setup for Cursor Cloud Agents and local desktop replication. It composes best-in-class skill packs into a 10-layer pipeline without dumping 900+ skills into context.

**Core rule:** pick **one primary methodology** per task. Pull other layers in situationally.

## 10-layer pipeline

```
discover → interrogate/spec → plan → implement → review → security → browser QA → ship → learn
```

```text
┌─────────────┐   ┌──────────────────┐   ┌──────────┐   ┌────────────┐
│  discover   │──▶│ interrogate/spec │──▶│   plan   │──▶│ implement  │
│ find-skills │   │ grill, office-hrs│   │ ce-plan  │   │ poteto-mode│
└─────────────┘   └──────────────────┘   └──────────┘   └────────────┘
                                                                │
       ┌────────────────────────────────────────────────────────┘
       ▼
┌────────────┐   ┌──────────┐   ┌────────────┐   ┌──────┐   ┌───────┐
│   review   │──▶│ security │──▶│ browser QA │──▶│ ship │──▶│ learn │
│ interrogate│   │ trailofb │   │ agent-brwsr│   │ /ship│   │ /learn│
└────────────┘   └──────────┘   └────────────┘   └──────┘   └───────┘
```

| Layer | Purpose | Primary tools |
|-------|---------|---------------|
| **0 discover** | Find skills, docs, patterns | `/find-skills`, skills.sh, Context7 MCP |
| **1 interrogate/spec** | Align intent, write specs | `/grill-with-docs`, `/to-spec`, `/interrogate`, `/office-hours` |
| **2 plan** | Executable plans before code | superpowers `writing-plans`, `/ce-plan`, OpenSpec, `/improve plan` |
| **3 implement** | Build with discipline | `/poteto-mode`, `/implement`, `/tdd`, `/ce-work` |
| **4 review** | Multi-model critique | `/interrogate`, `/code-review`, `/review`, `/ce-code-review` |
| **5 security** | Threat model, audit, fuzz | trailofbits (`semgrep`, `differential-review`, `sharp-edges`) |
| **6 browser QA** | Real UI verification | `agent-browser`, `/ce-test-browser`, `/qa` |
| **7 ship** | Release, deploy, PR | `/ship`, `/ce-commit-push-pr`, `deploy-to-vercel` |
| **8 learn** | Capture knowledge, compound | `/ce-compound`, `/learn`, `knowledge-capture` |
| **9 research** | Live web + deep research | `last30days`, `deep-research`, Exa MCP |
| **10 UI quality** | Polish and design review | `hallmark`, `frontend-design`, shadcn `improve` |

## Core 10 + 2026 additions

### Core 10 (install always)

| # | System | Source | Slash command | Role |
|---|--------|--------|---------------|------|
| 1 | **find-skills** | vercel-labs/skills | `/find-skills` | Skill discovery on demand |
| 2 | **pstack** | cursor/plugins/pstack | `/poteto-mode`, `/how`, `/why` | Rigorous engineering mode |
| 3 | **superpowers** | obra/superpowers | (auto-triggers) | Full SDLC with TDD, plans, subagents |
| 4 | **mattpocock** | mattpocock/skills | `/grill-me`, `/implement` | Alignment, TDD, architecture |
| 5 | **gstack** | garrytan/gstack | `/office-hours`, `/review`, `/qa`, `/ship` | Virtual eng team |
| 6 | **improve** | shadcn/improve | `/improve` | Read-only audit → executable plans |
| 7 | **trailofbits** | trailofbits/skills | (skill names) | Security review, fuzzing, semgrep |
| 8 | **agent-browser** | vercel-labs/agent-browser | (CLI + skill) | Headless browser automation |
| 9 | **vercel agent-skills** | vercel-labs/agent-skills | (skill names) | React/Next/Vercel best practices |
| 10 | **compound-engineering** | EveryInc/compound-engineering-plugin | `/ce-work`, `/ce-compound` | Compound loops |

### 2026 selective additions (high-signal only)

| Category | Source | Key skills | When to use |
|----------|--------|------------|-------------|
| **Research** | mvanhorn/last30days-skill | `last30days` | Recent news, trends, "what changed" |
| **Research** | 24601/agent-deep-research | `deep-research` | Multi-source deep dives |
| **UI quality** | nutlope/hallmark | `hallmark` | UI polish and design review |
| **Anthropic subset** | anthropics/skills | `mcp-builder`, `skill-creator`, `webapp-testing`, `frontend-design` | MCP building, testing, design |
| **GitHub toolbox** | github/awesome-copilot | many | Community patterns on demand |
| **Supabase** | supabase/agent-skills | `supabase`, postgres best practices | Supabase projects |
| **Cloudflare** | cloudflare/skills | `workers-best-practices`, `wrangler` | Workers/edge projects |
| **AWS** | aws/agent-toolkit-for-aws | `aws-cdk`, `aws-lambda`, Bedrock | AWS projects |
| **Remotion** | remotion-dev/skills | `remotion-*` | Video/codegen projects |
| **NVIDIA** | nvidia/skills | GPU/CUDA skills | ML/GPU workloads |
| **MCP** | @upstash/context7-mcp | Context7 | Live library docs lookup |
| **Spec** | @fission-ai/openspec | `/opsx-propose` | Formal spec-driven changes |
| **Graph** | graphifyy | `/graphify` | Codebase knowledge graph |

### Prax personal layer (skills-and-personas)

Source: [praxstack/skills-and-personas](https://github.com/praxstack/skills-and-personas). Installed by `install-prax-personal-layer.sh` (called from `install-pro-skills.sh`). Clones to `dev-skills-setup/skills-and-personas/` (gitignored, like gstack).

| Category | Skills | Slash command | When to use |
|----------|--------|---------------|-------------|
| **Reasoning depth** | kingmode, super-mode-core, ultrathink-frontend | `/kingmode` | Architecture, scalability, security, production decisions |
| **Backend (Java)** | backend-pe-java, backend-pe, backend-architecture-standards | `/backend-pe-java` | This repo — JVM concurrency, RESP server design |
| **Team roles** | constellation-team, principal-engineer, product-manager, qa-security-engineer, devops-sre-engineer, frontend-uiux-designer | `/constellation-team` | Cross-functional planning with role labels |
| **Teaching** | teach-pro-max, techtutor, gabriel-petersson-topdown-mentor, lecture-alchemist | `/teach-pro-max` | Adaptive lessons, DSA mentoring |
| **Documents** | blueprint-creator, spec-creator, transcript-pipeline | `/spec-creator` | Specs, blueprints, transcript pipelines |
| **Personal** | chronicle, idea-capturer, concept-cartographer | `/chronicle` | Journal intelligence, idea capture |
| **Ops loops** | superimprove, coding-agent-leadership-principles, cross-agent-handoff | `/superimprove` | Bounded audit-fix loops, agent handoffs |
| **Orchestrators** | apex-autonomous-mode, autonomous-orchestrion, orchestrion-universal-agent-router | `/orchestrion-universal-agent-router` | Multi-agent routing (advanced) |

**41 canonical skills** in `new-skills/` plus 4 extra portable skills (`teach-pro-max`, `superimprove`, `coding-agent-leadership-principles`, `cross-agent-handoff`). Personas in `personas/` and `md-personas/` are source material — invoke via the distilled skills above, not by copying persona files.

**Prompt workflows (paste, not slash):** `dev-skills-setup/skills-and-personas/prompts/high-end-operator/` — lifecycle paste prompts that invoke gstack/superpowers (Think → Plan → Build → Review → Test → Ship → Reflect). See `CATALOG.md`.

**Optional rules:** `.clinerules/` from the repo installs as `~/.cursor/rules/prax-*.mdc` (`alwaysApply: false`). Enable per-project when needed.

**Conflict policy — do NOT duplicate:**

| Already installed (PraxStack core) | skills-and-personas equivalent | Action |
|-----------------------------------|-------------------------------|--------|
| gstack (`/office-hours`, `/ship`, `/qa`) | high-end-operator paste prompts | Use gstack slash commands; prompts reference them |
| superpowers (auto-triggers) | autonomous-orchestrion | Pick one orchestrator per task |
| pstack (`/poteto-mode`) | kingmode, super-mode-core | Complementary: kingmode for depth, pstack for daily rigor |
| `/goal` (Cursor native) | — | Keep using Cursor's `/goal` for durable objectives |

**Install only this layer:**

```bash
./dev-skills-setup/install-prax-personal-layer.sh
# or individual skill:
npx skills add praxstack/skills-and-personas --skill backend-pe-java
```

### Explicitly NOT bulk-installed

| Source | Why skipped |
|--------|-------------|
| **microsoft/skills** (`-s '*'`) | 100+ skills, context rot. Cherry-pick ~10–15 via `/find-skills` |
| Full anthropics/skills dump | Only 4 high-value skills installed |
| Full nvidia/remotion unless needed | Cartridge install; skip if stack unused |

## Install locations

| Scope | Path | Purpose |
|-------|------|---------|
| **Global skills** | `~/.agents/skills/` | skills CLI canonical store |
| **Global Cursor** | `~/.cursor/skills/` | Slash command discovery |
| **Global rules** | `~/.cursor/rules/pstack-models.mdc` | pstack per-role model overrides |
| **Global MCP** | `~/.cursor/mcp.json` | Context7 and other MCP servers |
| **Project-local** | `.cursor/skills/`, `.cursor/rules/`, `openspec/` | Team-shared, repo-specific |
| **On-demand** | `/find-skills <topic>` | Install at runtime, not upfront |

### Global vs project-local vs on-demand

- **Global (`~/.cursor/`, `~/.agents/`):** Core 10, gstack, pstack, security packs. Install once per VM/desktop via `install-pro-skills.sh`.
- **Project-local (`.cursor/` in repo):** OpenSpec commands, graphify rule, team-specific skills. Commit to share with teammates.
- **On-demand (`/find-skills`):** Stack cartridges, microsoft skills, niche domains. Install when a task needs them.

## gstack flat slash commands

gstack's `./setup --host cursor` installs `gstack-*` prefixed directories. Cursor maps folder names to slash commands, so the installer creates **flat symlinks**:

| Prefixed (internal) | Flat (user-facing) |
|---------------------|-------------------|
| `gstack-plan-ceo-review` | `/plan-ceo-review` |
| `gstack-office-hours` | `/office-hours` |
| `gstack-review` | `/review` |
| `gstack-qa` | `/qa` |
| `gstack-ship` | `/ship` |

The installer runs `link_gstack_flat_cursor_skills` after gstack setup and removes duplicate `gstack-*` symlinks from `~/.cursor/skills/`.

## pstack model configuration

File: `~/.cursor/rules/pstack-models.mdc` (`alwaysApply: true`)

Maps each pstack role to a VM-available model slug. Roles fall back to skill defaults when a line is deleted. Aliases `inherit-parent` and `auto` mean "use parent chat model."

Cloud Agent VM models (Aug 2026):

```
inherit, claude-fable-5-thinking-high, claude-fable-5-thinking-xhigh,
claude-opus-5-thinking-high, claude-opus-5-thinking-high-fast,
claude-sonnet-5-thinking-high, claude-sonnet-5-thinking-xhigh,
composer-2.5, composer-2.5-fast, cursor-grok-4.5-high, cursor-grok-4.5-high-fast,
cursor-grok-4.6-high-fast, gemini-3.7-flash-high, gpt-5.6-luna-high,
gpt-5.6-sol-high, gpt-5.6-sol-high-fast, gpt-5.6-sol-xhigh, gpt-5.6-sol-xhigh-fast
```

Re-run `/setup-pstack` or edit the file when models change.

## Conflict warnings

**Do NOT run all methodologies on the same task.** Each has auto-triggers, subagent fan-out, and review loops that fight each other.

| Primary | Best for | Conflicts with |
|---------|----------|----------------|
| **pstack** (`/poteto-mode`) | Daily rigorous engineering | superpowers auto-triggers, gstack review loops |
| **superpowers** | Structured SDLC with TDD | pstack swarm/arena, gstack CEO reviews |
| **gstack** (`/office-hours`) | Product direction + ship | pstack `/how`/`/why`, CE compound loops |
| **compound-engineering** (`/ce-work`) | Compound loops, PR babysit | gstack `/ship`, superpowers plans |
| **improve** | Audit → plan handoff | Running as primary alongside any of the above |

**Safe composition pattern:**

1. Pick one primary (e.g. pstack for implementation)
2. Pull layers by phase: `/office-hours` (spec) → `/poteto-mode` (build) → trailofbits (security) → `/qa` (verify) → `/ship` (release)
3. Never stack `/poteto-mode` + superpowers TDD + `/ce-work` on the same file edit

## Native plugin commands

For richer integration on desktop Cursor:

```
/add-plugin pstack
/add-plugin superpowers
/add-plugin compound-engineering
```

Plugins complement but do not replace the skills CLI install. The installer copies pstack skills from `cursor/plugins` and documents native plugin install as optional.

## Local desktop replication

```bash
# 1. Clone this repo (or copy dev-skills-setup/)
git clone https://github.com/praxstack/redis-server-java.git
cd redis-server-java/dev-skills-setup

# 2. Run full installer
./install-pro-skills.sh

# 3. Configure pstack models for your machine
# In Cursor: /setup-pstack
# Or edit ~/.cursor/rules/pstack-models.mdc manually

# 4. Optional native plugins
# In Cursor: /add-plugin pstack
# In Cursor: /add-plugin superpowers
# In Cursor: /add-plugin compound-engineering

# 5. Project tools (from repo root)
cd ..
npx -y @fission-ai/openspec init    # spec-driven workflow
uv tool install graphifyy && graphify cursor install --project .

# 6. Verify
ls ~/.cursor/skills/plan-ceo-review/SKILL.md   # gstack flat alias
ls ~/.cursor/skills/poteto-mode/SKILL.md       # pstack
ls ~/.agents/skills | wc -l                    # skill count
```

## Cloud Agent VM setup

This repo's `.cursor/environment.json` boots Cloud Agents with bun, node, and the installer. After VM boot:

```bash
./dev-skills-setup/install-pro-skills.sh
```

Pre-written `~/.cursor/rules/pstack-models.mdc` targets Cloud Agent model slugs.

## Daily workflows

See [WORKFLOW.md](./WORKFLOW.md) for task-specific recipes.

| Task | Workflow |
|------|----------|
| New feature | `/grill-with-docs` → `/to-spec` → `/poteto-mode` |
| Bug fix | `/poteto-mode` repro → fix → verify |
| Codebase health | `/improve quick` → `/improve plan` → `/improve execute` |
| Before shipping | `differential-review` → `/interrogate` → `/qa` → `/ship` |
| Product direction | `/office-hours` → `/plan-ceo-review` |
| Research | `/last30days` or `deep-research` → implement |
| Spec-driven | `/opsx-propose` → `/opsx-apply` |

## Update all skills

```bash
npx skills update -g -y
cd dev-skills-setup/gstack && ./setup --host cursor --no-prefix
# Re-link flat aliases:
./install-pro-skills.sh   # idempotent
```

## Files in this directory

| File | Purpose |
|------|---------|
| `install-pro-skills.sh` | Full layered installer (global + project tools) |
| `install-prax-personal-layer.sh` | Prax personal skills from skills-and-personas |
| `PRAXSTACK.md` | This architecture doc |
| `WORKFLOW.md` | Daily workflow recipes |
| `gstack/` | Vendored gstack clone (installer manages) |
| `skills-and-personas/` | Vendored Prax personal skills clone (installer manages) |
| `cursor-plugins/` | Vendored cursor/plugins for pstack |
