# Super-Pro Developer Skills Stack

A layered, composable agent skills setup for serious engineering work in Cursor Cloud Agents.

**Philosophy:** compose, don't overload. Pick **one primary methodology** per task and pull in other layers situationally.

## 10-layer pipeline

```
discover → interrogate/spec → plan → implement → review → security → browser QA → ship → learn
```

```mermaid
flowchart LR
  D[discover] --> I[interrogate/spec]
  I --> P[plan]
  P --> M[implement]
  M --> R[review]
  R --> S[security]
  S --> B[browser QA]
  B --> SH[ship]
  SH --> L[learn]
```

| Layer | Purpose | Primary tools |
|-------|---------|---------------|
| **discover** | Find skills, docs, patterns | `/find-skills`, skills.sh |
| **interrogate/spec** | Align on intent, write specs | `/grill-with-docs`, `/to-spec`, `/interrogate`, `/office-hours` |
| **plan** | Executable plans before code | superpowers `writing-plans`, `/ce-plan`, Spec Kit, `/improve plan` |
| **implement** | Build with discipline | `/poteto-mode`, `/implement`, `/tdd`, `/ce-work` |
| **review** | Multi-model critique | `/interrogate`, `/code-review`, `/gstack-review`, `/ce-code-review` |
| **security** | Threat model, audit, fuzz | trailofbits (`semgrep`, `differential-review`, `sharp-edges`) |
| **browser QA** | Real UI verification | `agent-browser`, `/ce-test-browser`, gstack `/qa` |
| **ship** | Release, deploy, PR | `/ship`, `/ce-commit-push-pr`, `deploy-to-vercel` |
| **learn** | Capture knowledge, compound | `/ce-compound`, gstack `/learn`, `knowledge-capture` |

## Core 10

The default engineering cartridge — install these, configure once, use daily:

| # | System | Source | Role |
|---|--------|--------|------|
| 1 | **find-skills** | vercel-labs/skills | Skill discovery on demand |
| 2 | **pstack** | cursor/plugins/pstack | Rigorous engineering mode (`/poteto-mode`, `/how`, `/why`) |
| 3 | **superpowers** | obra/superpowers | Full SDLC with auto-triggers (TDD, plans, subagents) |
| 4 | **mattpocock/skills** | mattpocock/skills | Alignment, TDD, architecture (`/grill-me`, `/implement`) |
| 5 | **gstack** | garrytan/gstack | Virtual eng team (CEO, QA, security, ship) |
| 6 | **improve** | shadcn/improve | Read-only audit → executable plans |
| 7 | **trailofbits** | trailofbits/skills | Security review, fuzzing, semgrep, differential-review |
| 8 | **agent-browser** | vercel-labs/agent-browser | Headless browser automation for QA |
| 9 | **vercel-labs/agent-skills** | vercel-labs/agent-skills | React/Next/Vercel best practices |
| 10 | **compound-engineering** | EveryInc/compound-engineering-plugin | Compound loops (`/ce-work`, `/ce-compound`) |

Native plugin install (optional, richer integration):

```
/add-plugin compound-engineering
```

## When to use each layer

### Discover
- Unfamiliar domain or missing capability → `/find-skills <topic>`
- Browse [skills.sh](https://skills.sh) for community packs

### Interrogate / Spec
- New feature with ambiguity → `/grill-with-docs` then `/to-spec`
- Strategic product questions → `/office-hours` (gstack CEO)
- Hard requirements → `/interrogate` (pstack multi-model)

### Plan
- Non-trivial work → superpowers `writing-plans` or `/ce-plan`
- **Formal planning mode:** [GitHub Spec Kit](https://github.com/github/spec-kit)
  ```bash
  uv tool install specify-cli
  specify init . --ai cursor
  ```
- Tech debt / audit → `/improve quick` → `/improve plan <finding>`

### Implement
- Daily rigorous work → `/poteto-mode <task>`
- TDD at seams → `/tdd` (mattpocock) or superpowers `test-driven-development`
- Compound loop → `/ce-work`

### Review
- Pre-merge → `/interrogate` + `/code-review`
- Release gate → `/gstack-review` or `/review`
- Compound → `/ce-code-review`

### Security
- Before shipping sensitive code → trailofbits `differential-review`, `semgrep`, `sharp-edges`
- Supply chain → `supply-chain-risk-auditor`
- Crypto / auth → `constant-time-analysis`, `zeroize-audit`

### Browser QA
- UI changes → `agent-browser` CLI + skill, or `/ce-test-browser`
- Full product QA → gstack `/qa`
- gstack browse → `/gstack-browse` (separate from agent-browser)

### Ship
- Release engineer → `/ship` (gstack)
- PR workflow → `/ce-commit-push-pr`
- Vercel deploy → `deploy-to-vercel` skill

### Learn
- Post-ship knowledge → `/ce-compound`, gstack `/learn`
- Team memory → Notion `knowledge-capture` skill

## Stack-specific cartridges

Install only when your project uses that stack (optional in `install-pro-skills.sh`):

| Stack | Source | Key skills |
|-------|--------|------------|
| **Supabase** | supabase/agent-skills | `supabase`, `supabase-postgres-best-practices` |
| **Cloudflare** | cloudflare/skills | `workers-best-practices`, `wrangler`, `durable-objects` |
| **AWS** | aws/agent-toolkit-for-aws | `aws-cdk`, `aws-lambda`, `aws-iam`, `amazon-bedrock` |

## microsoft/skills — selective install only

**Do NOT** bulk install: `npx skills add microsoft/skills -s '*'`

That dumps 100+ skills and causes context rot. Cherry-pick ~10–15 as needed:

```bash
# Search first
/find-skills microsoft azure github

# Example selective installs (adjust to your stack)
npx skills add microsoft/skills -g -y -a cursor -s azure-compute -s github-actions
```

Curated starting set (install individually when relevant):
- Azure diagnostics / compute / RBAC skills
- GitHub Actions / Copilot workflow skills
- Security and compliance skills for your cloud
- Testing and DevOps skills matching your CI

## gstack on Cursor

gstack requires the Cursor host adapter:

```bash
cd /tmp/gstack && ./setup --host cursor
```

**Caveat:** [gstack issue #2361](https://github.com/garrytan/gstack/issues/2361) — some hooks (e.g. AskUserQuestion) may not register on Cursor until resolved. Core skills (`/office-hours`, `/review`, `/qa`, `/ship`) work via `~/.cursor/skills/gstack-*`.

Verified on this VM: `./setup --host cursor` completes successfully.

## agent-browser CLI

```bash
npm install -g agent-browser --prefix ~/.local   # if global npm lacks permissions
export PATH="$HOME/.local/bin:$PATH"
agent-browser install
# If shared-library errors on Linux:
agent-browser install --with-deps
```

## One-time setup per environment

```bash
./dev-skills-setup/install-pro-skills.sh   # full layered install
/setup-pstack                              # or use pre-written pstack-models.mdc
/setup-matt-pocock-skills                  # issue tracker, labels, docs path
uv tool install specify-cli                # Spec Kit formal planning
```

## Recommended daily workflows

### New feature
```
/grill-with-docs → /to-spec → /poteto-mode implement with TDD
```

### Bug fix
```
/poteto-mode repro first, fix, verify
```

### Codebase health
```
/improve quick → /improve plan <id> → /improve execute <id>
```

### Before shipping
```
/differential-review → /interrogate → /qa or agent-browser → /ship
```

### Product direction
```
/office-hours → /plan-ceo-review → /improve next
```

## Install locations

- Global: `~/.agents/skills/` and `~/.cursor/skills/`
- pstack models: `~/.cursor/rules/pstack-models.mdc` (`alwaysApply: true`)
- Repo-local: `.cursor/skills/` (commit for team sharing)

## Update all skills

```bash
npx skills update -g -y
cd ~/.cursor/skills/gstack && ./setup --host cursor   # or /gstack-upgrade
```

## Warning: one primary workflow

| Primary | Best for |
|---------|----------|
| **pstack** | Daily rigorous engineering in Cursor |
| **superpowers** | Structured SDLC with automatic skill triggers |
| **gstack** | Product + ship + QA as virtual team |
| **compound-engineering** | Compound loops, browser test, PR babysit |
| **mattpocock** | Alignment, TDD, architecture discipline |
| **improve** | Audit/plan handoff to cheaper executors |

Do not run all auto-triggers on every task. Choose one column and pull others in by layer.

## More to explore

| Repo | Why |
|------|-----|
| [skills.sh](https://skills.sh) | Open skills directory |
| [github/awesome-copilot](https://github.com/github/awesome-copilot) | Community skills (installed) |
| [CodeRabbit autofix](https://github.com/coderabbitai/autofix) | PR review + autofix |
| [firebase/agent-skills](https://github.com/firebase/agent-skills) | Firebase/Firestore |
