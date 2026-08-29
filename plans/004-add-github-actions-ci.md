# Plan 004: Add GitHub Actions CI for mvn test

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat cfe2170..HEAD -- .github/`

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx
- **Planned at**: commit `cfe2170`, 2026-08-29

## Why this matters

The repository has no `.github/workflows/` CI. Regressions rely on manual `mvn test`. A minimal Java 17 + Maven workflow on push/PR gives a one-command verification gate for all future plans and PRs.

## Current state

- Build: Maven (`pom.xml`), Java 17
- Test command: `mvn test`
- No existing CI configuration
- Cloud Agent install script: `scripts/cloud-agent-install.sh` runs `mvn clean package -DskipTests` (CI should run tests)

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Local verify | `mvn test -q` | exit 0 |
| Validate workflow YAML | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/ci.yml'))"` | exit 0 (if PyYAML available) or manual review |

## Steps

### Step 1 — Create workflow file

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - name: Run tests
        run: mvn -B test
```

### Step 2 — Verify locally

```bash
mvn -B test
```

### Step 3 — Optional README badge

Add to `README.md` near existing badges (only if workflow is pushed and expected to run):

```markdown
[![CI](https://github.com/praxstack/redis-server-java/actions/workflows/ci.yml/badge.svg)](https://github.com/praxstack/redis-server-java/actions/workflows/ci.yml)
```

Skip badge if you cannot confirm Actions is enabled on the repo.

## STOP conditions

- If `.github/workflows/ci.yml` already exists, stop and only fix gaps.
- If `mvn test` fails locally, stop — fix tests first (not in this plan's scope).

## Out of scope

- Deploy/release workflows
- Codecov or coverage gates

## Done criteria

- [ ] `.github/workflows/ci.yml` exists and runs `mvn test` on Java 17
- [ ] Local `mvn -B test` passes
- [ ] Workflow triggers on push to `main` and on PRs
