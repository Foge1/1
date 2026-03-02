# Static analysis policy

## Goals

- Keep CI green for the existing codebase while enforcing strict gates for new code.
- Freeze current Detekt technical debt with baselines and prevent new debt from being introduced.
- Enforce deterministic Kotlin formatting in local development and CI.

## Detekt

### Configuration

- Shared config is stored at `config/detekt/detekt.yml`.
- `maxIssues = 0` remains enabled.
- `detekt-formatting` is applied in Kotlin Android modules so `formatting` rules in config are valid.

### Baselines

Current debt is frozen with module-specific baselines:

- `config/detekt/baseline-app.xml`
- `config/detekt/baseline-feature-orders.xml`

Baselines are wired per module in Gradle:

- `:app` -> `baseline-app.xml`
- `:feature-orders` -> `baseline-feature-orders.xml`

### How to reduce baseline safely

Do **not** regenerate baselines just to make CI green.

Only update baseline entries when:

1. The underlying code smell has been fixed in code.
2. The corresponding baseline IDs are removed as a follow-up cleanup.

This ensures new violations fail CI while old debt is gradually burned down.

## KtLint

### Local workflow

Before opening/updating a PR run:

```bash
./gradlew ktlintFormatAll
```

### CI gate

CI runs:

```bash
./gradlew ktlintCheckAll
```

If formatting or style checks fail, PR must be fixed before merge.

## Policy

- New technical debt is forbidden.
- Existing Detekt debt is tracked explicitly via baselines and should shrink over time.
- KtLint check is mandatory in CI for every PR.
