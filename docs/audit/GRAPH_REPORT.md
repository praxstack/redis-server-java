# Graph Report - redis-server-java  (2026-10-02)

## Corpus Check
- 48 files · ~35,444 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 5 file(s) not represented in the graph (top: (none) 4, .mdc 1)

## Summary
- 432 nodes · 755 edges · 30 communities (16 shown, 14 thin omitted)
- Extraction: 78% EXTRACTED · 22% INFERRED · 0% AMBIGUOUS · INFERRED: 169 edges (avg confidence: 0.89)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `e73b5cb3`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Store
- org.junit.jupiter.api.Test
- RespParserTest
- ServerIntegrationTest.java
- ServerIntegrationTest
- Plan 001: Fix README StoreTest documentation drift
- Operations — redis-server-java
- Super-Pro Developer Skills Stack
- Architecture — redis-server-java
- PraxStack Architecture
- install-pro-skills.sh
- openspec-explore/SKILL.md
- Plan 004: Add GitHub Actions CI for mvn test
- Redis Server in Java
- opsx-explore.md
- install-prax-personal-layer.sh
- PraxStack Skills Stack — pointer
- cloud-agent-install.sh
- com.praxstack:redis-server-java

## God Nodes (most connected - your core abstractions)
1. `Store` - 43 edges
2. `CommandDispatcherTest` - 19 edges
3. `CommandDispatcher` - 17 edges
4. `StoreTest` - 17 edges
5. `Step 1 — Create `StoreTest.java`` - 15 edges
6. `ServerIntegrationTest` - 14 edges
7. `Super-Pro Developer Skills Stack` - 14 edges
8. `Server` - 13 edges
9. `PraxStack Architecture` - 13 edges
10. `Operations — redis-server-java` - 13 edges

## Surprising Connections (you probably didn't know these)
- `Shared dispatcher` --references--> `Store`  [INFERRED]
  docs/enterprise/CONCEPTS/client-handler.md → src/main/java/com/praxstack/redis/Store.java
- `Current state` --references--> `StoreTest`  [INFERRED]
  plans/001-fix-readme-storetest-drift.md → src/test/java/com/praxstack/redis/StoreTest.java
- `Done criteria` --references--> `StoreTest`  [INFERRED]
  plans/001-fix-readme-storetest-drift.md → src/test/java/com/praxstack/redis/StoreTest.java
- `Request path` --references--> `Store`  [INFERRED]
  docs/enterprise/ARCHITECTURE.md → src/main/java/com/praxstack/redis/Store.java
- `Out of scope` --references--> `Store`  [INFERRED]
  plans/002-add-store-unit-tests.md → src/main/java/com/praxstack/redis/Store.java

## Import Cycles
- None detected.

## Communities (30 total, 14 thin omitted)

### Community 0 - "Store"
Cohesion: 0.09
Nodes (29): Handler examples, Limitations (prototype scope), Operations, Active expiry (background), INCR preserves TTL, Lazy expiry (read path), SET with TTL, Stored value (+21 more)

### Community 1 - "org.junit.jupiter.api.Test"
Cohesion: 0.11
Nodes (3): RespEncoder, CommandDispatcherTest, RespEncoderTest

### Community 2 - "RespParserTest"
Cohesion: 0.08
Nodes (19): Further reading, Grammar (client → server), Implementation, RESP2 Protocol, Security: bulk size limit, Commands you will need, Current state, Done criteria (+11 more)

### Community 4 - "ServerIntegrationTest"
Cohesion: 0.08
Nodes (21): Client Handler (Connection Lifecycle), Failure modes, Lifecycle, Not in scope, Pipelining, Resource management, Shared dispatcher, Atomic INCR (+13 more)

### Community 5 - "Plan 001: Fix README StoreTest documentation drift"
Cohesion: 0.06
Nodes (28): Commands you will need, Current state, Done criteria, Out of scope, Plan 001: Fix README StoreTest documentation drift, Status, Step 1 — Update engineering decisions table, Step 3 — Verify (+20 more)

### Community 6 - "Operations — redis-server-java"
Cohesion: 0.08
Nodes (17): Shutdown, Benchmark (optional), Build, CI, Cloud Agent install, Configuration knobs (code-level), Git branch hygiene, Graceful shutdown (+9 more)

### Community 7 - "Super-Pro Developer Skills Stack"
Cohesion: 0.07
Nodes (29): 10-layer pipeline, 2026 selective additions, agent-browser CLI, Before shipping, Browser QA, Bug fix, Codebase health, Core 10 (+21 more)

### Community 8 - "Architecture — redis-server-java"
Cohesion: 0.09
Nodes (14): Architecture — redis-server-java, Concurrency model, Request path, Security hardening, Subsystems, System context, Test coverage map, Case insensitivity (+6 more)

### Community 9 - "PraxStack Architecture"
Cohesion: 0.10
Nodes (18): 10-layer pipeline, 2026 selective additions (high-signal only), Cloud Agent VM setup, Conflict warnings, Core 10 + 2026 additions, Core 10 (install always), Daily workflows, Explicitly NOT bulk-installed (+10 more)

### Community 10 - "install-pro-skills.sh"
Cohesion: 0.33
Nodes (11): install_agent_browser_cli(), install_compound_engineering(), install_gstack(), install_mcp_context7(), install_prax_personal_layer(), install_project_tools(), install_pstack(), install_pstack_models() (+3 more)

### Community 11 - "openspec-explore/SKILL.md"
Cohesion: 0.18
Nodes (10): Check for context, Ending Discovery, Guardrails, Handling Different Entry Points, OpenSpec Awareness, The Stance, What You Don't Have To Do, What You Might Do (+2 more)

### Community 12 - "Plan 004: Add GitHub Actions CI for mvn test"
Cohesion: 0.18
Nodes (11): Commands you will need, Current state, Out of scope, Plan 004: Add GitHub Actions CI for mvn test, Status, Step 1 — Create workflow file, Step 2 — Verify locally, Step 3 — Optional README badge (+3 more)

### Community 13 - "Redis Server in Java"
Cohesion: 0.18
Nodes (10): Architecture, Benchmarks, Build & Run, Commands Supported, Key Engineering Decisions, License, Project Structure, Redis Server in Java (+2 more)

### Community 14 - "opsx-explore.md"
Cohesion: 0.20
Nodes (9): Check for context, Ending Discovery, Guardrails, OpenSpec Awareness, The Stance, What You Don't Have To Do, What You Might Do, When a change exists (+1 more)

### Community 15 - "install-prax-personal-layer.sh"
Cohesion: 0.58
Nodes (8): clone_repo(), install_extra_skills(), install_new_skills(), install_rules(), install_skill_dir(), log(), main(), install-prax-personal-layer.sh script

### Community 17 - "PraxStack Skills Stack — pointer"
Cohesion: 0.33
Nodes (5): Cloud Agent environment, How this repo uses the stack, PraxStack Skills Stack — pointer, Quick links, Recommended agent workflow

## Knowledge Gaps
- **140 isolated node(s):** `com.praxstack:redis-server-java`, `cloud-agent-install.sh script`, `The Stance`, `What You Might Do`, `Check for context` (+135 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 181 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Store` connect `Store` to `org.junit.jupiter.api.Test`, `ServerIntegrationTest`, `Operations — redis-server-java`, `Architecture — redis-server-java`, `CommandDispatcher.java`?**
  _High betweenness centrality (0.123) - this node is a cross-community bridge._
- **Why does `Server` connect `Operations — redis-server-java` to `Store`, `Architecture — redis-server-java`, `ServerIntegrationTest.java`, `ServerIntegrationTest`?**
  _High betweenness centrality (0.082) - this node is a cross-community bridge._
- **Why does `CommandDispatcher` connect `Store` to `org.junit.jupiter.api.Test`, `ServerIntegrationTest.java`, `Operations — redis-server-java`, `Architecture — redis-server-java`, `CommandDispatcher.java`?**
  _High betweenness centrality (0.044) - this node is a cross-community bridge._
- **Are the 28 inferred relationships involving `Store` (e.g. with `Request path` and `Shared dispatcher`) actually correct?**
  _`Store` has 28 INFERRED edges - model-reasoned connections that need verification._
- **Are the 4 inferred relationships involving `CommandDispatcherTest` (e.g. with `Extension point` and `Step 1 — Create `StoreTest.java``) actually correct?**
  _`CommandDispatcherTest` has 4 INFERRED edges - model-reasoned connections that need verification._
- **Are the 6 inferred relationships involving `StoreTest` (e.g. with `Current state` and `Done criteria`) actually correct?**
  _`StoreTest` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `com.praxstack:redis-server-java`, `cloud-agent-install.sh script`, `The Stance` to the rest of the system?**
  _140 weakly-connected nodes found - possible documentation gaps or missing edges._