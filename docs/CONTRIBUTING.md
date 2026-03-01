# Contributing

## Detekt baseline policy

Detekt is enabled in CI and must stay strict (`ignoreFailures = false`).

To support incremental adoption on legacy code, the repository keeps module baselines:
- `app/detekt-baseline.xml`
- `feature-orders/detekt-baseline.xml`

### Rules of use
- Baseline captures only **existing** violations.
- Any **new** violation (not present in baseline) fails CI.
- Do **not** regenerate baseline as a routine fix for failing PRs.

### How to update baseline (only when consciously managing tech debt)
1. Fix a selected subset of existing violations.
2. Regenerate only the affected module baseline:
   - `./gradlew :app:detektBaseline`
   - `./gradlew :feature-orders:detektBaseline`
3. Commit baseline update together with code changes that paid down debt.
