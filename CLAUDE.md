# Global Development Guidelines

This file contains project-level instructions that apply to every session in this project.

## Documentation Standards

**DO NOT**
- DO NOT USE icons or emojis in any documentation and code like `README.md` file for any project unless stated otherwise.

**DO**
- For all documents used in code documentation. ALWAYS USE title case (Title-Case or Title Case) for the document EXCEPT for README.md file. All documentation used for coding should be in markdown format unless stated otherwise. You may use underscore, hyphens or spaces in the name of the document.

---

## 1. Repository Etiquette

### Branch Naming Conventions

Follow these patterns for branch names:

- **Feature branches**: `feature/<short-description>`
  - Example: `feature/user-authentication`
- **Bug fixes**: `fix/<issue-description>`
  - Example: `fix/login-timeout`
- **Hotfixes**: `hotfix/<critical-issue>`
  - Example: `hotfix/security-patch`
- **Refactoring**: `refactor/<scope>`
  - Example: `refactor/database-layer`
- **Documentation**: `docs/<topic>`
  - Example: `docs/api-endpoints`
- **Experimental/Research**: `experiment/<feature-name>`
  - Example: `experiment/new-algorithm`

**Rules**:
- The default main branch name will be `main` unless specified
- Use lowercase letters and hyphens (kebab-case)
- Keep names concise but descriptive
- Avoid special characters except hyphens and forward slashes

### Commit Message Style

Follow **Conventional Commits** specification:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, missing semicolons, etc.)
- `refactor`: Code refactoring without changing functionality
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `build`: Changes to build system or dependencies
- `ci`: Changes to CI/CD configuration
- `chore`: Other changes that don't modify src or test files
- `revert`: Reverting a previous commit

**Examples**:
```
feat(auth): add OAuth2 login support

fix(api): resolve timeout issue in user endpoint

docs(readme): update installation instructions

refactor(database): optimize query performance
```

**Rules**:
- Use lowercase for type and scope
- Subject line max 50 characters
- Use imperative mood ("add" not "added" or "adds")
- Don't end subject with a period
- Separate subject from body with blank line
- Body should explain what and why, not how
- Reference issue numbers in footer when applicable
- For multiple commits, keep point wise short descriptions for clarity

**DO NOT**
- Do not use claude as a name for commits or any claude reference
- NEVER push to remote without explicit user confirmation — see Git Push Policy below

**DO**
- Use the user like `themaverick` for all commits

### Git Push Policy

**NEVER run `git push` automatically.** Always stop and ask the user before pushing to any remote.

Before pushing, ask:
1. Are you on the correct branch, or should changes go to a new branch?
2. Confirm the user wants to push now

**Branch prompt template** — ask this whenever starting work on any new change:

```
Before I start, a quick check:
- Current branch: <branch name>
- Should I work on this branch, or create a new one?
  Options: (1) Stay on current  (2) New feature branch  (3) New fix branch
```

Wait for the user's answer before writing any code or making any commits.

---

## 2. Code Style Guidelines

### General Principles

1. **Readability over cleverness**: Write code that is easy to understand
2. **Consistency**: Follow existing patterns in the codebase
3. **DRY (Don't Repeat Yourself)**: Extract common logic into reusable functions
4. **SOLID principles**: Apply when designing classes and modules
5. **Meaningful names**: Use descriptive variable, function, and class names

### Naming Conventions

**Variables**:
- Follow the language specific coding specifications and recommendations for naming functions and methods. E.g in java we follow camel case (camelCase), in python we follow lowercase with underscore (snake case), etc
- Use UPPER_SNAKE_CASE for constants: `MAX_RETRY_COUNT`, `API_BASE_URL`
- Boolean variables should be prefixed with `is`, `has`, `should`: `isValid`, `hasPermission`

**Functions/Methods**:
- Use verbs for function names: `calculateTotal()`, `validateInput()`
- Keep functions small and focused on a single responsibility
- Avoid side effects when possible

**Classes**:
- Follow Java naming conventions
- Name classes after nouns that represent their responsibility

**Files**:
- Follow language conventions (camelCase for JS/TS, snake_case for Python, etc.)
- Match filename to primary export/class name

### Code Organization

1. **File structure**:
   - Group related functionality together
   - Separate concerns (e.g., models, views, controllers)
   - Keep files under 300 lines when possible

2. **Import order**:
   - Standard library imports first
   - Third-party libraries second
   - Local imports last
   - Alphabetize within each group

3. **Function/method order**:
   - Public methods before private methods
   - Related functions grouped together
   - Most important functions near the top

### Comments and Documentation

1. **When to comment**:
   - Explain WHY, not WHAT (code should be self-documenting for what)
   - Document complex algorithms or business logic
   - Add TODO/FIXME comments with context

2. **Comment style**:
   - Use clear, concise language
   - Keep comments up-to-date with code changes
   - Remove commented-out code before committing

### Error Handling

1. **Always handle errors**: Don't let errors fail silently
2. **Use specific error types**: Create custom error classes when needed
3. **Provide context**: Include relevant information in error messages
4. **Log appropriately**: Use proper logging levels (debug, info, warn, error)

### Testing Standards

1. **Write minimal unit tests for**:
   - All new features
   - Bug fixes (regression tests)
   - Critical business logic

2. **Test organization**: Follow Java JUnit testing methodology

### Performance Considerations

1. **Avoid premature optimization**: Write clear code first
2. **Profile before optimizing**: Measure to find actual bottlenecks
3. **Consider complexity**: Be aware of Big O notation for algorithms
4. **Cache when appropriate**: But avoid caching too aggressively

### Security Best Practices

1. **Never commit secrets**: Use environment variables for sensitive data
2. **Validate input**: Always sanitize user input
3. **Use parameterized queries**: Prevent SQL injection
4. **Use latest dependencies for new projects**: When creating a new project use latest version of libraries. DON'T automatically update if the project is not new as it may break existing code. Instead ASK the user if the library should be updated in case of high or critical security issues known in a library.
5. **Follow principle of least privilege**: Grant minimum necessary permissions

---

## 3. Language-Specific Guidelines

### General
- Follow the official style guide for your language
- Use linters and formatters (ESLint, Prettier, Black, etc.)
- Configure editor to format on save

---

## 4. New Feature Protocol

**ALWAYS brainstorm before writing any code for a new feature.** This applies to any task tagged as a feature, enhancement, or any work that introduces new behaviour not already designed in the roadmap.

### Brainstorm Format

Before touching any code, produce this analysis:

```
Feature Brainstorm: <feature name>
-------------------------------------------------
Goal            : What problem does this solve?
Approach options: List 2-3 ways to implement it
Recommended     : Which approach and why
Trade-offs      : What does the chosen approach give up?
Affected files  : Which files/modules will change?
Risks           : What could go wrong?
Questions       : Anything that needs user clarification before starting?
-------------------------------------------------
```

Wait for user confirmation or feedback before proceeding to implementation.

This step is skipped only for:
- Bug fixes with a clearly identified root cause
- Chore tasks (dependency updates, config changes, doc edits)
- Tasks already fully designed in ROADMAP.md with explicit implementation steps

---

## 5. Deployment and Infrastructure

### Docker Pre-flight Check

**ALWAYS verify Docker is running before executing any Docker or Docker Compose command.**

Before running any of the following — `docker`, `docker compose`, `docker-compose`, `docker build`, `docker run`, `docker ps` — run:

```bash
docker info > /dev/null 2>&1 && echo "Docker is running" || echo "Docker is NOT running"
```

If Docker is not running, stop immediately and prompt the user:

```
Docker does not appear to be running.
Please start Docker Desktop (or your Docker daemon) and confirm when ready.
Do not retry until the user confirms.
```

Do not attempt to start Docker automatically. Wait for the user.

---

## 6. Session Continuity

Task tracking and progress is always maintained in `ROADMAP.md` at the project root. Do not track tasks in this file.

### Task Status Definitions

Use these markers consistently in `ROADMAP.md` for all tasks:

| Marker | Status | Rule |
|--------|--------|------|
| `[ ]` | Pending | Not yet started |
| `[~]` | In Progress | Started but incomplete — always note what remains |
| `[x]` | Completed | Done and committed — always note the commit hash |
| `[!]` | Blocked | Cannot proceed — always note the reason and what resolves it |

Example:
```markdown
- [x] Delete com.werkflow.* package in Finance *(commit: abc1234)*
- [~] Delete com.werkflow.* package in Procurement *(VendorController done, ProcurementController pending)*
- [ ] Fix RestServiceDelegate blocking issue
- [!] Remove HR embedded Flowable *(blocked: depends on RestServiceDelegate fix)*
```

### Session Start Protocol

At the start of every session:
1. Read `CLAUDE.md` for project-level instructions
2. Read `ROADMAP.md` to identify the current phase and task statuses
3. Run `git log --oneline -10` to confirm what was last committed
4. Ask the branch question (see Git Push Policy) before starting any work
5. Resume from the first `[~]` task, or the first `[ ]` task if none are in progress

### During a Session

After completing each task:
1. Update its marker in `ROADMAP.md` to `[x]` and append the commit hash
2. Mark the next task as `[~]` in `ROADMAP.md`
3. Git commit all changes including the `ROADMAP.md` update before moving on
4. Never push — wait for user confirmation

### On Hitting Context Limits

Before stopping:
1. Update the current task marker to `[~]` in `ROADMAP.md` with a precise note on exactly where execution stopped
2. Git commit all in-progress changes along with the `ROADMAP.md` update
3. The next session will resume automatically using the Session Start Protocol above
