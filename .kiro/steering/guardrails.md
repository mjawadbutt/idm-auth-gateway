# Project Guardrails

## Developer Context

You are assisting a senior Java developer. Assume familiarity with design patterns, SOLID principles, and standard
enterprise best practices. Do not explain fundamentals unless explicitly asked.

---

## Tooling

Prefer Bash/Powershell/Cmd commands (Read/Write/Edit/Glob/Grep, etc) over tool usage WHENEVER possible — The user
reviews diffs via git and IDE local history, not the tool output. Use Write/Edit only when a targeted in-place patch is
cleaner than a shell command. Batch multi-file writes into one Bash call. Main objective of this one is to minimize
token usage.

---

## Response Style

- Do not generate documentation, docstrings, README sections, or usage examples unless explicitly requested.
- Add inline comments only on non-obvious logic.
- When multiple implementation options exist, pick the most explicit and maintainable one and briefly state why.
- Do not refactor code beyond the scope of the request.
- Use a direct tone — no hand-holding or over-explanation.

---

## Code Style

- **Use 2-space indentation** for all languages. Never use tabs or 4-space indents. This applies to all generated code
  regardless of language. Code will be read and maintained by humans. Prioritize readability, clarity, and explicitness
  over cleverness or brevity.
- **Use 2-space indentation** for all languages. Never use tabs or 4-space indents. This applies to all generated code
  regardless of language.
- **High cohesion, low coupling.** Each class and function has one clear responsibility.
- **Immutability and type-safety.** Always prefer immutable types and strong typing unless infeasible or in conflict
  with another guideline. Apply `final` to parameters. Apply `@NotNull` unless the value is genuinely nullable.
- **Expert pattern.** Place methods on the class that owns the data they operate on.
- **No method overloading.** Use distinct, descriptive method names instead.
- **Prefer verbose and readable code over concise code.** Line count is not a metric.
- **Consistency.** Follow existing patterns in the codebase unless doing so violates another guideline here.
- **Do not remove commented-out code or comments** unless explicitly asked to do so.
- **Use `jakarta.validation.constraints.NotNull`** (not `org.springframework.lang.NonNull`) for null-safety annotations.
- **Do not use `@Autowired`** on constructors in main application code — Spring injects single-constructor beans
  automatically. Only use `@Autowired` where required (e.g. JUnit 5 Spring context test classes).

---

## Git Usage

You are allowed to run the following git commands freely:

**Read / inspect:**

- `git status`, `git log`, `git diff`, `git show`, `git blame`, `git shortlog`
- `git branch`, `git tag`, `git stash list`
- `git ls-files`, `git cat-file`, `git rev-parse`, `git reflog`

**Local branch management:**

- `git checkout`, `git switch`, `git branch` (create/delete/rename local branches)
- `git merge`, `git rebase` (local only)
- `git cherry-pick`, `git reset`, `git restore`, `git revert`

**Staging / index:**

- `git add`, `git rm`, `git mv`
- `git stash`, `git stash pop`, `git stash apply`, `git stash drop`

**Config / maintenance:**

- `git config` (local only), `git clean`, `git gc`

All commands above are **pre-approved — run them without asking for confirmation**.

**Still require explicit user instruction before running:**

- Any remote interaction: `git fetch`, `git pull`, `git push`, `git remote`
- Any commit: `git commit`, `git merge --commit`, `git rebase` with auto-commit
- `git submodule` commands that touch remotes

---

## Deviations

Always state the reason before deviating from any guideline above.
