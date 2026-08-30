# PraxStack Skills Stack — pointer

This Redis server repo includes a **PraxStack development skills layer** under `dev-skills-setup/`. Use it to bootstrap Cursor/Cloud Agent environments with team personas, verification gates, and install scripts.

## Quick links

| Resource | Path |
|----------|------|
| PraxStack overview | `dev-skills-setup/PRAXSTACK.md` |
| Pro skills installer | `dev-skills-setup/install-pro-skills.sh` |
| Personal layer installer | `dev-skills-setup/install-prax-personal-layer.sh` |
| Team personas | `dev-skills-setup/skills-and-personas/team-personas/` |
| Constellation team skills | `dev-skills-setup/skills-and-personas/team-personas/constellation-team/` |

## How this repo uses the stack

- **Plans** (`plans/`) — auditable improvement backlog executed by Cloud Agents
- **CI** (`.github/workflows/ci.yml`) — `mvn test` gate on every PR
- **Enterprise docs** (`docs/enterprise/`) — architecture, operations, concepts

## Recommended agent workflow

1. Read `plans/README.md` for open work
2. Run `mvn test` before and after changes
3. Update `docs/enterprise/` when behavior changes
4. Use constellation personas for review roles (backend, QA/security, DevOps)

## Cloud Agent environment

`.cursor/environment.json` and `scripts/cloud-agent-install.sh` configure the Java/Maven toolchain. For environment setup issues, see the Cursor `env-setup` skill.
