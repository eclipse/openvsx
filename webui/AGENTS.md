# AGENTS.md

Operating rules for AI coding agents working on the Open VSX web UI
(`openvsx-webui`) — the published React component library in `src/` plus the
standalone app under `src/default/`. Claude Code loads this file via
`CLAUDE.md`; other agents read it directly.

This is the short, must-follow contract. The change workflow lives in the
`write-code` skill and test guidance in the `write-tests` skill. If any rule
here conflicts with an explicit request from the user, ask before overriding it.

## Non-negotiables

- **`yarn lint` must pass before you commit.** Full output (no `| tail`); fix
  every error, warning, and info. Never disable a rule or commit with
  `--no-verify`.
- **Every behavioral change ships with a unit test and a changelog entry.**
  Tests are how a change is verified (see the `write-tests` skill); changelog
  entries go under `## [next]` in `CHANGELOG.md`.
- **`yarn.lock` is the source of truth for dependencies.** Direct external deps
  stay pinned to exact versions; treat lockfile changes as reviewed code.
- **New source files need the EPL-2.0 license header** (copy it from any
  existing file).
- **Never commit unless the user asks**, and stage only the files you changed
  (`git add <path>`), never `git add -A` / `git add .`.

## Project shape

- **Pages/routes:** add a page under `src/pages/<area>/` and its path to that
  area's `*-routes.ts`; build paths with `createRoute` (`src/utils.ts`).
- **Components:** reusable UI goes in `src/components/`; keep design in the MUI
  theme, not inline `sx`, and prefer `rem` over `px`. Break large components
  into smaller, focused ones rather than growing one big component. Prefer a
  `styled` component over inline `sx` once the styling is more than trivial — a
  named styled component reads better than a long `sx` block, even for a single
  use; reserve `sx` for a one-off prop or two.
- **Data fetching** (mid-migration to TanStack Query): wrap the API client in a
  hook rather than calling the service straight from a component. Create a new
  hook in the enclosing folder of the feature that uses it; move it to
  `src/hooks/` only once a second place needs it. Follow the
  `tanstack-query-conventions` skill; migrate an existing endpoint only when the
  user asks, via the `migrate-to-tanstack` skill.
- **Shared state:** cross-component state goes in a context under `src/context/`.
- **Public API:** this package is published — anything consumers should use must
  be re-exported from `src/index.ts`. Keep that surface intentional.

## Changing code requires tests

Every behavioral change updates coverage:

- **vitest** unit tests under `test/unit/`, mirroring the source path
  (`yarn test`). Follow the `write-tests` skill; for a bug fix add a regression
  test and confirm it fails without the fix.
- A change with no matching test update is incomplete. Pure styling/config with
  no testable surface is the only exception — say so explicitly rather than
  skipping silently.

The Playwright smoke test in `test/e2e/` runs only when the user asks
(`yarn smoke-tests`).

## Workflow and code quality

Follow the `write-code` skill: understand → implement → test → changelog → lint.
The rules below always apply.

- Read files in full before wide-ranging changes, before editing files you have
  not inspected, and when investigating. Do not rely on search snippets.
- No `any` unless absolutely necessary. Check `node_modules` for external API
  types; don't guess.
- **No inline imports** (`await import()`, `import("pkg").Type`, dynamic type
  imports). Top-level imports only.
- Keep code comments short (1–3 lines): state only the non-obvious constraint or
  rationale, never narrate what the code does.
- Inline single-line helpers that have only one call site.
- Never remove or downgrade code to silence a type error from an outdated
  dependency — upgrade the dependency instead.
- Ask before removing functionality or code that appears intentional. Do not
  preserve backward compatibility unless the user asks for it.

## Conventions

- Commit subjects use a conventional-commit prefix: `feat:`, `fix:`, `chore:`,
  `docs:`, `test:`, `style:`, or `ci:`.
- No emojis in commits, pull requests, issues, or code. Keep prose concise,
  direct, and technical — no cheerful filler (e.g. "Thanks @user", not "Thanks
  so much @user!").
- Answer a user's question before making edits or running implementation
  commands.
- When responding to feedback or an analysis, say whether you agree or disagree
  before describing what you changed.

## Dependencies

- Install locally with `yarn install`; clean/CI-style with
  `yarn install --immutable`. Don't run lifecycle scripts unless the user asks.

## Git

Multiple sessions may be running in this cwd at the same time, each modifying
different files. Git operations that touch unstaged, staged, or untracked files
outside your own changes will stomp on other sessions' work. Follow these rules:

Committing:

- Only commit files YOU changed in THIS session.
- Stage explicit paths (`git add <path1> <path2>`); never `git add -A` / `git add .`.
- Before committing, run `git status` and verify you are only staging your files.

Never run (destroys other agents' work or bypasses checks):

- `git reset --hard`, `git checkout .`, `git clean -fd`, `git stash`, `git add -A`, `git add .`, `git commit --no-verify`.

If rebase conflicts occur:

- Resolve conflicts only in files you modified.
- If a conflict is in a file you did not modify, abort and ask the user.
- Never force push.

## Changelog

Location: `CHANGELOG.md`.

Sections under `## [next]`: `### Added`, `### Changed`, `### Fixed`, `### Removed`, `### Dependencies`.

Rules:

- All new entries go under `## [next]`. Read the full section first and append to existing subsections; never duplicate them; if subsection does not exist yet add it.
- Released version sections (e.g. `## [0.5.0]`) are immutable; never modify them.
