# Dependency and version management

## Source of truth
- All dependency and plugin versions are centralized in `gradle/libs.versions.toml`.
- Module-level `build.gradle` files must reference dependencies through `libs.*` aliases.
- Root `build.gradle` must use plugin aliases from the version catalog.

## Adding a new dependency
1. Add (or reuse) a version in the `[versions]` section.
2. Add a library alias in `[libraries]`.
3. Use the alias in module dependencies, for example:
   - `implementation libs.androidx.core.ktx`
   - `testImplementation libs.junit4`
   - `kapt libs.androidx.room.compiler`
4. For Compose artifacts managed by BOM:
   - Declare `implementation platform(libs.androidx.compose.bom)` in the module.
   - Keep Compose artifact aliases in TOML without explicit versions.

## Updating versions
1. Change the corresponding value in `[versions]` only.
2. Avoid duplicating versions in Gradle module files.
3. Run project checks/builds after any version change.
