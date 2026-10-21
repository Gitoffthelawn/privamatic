# <Feature> — spec

Run type: implementation run / read-only design run / release run. Branch: `<prefix>/<name>` (from `master`).

## Goal

<One or two sentences: what the user gets, or what bug stops happening.>

## Scope

**In:**
- <change>

**Out:**
- <thing that looks related but is not part of this run>

## Oliver's calls

Build the default and report it under **Found**. Once Oliver decides, record the decision and the date here.

| Question | Default |
|---|---|
| <question> | <default> |

## Invariants

The standing invariants are in `docs/reviews/REVIEW_PROMPT.md`; do not repeat them here. List only those specific to this run.
They become items 8+ of the external review prompt.

1. <invariant>

## Milestones

Each gate must pass on a running build (Pixel 8, via the `device-verify` subagent where a device is involved).

| # | Milestone | Gate (evidence that proves it) |
|---|---|---|
| 1 | <milestone> | <command / screenshot / test, and the expected result> |

## STOP points

| When | What Oliver does |
|---|---|
| After the diff review, before merge | Runs the external review (see CLAUDE.md, External review) and saves the findings file. |
| <when> | <what> |
