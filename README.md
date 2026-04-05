# SuperviseSuite Selenium E2E Tests

End-to-end UI automation for SuperviseSuite supervisor registration/auth flows using:
- Selenium WebDriver
- JUnit 5
- Allure reporting

## Prerequisites

Before running tests:
- Frontend running at `http://localhost:5173`
- Backend running at `http://localhost:8080`
- Database available for backend

## Run Tests

Team standard (recommended):

```bash
cp .env.example .env
./scripts/run-tests.sh
```

How `run-tests.sh` works:
- `run.mode=single` -> runs one browser from `browser`
- `run.mode=multi` -> runs comma-list from `run.browsers`

Common `.env` modes:

```env
# Fast result run
run.mode=single
browser=chrome
speed.profile=fast
```

```env
# Full browser matrix run
run.mode=multi
run.browsers=chrome,firefox,safari
speed.profile=fast
```

Direct Maven (advanced/manual):

```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
mvn test -Dbrowser=safari
```

Optional built-in Maven profile:

```bash
mvn test -Pmulti-browser
```

After run, archive proofs:

```bash
./scripts/archive-proofs.sh US-201 <RUN_ID>
```

Notes:
- Safari requires enabling `Develop > Allow Remote Automation`.
- Supported browser values: `chrome`, `firefox`, `safari`.
- `speed.profile` presets:
  - `fast`: step `0`, char `0` (instant)
  - `normal`: step `700`, char `60` (default)
  - `slow`: step `1200`, char `80` (clear demo)
- `step.delay.ms` and `char.delay.ms` override profile presets only when explicitly set.

## Architecture (SOLID-friendly)

Shared/reusable layers:
- `tests/BaseUiTest`  
  Centralized driver lifecycle + shared JUnit extension wiring.
- `config/TestConfig`  
  Centralized system-property config (browser policy, timeouts, speed profile, story key).
- `driver/DriverFactory`  
  Browser-specific WebDriver creation.
- `extensions/ScreenshotExtension`  
  Cross-cutting screenshot + artifact/report publishing for all tests.
- `pages/*`  
  Page Objects only (UI interactions + page-level queries).

Test classes (for example `SupervisorRegistrationTest`) only contain scenario logic/assertions.

## Reporting and Artifacts

Allure raw results:
- `target/allure-results`

Runtime screenshot artifacts:
- `target/test-artifacts/<USER_STORY>/<TEST_SUITE>/<BROWSER>/passed/<story-category>/*.png`
- `target/test-artifacts/<USER_STORY>/<TEST_SUITE>/<BROWSER>/failed/<story-category>/*.png`

Committable proof set (with Markdown report and images, outside `target/`):
- `proofs/<USER_STORY>/<RUN_ID>/<TEST_SUITE>/<BROWSER>/REPORT.md`
- `proofs/<USER_STORY>/<RUN_ID>/<TEST_SUITE>/<BROWSER>/SUMMARY.json`
- `proofs/<USER_STORY>/<RUN_ID>/<TEST_SUITE>/<BROWSER>/passed/<story-category>/*.png`
- `proofs/<USER_STORY>/<RUN_ID>/<TEST_SUITE>/<BROWSER>/failed/<story-category>/*.png`

Example:
- `proofs/US-201/20260405-082133/supervisorregistrationtest/chrome/REPORT.md`
- `proofs/US-201/20260405-082133/supervisorregistrationtest/chrome/failed/client-side-validation/...png`

Each saved image includes a top banner with:
- outcome (`PASSED` / `FAILED`)
- user story key (`US-201`)
- category (from `@Story`)
- timestamp

Story key source priority:
1. `@UserStory` on test method
2. `@UserStory` on test class
3. `-Dtest.story.key=...` fallback

Generate/open Allure report:

```bash
mvn allure:serve
```

Proof report behavior:
- `REPORT.md` is rewritten during the run and includes:
  - run metadata (story, suite, browser, run id)
  - git commit hash
  - capture index table
  - embedded screenshots for each captured test state
- `SUMMARY.json` is generated for CI/machine-readable validation.

Skip/abort evidence:
- Aborted/disabled tests generate proof cards as images, so every outcome has visual evidence.

## Current User Story Coverage (Supervisor Registration)

Implemented scenarios:
- Successful registration with SLIIT email and redirect to login.
- Invalid email domain validation (non-SLIIT and `@my.sliit.lk`).
- Duplicate account prevention.
- Input validation for required fields and password/email rules.
- Registration -> Login integration flow:
  - authenticates successfully
  - redirects to supervisor route
  - confirms authenticated role is `SUPERVISOR`

## Story-Based Organization Pattern

For each new story:
1. Create a dedicated test class (for example `Us202ProjectCreationTest`) extending `BaseUiTest`.
2. Add `@UserStory("US-202")` at class level.
3. Keep test names in `TC-xx` format for deterministic artifact naming.
4. Reuse existing page objects and shared config; only new scenario logic should be added in the test class.

## Quality Controls

- Flake retry: `retry.max` controls retries per test (`0` disables retries).
- Naming guard: `naming.validation.enabled=true` + `test.name.pattern` enforce deterministic test names.
- Preflight checks: `preflight.enabled=true` validates frontend/backend reachability before browser startup.
- Optional backend role verification: enable `backend.role.assertion.enabled=true` for API-side role checks.

## Proof Archive Command

Zip a full proof run:

```bash
./scripts/archive-proofs.sh US-201 20260405-082133
```

## CI Artifact Publishing

GitHub Actions workflow:
- [.github/workflows/selenium-proofs.yml](/Users/nimsara/Desktop/SuperviseSuite/SuperviseSuite-Selenium/.github/workflows/selenium-proofs.yml)

It uploads:
- `proofs/**`
- `target/allure-results/**`

## Environment File Format

Use [.env.example](/Users/nimsara/Desktop/SuperviseSuite/SuperviseSuite-Selenium/.env.example) as the template.

Supported keys:
- `browser`
- `allowed.browsers`
- `run.mode`
- `run.browsers`
- `base.url`
- `implicit.wait.seconds`
- `page.load.timeout.seconds`
- `script.timeout.seconds`
- `modal.wait.seconds`
- `speed.profile`
- `step.delay.ms`
- `char.delay.ms`
- `test.story.key`
- `retry.max`
- `naming.validation.enabled`
- `test.name.pattern`
- `preflight.enabled`
- `preflight.timeout.seconds`
- `backend.base.url`
- `backend.role.assertion.enabled`
- `proofs.enabled`
- `proofs.dir`
- `proofs.run.id`

Resolution priority for config values:
1. JVM system property (`-Dkey=value`)
2. OS environment variable (`KEY` or mapped `KEY_WITH_UNDERSCORES`)
3. `.env` file
4. Built-in default
