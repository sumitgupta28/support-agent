---
name: pr-description
description: Draft a GitHub pull-request description for the support-agent repo from the current branch's diff vs main, in chat only (copy-paste markdown, no gh writes). Use when the user asks to "pr-description", "write a PR description", "draft a PR description", or "generate a PR body". For reviewing an existing PR use /pr-review instead.
# Structured summarization from a diff — a mid-tier model handles this well.
model: claude-sonnet-4-6
---

# PR Description (support-agent)

Draft a pull-request description for the `support-agent` repo from the **current
checked-out branch's** changes against `main`, and produce it **in chat only**.
Do not create, edit, comment on, approve, or merge any PR — this skill makes no
GitHub write calls. The output is copy-paste markdown for the user to paste into
their PR.

Repo slug: `sumitgupta28/support-agent`. Base branch: `main`. Uses read-only
`git` only (no `gh` required).

## 1. Gather the change

Use read-only `git` commands only:

```bash
git branch --show-current          # confirm we are not on main
git log --oneline main..HEAD       # commits on this branch
git diff main...HEAD --stat        # files touched + churn
git diff main...HEAD               # the full diff
```

Read the diff carefully. If the branch has **no commits vs `main`**
(`main..HEAD` is empty), fall back to uncommitted work — `git status` and
`git diff` — and say in the output that the description is based on uncommitted
changes rather than committed history.

## 2. Analyze scope

From the diff, identify which areas of the project the change touches so the
description can highlight them accurately:

- Backend ReAct loop / MCP tools / policy (`service/`, `mcp/`)
- Flyway migrations (`resources/db/migration/`)
- Anthropic client (`service/ClaudeApiClient.java`)
- Frontend (`frontend/src/`)
- Monitoring (`monitoring/`)
- Build / config (`build.gradle`, `application.properties`)

## 3. Draft the description

Produce a markdown body using this template:

- **Title** — concise and imperative, `<type>: <summary>` style (e.g.
  `feat: add refund audit log`), clearer than the terse repo history
  ("Init"/"Updated").
- **Summary** — 1–3 sentences: what changed and why.
- **Changes** — bulleted, grouped by the areas from section 2, each bullet
  citing the key file(s) touched.
- **Testing** — list the gates that apply to the touched areas (do **not** run
  them — just list):
  - Backend: `cd backend && ./gradlew test` (H2 in-memory; no PostgreSQL or API
    key needed).
  - Frontend: `cd frontend && npm run lint && npm run build`.
- **Notes / risk callouts** — include only when relevant to what the diff
  touches. These encode invariants from `CLAUDE.md`:
  - **Schema changes** — a new Flyway migration (`V3__…`, etc.) under
    `resources/db/migration/`; Hibernate is `ddl-auto=validate`, so entity
    changes without a matching migration break startup. Call out if present.
  - **Two-layer refund enforcement** — changes to `service/PolicyEngine.java`
    (per-session tool ordering) and/or tool-internal DB re-validation in
    `ProcessRefundTool.execute()` (ownership, 30-day window, not-already-refunded).
  - **Anthropic client** — the `anthropic-version: 2023-06-01` header and
    model/key/max-tokens wiring in `ClaudeApiClient.java`.
  - **Build constraints** — any bump to `junit-jupiter.version` or
    reintroduction of `spring-boot-devtools` (blocked behind the Zscaler proxy).
  - **Session state** — `service/ContextManager.java` is in-memory and not
    multi-instance safe; note any assumption of persistence or horizontal scaling.

## 4. Output rules

- Deliver the full description as a **single fenced markdown block** so it can be
  copied and pasted directly into a PR.
- Do **not** run `gh pr create`, `gh pr edit`, or any other GitHub write command.
- Close by noting that **no changes were made to GitHub** and the user can paste
  the body into their pull request.
