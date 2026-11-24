---
name: work-queue
description: >-
  Take one GitHub issue from the agent-ready queue and turn it into a pull
  request. Use when a run is started with "Process one item from the GitHub
  work queue" or when asked to work the agent queue. Handles claiming the
  issue, refusing underspecified work, branching, implementing, checking,
  committing, pushing and opening the PR.
metadata:
  opencode/slash: "false"
  opencode/autoinvoke: "false"
---

# Work queue

One run = one issue. The queue is this repository's GitHub issues with the
`agent-ready` label. A human writes and refines the issue; you implement it and
open a pull request. A human merges.

## 1. Pick and claim one issue

Bring the checkout up to date first. This clone is only updated by pulling, so
an issue and its plan are invisible until you do:

```bash
git status --porcelain    # report and stop if it shows someone else's changes
git switch master
git pull --ff-only
```

Then ask the queue:

```bash
gh issue list --label agent-ready --limit 1 --json number,title,body,labels
```

If the list is empty, reply with exactly `IDLE` and stop. Do not invent work.

Claim it before doing anything else, so a second run cannot take it:

```bash
gh issue edit <n> --add-label agent-working --remove-label agent-ready
```

## 2. Gate on the issue text

An issue is workable only when it states **what** to change **where** it applies
and **how** completion is verified. Concretely, it must contain acceptance
criteria: a checklist, a "done when" section, or an explicit expected behaviour
with the command or path that demonstrates it.

If any of that is missing or reads two ways:

```bash
gh issue edit <n> --add-label agent-blocked --remove-label agent-working
gh issue comment <n> --body "Blocked: <what is missing, in one or two sentences>."
```

Then stop. Do not guess scope, and do not ask a question — nobody is watching
the run. The person who refines the issue moves it back to `agent-ready`.

## 3. Branch

Start from an up-to-date default branch. This repository uses `master`, and
`master` is never pushed to by the agent.

```bash
git fetch origin
git switch -c agent/<n>-<short-slug> origin/master
```

`<short-slug>` is two to four lowercase words from the title, hyphenated. Never
reuse a branch name, never force-push, never rebase someone else's branch.

Work only on files the issue is about. Leave unrelated uncommitted changes
alone; this checkout is shared with humans.

## 4. Implement

If the issue names a plan under `docs/superpowers/plans/`, follow the
`superpowers-plan` skill instead of this section; it executes the plan's tasks
and returns to step 5 below.

Follow `AGENTS.md` and `LLM_CODE_STYLE.md` at the repo root. This is a Clojure
project with CLEAN architecture layering: dependencies point inwards, business
rules stay out of Ring and persistence code. Keep the change as small as the
issue allows.

## 5. Check before committing

Run the repository's check, in this order:

1. `make check` if a `Makefile` with that target exists.
2. The command in `.github/workflows/*` if CI exists.
3. Otherwise run the tests for every namespace you touched:

```bash
clojure -M:dev -e "(require 'clojure.test 'is.galt.<path>.<name>-test) (let [r (clojure.test/run-tests 'is.galt.<path>.<name>-test)] (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

Replace `is.galt.<path>.<name>-test` with the test namespace (file path with
`/` to `.`, `_` to `-`). If the change has no matching test namespace, at least
load every namespace you edited:

```bash
clojure -M:dev -e "(require 'is.galt.<ns1> 'is.galt.<ns2>)"
```

A check that fails because of your change is a blocker, not something to work
around. If it fails for reasons that predate your branch, say so in the PR body.

## 6. Commit and push

```bash
git add <files you changed>
git commit -m "#<n> <summary>"
git push -u origin agent/<n>-<slug>
```

`<summary>` is one line, imperative, describing the outcome (for example
`#42 add note deletion use case`). No trailers, no co-author lines.

## 7. Pull request

```bash
url=$(gh pr create --base master --title "#<n> <summary>" --body "Closes #<n>

<what changed and why, two or three sentences>

Checked with: <the command you ran>")
```

Then hand it to review and link it on the issue:

```bash
gh issue edit <n> --add-label agent-review --remove-label agent-working
gh issue comment <n> --body "Pull request: $url"
```

## 8. Blocked mid-work

If the issue turns out to be wrong, or a check cannot pass without a decision
only a human can make, stop and:

```bash
git checkout master
git branch -D agent/<n>-<slug>          # only if nothing was pushed
gh issue edit <n> --add-label agent-blocked --remove-label agent-working
gh issue comment <n> --body "Blocked: <one or two sentences>."
```

Do not open a half-finished PR.

## Hard limits

- One issue per run. When the PR is open, the run is over.
- Never merge, never push to `master`, never change branch protection.
- Never close an issue; `Closes #<n>` does that when the PR merges.
- Never edit labels other than the four in this queue, and never edit another
  issue.
- Never commit secrets, `.env` files, or generated artifacts.
- Report in the final message: issue number, branch, PR URL, check outcome, or
  the reason for blocking.
