# Global Development Guidelines

This file contains project-level instructions that apply to this project.

## Documentation Standards

**DO NOT**
- DO NOT USE icons or emojis in any documentation and code like `README.md` file for any project unless stated otherwise.

**DO**
- For all documents used in code documentation. ALWAYS USE title case (Title-Case or Title Case) for the document EXCEPT for README.md file. All documrntation used for coding should be in markdown format unless stated otherwise. You may use underscore, hyphens or spaces in the name of the document.

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

**DO** 
- Use the user like `themaverick` for all commits

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
- Follow the language specific coding specifications and recommendations for naming functions and methods. E.g in java we follow camel case (camelCase), in python we follow lowecase with underscore (snake case), etc
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

3. **Test organization**: Follow Java JUnit testing methodology

### Performance Considerations

1. **Avoid premature optimization**: Write clear code first
2. **Profile before optimizing**: Measure to find actual bottlenecks
3. **Consider complexity**: Be aware of Big O notation for algorithms
4. **Cache when appropriate**: But avoid caching too aggressively

### Security Best Practices

1. **Never commit secrets**: Use environment variables for sensitive data
2. **Validate input**: Always sanitize user input
3. **Use parameterized queries**: Prevent SQL injection
4. **User latest dependencies for new projects**: When creating a new project use latest version of libraries. DON'T automatically update if the project is not new as it may break existing code. Instead ASK the user if the library should be updated in case of high or critical security issues known in a library.
5. **Follow principle of least privilege**: Grant minimum necessary permissions

---

## Language-Specific Guidelines

### General
- Follow the official style guide for your language
- Use linters and formatters (ESLint, Prettier, Black, etc.)
- Configure editor to format on save

---
