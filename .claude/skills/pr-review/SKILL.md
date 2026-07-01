---
name: pr-review
description: Review a GitHub pull request for the support-agent repo against this project's specific conventions (two-layer rule enforcement, Flyway-only schema, pinned test deps, strict frontend lint). Use when the user asks to "pr-review", "review PR #N", "review this pull request", or gives a github.com PR URL. For reviewing local uncommitted changes use /code-review instead.
---

# PR Review (support-agent)

Review a GitHub pull request for the `support-agent` repo and produce a written
report **in chat only**. Do not post comments, approve, merge, or make any other
change to GitHub — this skill is read-only against the remote.

Repo slug: `sumitgupta28/support-agent`. Requires an authenticated `gh` CLI
(`gh auth status`).

## 1. Resolve the PR

The PR is given as a number, a `#N` reference, or a `github.com/.../pull/N` URL in
the invocation. Extract `N` and fetch:

```bash
gh pr view <N> --json number,title,body,author,files,additions,deletions,baseRefName,headRefName
gh pr diff <N>
```

If no PR was given, list open PRs and ask which one:

```bash
gh pr list --state open
```

Read the diff carefully. All findings must cite `file:line` from the diff.

## 2. General correctness pass

Look for real defects: bugs, unhandled edge cases, missing error handling, and
security issues. This is a **security-sensitive support agent that issues
refunds** — scrutinize anything touching authentication of the customer, refund
eligibility, or money-moving paths especially hard.

## 3. Project-specific checklist

Apply each check **only when the PR touches the relevant area**. These encode
invariants from `CLAUDE.md` that a generic reviewer would miss.

- **Two-layer rule enforcement** — if tool ordering or business rules change,
  verify BOTH layers are updated and neither is collapsed into the other:
  - `service/PolicyEngine.java` — the per-session tool-ordering gate
    (`get_customer` before mutations, `lookup_order` before `process_refund`,
    no duplicate refund per session).
  - Tool-internal DB re-validation — e.g. `ProcessRefundTool.execute()`
    re-checks ownership, the 30-day window (`refundEligible`), and
    not-already-refunded against the database. Trust the DB, not the model's
    cached tool results.

- **Schema changes** — must go through a **new Flyway migration** under
  `resources/db/migration/` (`V3__...`, etc.). Hibernate is `ddl-auto=validate`,
  so editing JPA entities alone without a matching migration will fail startup.
  Flag entity changes with no accompanying migration.

- **New MCP tools** — should be a `@Component implements McpTool`, auto-discovered
  by `McpToolRegistry` (no manual registration). Mutating tools must be
  `@Transactional`.

- **Session state** — `service/ContextManager.java` is in-memory
  (`ConcurrentHashMap` keyed by `sessionId`) and **not multi-instance safe**.
  Flag any change that assumes persistence, restart-survival, or horizontal
  scaling of session state.

- **Anthropic client** — changes to `service/ClaudeApiClient.java` must preserve
  the `anthropic-version: 2023-06-01` header and the model/key/max-tokens wiring
  from `application.properties`. Flag introduction of an SDK where the hand-rolled
  OkHttp call is expected.

- **Build constraints** — flag any bump to `junit-jupiter.version` or
  reintroduction of `spring-boot-devtools` in `backend/build.gradle`; these jars
  aren't in the local Gradle cache and downloads are blocked behind the corporate
  Zscaler proxy.


- **Frontend** — lint runs with `--max-warnings 0` (zero tolerance). Components
  should stay presentational with state centralized in `hooks/useAgent.js`; flag
  business/state logic leaking into presentational components.

## 4. Verification guidance

Note (do not run unless the user asks) which gates the author should pass for the
areas the PR touches:

- Backend changes: `cd backend && ./gradlew test` (H2 in-memory; no PostgreSQL or
  API key needed).
- Frontend changes: `cd frontend && npm run lint && npm run build`.

## 5. Report format

Output a structured markdown report in chat. No `gh` write commands.

- **Summary** — PR number, title, author, and a one-line scope description.
- **Blocking issues** — correctness, security, or convention violations that
  should be fixed before merge. Each with `file:line` and why it matters.
- **Suggestions** — non-blocking improvements.
- **Nits** — minor/style.
- **Convention checklist** — list which project-specific checks (section 3) were
  relevant and their pass/flag status.
- Close with an explicit note that **no changes were made to the PR or GitHub**.
