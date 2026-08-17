# AGENTS.md

Exposures phone companion app — Android, Kotlin. Pairs with the exposures-watch app.

## Commit messages

Follow the [`commit-best-practices`](.claude/skills/commit-best-practices/SKILL.md) skill when writing commit messages.

## Architecture questions

For phone app architecture, component ownership, and where to implement phone-side changes, invoke the `describe-phone-architecture` skill (lives in `~/.claude/skills/` — global, not part of this repo, so it stays available regardless of which Exposures repo a session started in).

## Kotlin best practices

For Kotlin coding standards (immutability, null safety, naming, idiomatic constructs, concurrency/error handling, formatting), invoke the `kotlin-best-practices` skill (lives in `~/.claude/skills/` — global, not part of this repo).
