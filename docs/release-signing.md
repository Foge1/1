# Release signing

## Security requirements

- Do not commit the keystore file into the repository.
- Keep all signing credentials in GitHub Secrets (CI) or local environment / Gradle properties (local).
- `:app:assembleProdRelease` works locally without signing credentials (build remains unsigned).
- Signed CI release requires all configured secrets.

## Variables used by Gradle

The app module reads these values from environment variables first, then from Gradle properties:

- `KEYSTORE_PATH`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

If all four values are provided, `signingConfigs.release` is applied to release builds.
If any value is missing, release is assembled unsigned.

## Local setup

Choose one of two approaches:

### Option A: environment variables

```bash
export KEYSTORE_PATH=/absolute/path/to/release.keystore
export KEYSTORE_PASSWORD='***'
export KEY_ALIAS='***'
export KEY_PASSWORD='***'
./gradlew :app:assembleProdRelease
```

### Option B: `~/.gradle/gradle.properties`

```properties
KEYSTORE_PATH=/absolute/path/to/release.keystore
KEYSTORE_PASSWORD=***
KEY_ALIAS=***
KEY_PASSWORD=***
```

Then run:

```bash
./gradlew :app:assembleProdRelease
```

Without these values, the same command still succeeds with an unsigned release APK.

## CI setup (GitHub Actions)

Required repository secrets:

- `KEYSTORE_BASE64` (base64-encoded keystore content)
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

CI flow:

1. Decode `KEYSTORE_BASE64` into a temporary file inside `$RUNNER_TEMP`.
2. Pass signing values through `env` (do not print secrets).
3. Run `./gradlew :app:assembleProdRelease`.

The workflow includes explicit checks to fail fast when any required secret is missing.
