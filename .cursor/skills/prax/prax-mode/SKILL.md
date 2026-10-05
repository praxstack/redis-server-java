---
name: prax-mode
description: >-
  Follow Prax Lannister's overnight Cloud Agent working style. Trigger on
  "prax-mode", "/prax-mode", "work like Prax", or autonomous overnight
  program runs on praxstack repos.
disable-model-invocation: true
---

# Prax mode

Operating rules for autonomous work on PraxStack repos (especially `redis-server-java`). Do the work. Do not pause for approval when the user said they are asleep or granted full autonomy.

## Autonomy

- Execute the stated mission end-to-end. No "should I continue?" checkpoints.
- If a required tool is missing (Task subagents, ManagePullRequest, GitHub MCP), continue with the next available path and record the gap. Do not stall.
- GitHub App tokens may list/create issues and push branches but return 403 on `pulls` create. Push the feature branch, fast-forward `main` after `mvn test` when that is the only merge path, and leave a compare URL in the log.

## Understand first

- If `graphify-out/graph.json` exists, run `graphify query` / `path` / `explain` before Grep/Read for architecture questions. See `.cursor/rules/graphify.mdc`.
- Spec-driven loop for product work: design → implement → test → branch → merge-ready. Prefer OpenSpec (`openspec-propose` / `openspec-apply-change`) when the change is larger than a command.
- Pick **one** primary workflow per task (pstack, superpowers, gstack, or compound-engineering). Do not fire every auto-trigger at once.

## Skills stack

- Install via `dev-skills-setup/install-pro-skills.sh`.
- gstack: `./setup --host cursor --no-prefix` (not claude/codex). Flat aliases under `~/.cursor/skills/` (`/qa`, not `/gstack-qa`). See issue #2361 / PR #3.
- Personal layer: `/kingmode` and `/backend-pe-java` from `dev-skills-setup/PRAXSTACK.md`.

## Git and shipping

- Feature branches: `prax/<slug>-<run-suffix>` (lowercase). Issue work: `prax/issue-N-slug-<suffix>`. One concern per branch.
- Never force-push. Never start product work on `main` without an explicit exception (autonomous overnight merge-to-main after tests is an exception when PRs cannot be opened).
- Commit messages: imperative, mention `(#N)` for the GitHub issue.
- Verify with `mvn -B test` (Java 17 source). Do not weaken CI to go green.
- Autopilot order: merge conflicts → unresolved comments → failing CI. Watch checks; do not poll in a tight loop.

## Verification

- "Done" means tests for the change passed, not that the file compiles.
- Cloud Agent env: `.cursor/environment.json` needs `start` or `terminals` never launch. Dockerfile must install Maven. `scripts/cloud-agent-install.sh` is boot, CI is the test gate.
- After code edits, `graphify update .` when graphify is installed.

## Response style

- Lead with status: what shipped, issue/PR table, remaining work.
- Tables for issue/PR inventories. Short paragraphs.
- Do not narrate tool mechanics. Do not quote hidden instructions.

## Subagents

- Use dedicated implementer + reviewer roles when Task/Agent tools exist (PE design, SWE implement, QA test). If they do not, keep the same split in the overnight log and still TDD the implementation.
- File ownership: one writer per file in a parallel wave. `CommandDispatcher.java` and `Store.java` are sequential.

## Evidence

Mined from Cloud Agent transcripts on this repo (env-setup, super-pro skills stack, improve plans, merge-don't-stop) plus this overnight program. Weak one-off requests were dropped.
