---
name: superpowers-plan
description: >-
  Execute a Superpowers implementation plan end to end and open one pull
  request. Use when a work-queue issue points at docs/superpowers/plans/*.md.
  Reads the plan and its spec, works the tasks in order with their TDD steps,
  ticks checkboxes as it commits, and reports human-only checks honestly.
metadata:
  opencode/slash: "false"
  opencode/autoinvoke: "false"
---

# Superpowers plan

The issue is claimed and the branch exists (work-queue steps 1 to 3). This skill
takes over at implementation and returns to work-queue step 5 (check) and 7
(PR) when the plan's last task is done.

A plan is authoritative. The plan and its spec decide scope, layering, naming
and tests; this skill only executes them.

## 1. Read before writing

Read the whole plan and the spec it names (usually
`docs/superpowers/specs/<same-stem>-design.md`), plus the repository's
`AGENTS.md` and `LLM_CODE_STYLE.md`.

Binding parts of the plan: **Global Constraints** (they override convenience)
and **Review Focus** (they are the acceptance criteria; every point needs a
test or an explicit check).

Ignore the plan header line asking for `superpowers:subagent-driven-development`
or `superpowers:executing-plans` — those skills are not installed here. This
skill is the executor.

If the plan names files that do not exist on `origin/master`, that is a
deviation, not a reason to invent them: follow section 4.

## 2. Work the tasks in order

For each `### Task N` block, in order:

1. Follow its steps literally: write the failing test, run it and confirm it
   fails for the stated reason, implement, run it again and confirm it passes.
2. Keep to the task's **Files** list. If a file outside it must change, say so
   in the commit body.
3. Commit with the plan's message prefixed by the issue number, for example
   `#42 feat: config for learner cookie and completion storage`.
4. Tick that task's `- [ ]` boxes in the plan file and commit the plan change
   **in the same commit**. Checkbox progress on the remote is the human's
   dashboard.

Do not batch tasks. Do not skip a task because it looks mechanical. Do not
reorder tasks: later tasks consume the interfaces earlier ones produce.

## 3. Verification task

The final task is usually "full check". Run its commands verbatim. A failure is
a blocker (work-queue step 8), not something to route around.

Where the plan asks for a manual check (open a page, look at mobile layout,
listen to a sound), do the headless part only: start the system, request the
URL, inspect the data file or rendered markup. Never claim a visual check
passed. Collect those into a "For human review" list for the PR body.

## 4. Deviations and blockers

When reality differs from the plan (renamed namespace, different library
version, a step that cannot work as written), make the smallest change that
keeps the plan's interface contract, and record it in the commit body and the
PR body under "Deviations".

Stop and block (work-queue step 8) when:

- the plan contradicts the spec on behaviour, or the spec is silent on a
  decision the task needs;
- a constraint in Global Constraints cannot be met;
- a check fails for a reason your change caused and the fix needs a product
  decision;
- the plan requires credentials, a live service, or a human action.

One or two sentences on the issue saying which task and step stopped you. No
half-finished PR.

## 5. Finish

Run work-queue step 5 (check) once more on the whole tree, then step 6 (push)
and step 7 (PR), with the PR body extended:

```markdown
Closes #<n>

<what the plan delivered, two or three sentences>

Tasks: <done>/<total>, all boxes ticked in <plan path>

Deviations: <none, or one line each>

Checked with: <commands run and their result>

For human review:
- <manual checks the plan asked for that need eyes>
```

## Hard limits

- One plan per issue and one PR per plan.
- No merge, no push to `master`, no rebase, no force-push.
- Never edit the plan's constraints to make a test pass.
- Never mark a checkbox done before its steps actually pass.
- Report in the final message: issue, branch, PR URL, tasks done, deviations,
  check outcome, or the task and reason for blocking.
