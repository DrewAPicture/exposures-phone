---
name: commit-best-practices
description: Use whenever writing, reviewing, or amending a commit message in this repository. Covers message structure, summary-line rules, formatting conventions, linking issues/commits, and a pre-commit checklist.
---

# Commit Message Best Practices

Commit messages have more than one reader: the reviewer looking at the change today, another contributor scanning history six months from now, release-note authors, and tooling that parses commit history (changelogs, blame, bisect). Write for all of them. The message should explain the **what** and **why** of a change — the diff already shows the **how**.

## Message structure

A commit message is built from these parts, in order. Only the summary is required; include the rest when relevant.

1. **Summary** (required) — one line.
2. **Description** (recommended for anything non-trivial) — one or more paragraphs.
3. **Context links** (optional) — where the change was developed or discussed (a PR URL, a design doc, an issue thread).
4. **Follow-up reference** (when applicable) — ties a fast-follow or test-only commit back to the commit it follows up on.
5. **Cherry-pick / backport metadata** (when applicable) — who reviewed it and which commit it was cherry-picked from, for changes ported to a release or maintenance branch.
6. **Issue/ticket references** (optional) — which issues the commit fixes or relates to.

## Summary line

- One line, no line breaks.
- Aim for **~50 characters**, treat **70** as a hard ceiling.
- Use the **imperative mood**: "Relax null comparisons", not "Relaxes null comparisons" or "Relaxed null comparisons".
- Start with a capital letter, end with a period.
- A component or module prefix is fine when it adds signal (e.g. `Sync: `, `Auth: `).
- If you can't compress the change into one honest sentence, the commit is probably not atomic enough — split it.

## Description

- Separate from the summary by a **blank line**.
- Multiple paragraphs are fine, separated by blank lines; don't hand-wrap lines within a paragraph.
- Write for someone with no other context: a future contributor debugging this code, or someone drafting release notes from history alone.
- Good things to include: why the change was necessary, what alternatives were considered and rejected, any new hooks/APIs/surface area introduced, gotchas or edge cases, and backstory that explains a non-obvious decision.

## Formatting rules

- Every line starts with a capital letter and ends with a period (outside of code spans).
- Wrap function, method, hook, and variable names in backticks — this keeps them literal in Markdown-rendering tools (GitHub, chat integrations) instead of being mangled or misparsed.
- Issue/PR numbers (`#123`) and commit SHAs typically auto-link in GitHub and most chat tools — wrap them in backticks so they render as code, and avoid bare `#`-plus-digits text unless you intend that auto-link.
- Avoid words your issue tracker, CI, or bot integrations treat as special trigger keywords unless you intend the automated behavior they cause (e.g. auto-closing an issue, auto-labeling, auto-notifying a channel).
- Outside the summary line, there's no character limit — clarity wins over brevity in the description.

## Context links

Optional links to where the change was developed or discussed — most often a pull request URL, but a design doc or discussion thread works too. Skip this if the PR itself is invisible to future readers of the message (i.e. usually skip it — the PR *is* the commit on most platforms).

## Follow-up reference

When a commit is a direct continuation of earlier work (adding tests for a prior change, a fast-follow fix), say so explicitly:

- Preceded by a blank line.
- Reference the original commit by its short SHA in backticks, e.g. `` Follow-up to `a1b2c3d`. ``

## Cherry-pick / backport metadata

When porting a change to a release or maintenance branch:

- Preceded by a blank line.
- Two lines, back to back (no blank line between them):
  - `Reviewed by: <name>[, <name>...]`
  - `Cherry-picked from: <short SHA>`
- List every reviewer, comma-separated, if more than one person reviewed the port.

## Issue/ticket references

- Own line, directly below the description (or below the cherry-pick metadata, if present).
- Multiple issues: comma-separated.
- When a commit both fixes some issues and merely relates to others, split them clearly: `Fixes #123, #124. See #125.`
- Put a long list of references on their own line(s) rather than cramming them into a paragraph.
- Know your tracker's auto-close syntax and use it deliberately — most trackers close an issue automatically when a merged commit's message contains a recognized "fixes"/"closes" keyword next to its number.

## Examples

**Weak** — no context, no formatting, nothing for future readers to work with:

> don't use strict comparisons for ids. fixes bug.

**Better, but thin** — names the fix, still no *why*, no formatting discipline:

> Fixing category lookup and other places that use string ids. fixes #2000.

**Good** — atomic summary, explains the *why*, code references in backticks, proper issue reference:

> Repository: Relax string/int ID comparisons.
>
> IDs are sometimes provided as strings by callers. This is particularly evident in `resolveCategoryId()`, where the `selected` argument was not being respected because of a strict `===` check. Consumers of this API should also be wary of using strict comparisons for IDs coming from external input.
>
> Fixes #237.

## Pre-commit checklist

Before committing:

- Run the linter/formatter and static analysis for every language touched.
- Run the full test suite, not just the tests you think are affected.
- Re-read your own diff — check for accidental debug code, unrelated changes, and files that shouldn't be there.
- Confirm you're committing to one branch at a time — mixing multiple target branches into a single commit corrupts history and confuses anything that mirrors or syncs it downstream.
- During a release-candidate or code-freeze window, get a second reviewer before committing, even for small changes.
