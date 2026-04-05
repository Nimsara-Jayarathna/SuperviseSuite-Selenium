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

Default run:

```bash
mvn test
```

Run via `.env` (recommended for team consistency):

```bash
cp .env.example .env
mvn test
```

`.env` is auto-loaded by the test config. No manual `source .env` is required.

Run with browser:

```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
mvn test -Dbrowser=safari
```

Optional runtime config:

```bash
mvn test \
  -Dbrowser=chrome \
  -Dallowed.browsers=chrome,firefox,safari \
  -Dbase.url=http://localhost:5173 \
  -Dimplicit.wait.seconds=5 \
  -Dpage.load.timeout.seconds=30 \
  -Dscript.timeout.seconds=30 \
  -Dmodal.wait.seconds=10 \
  -Dspeed.profile=normal \
  -Dstep.delay.ms=700 \
  -Dchar.delay.ms=60 \
  -Dtest.story.key=US-201
```

Notes:
- Safari requires enabling `Develop > Allow Remote Automation`.
- Supported browser values: `chrome`, `firefox`, `safari`.
- `speed.profile` supports `fast`, `normal`, `slow`.
- `step.delay.ms` and `char.delay.ms` override profile defaults when provided.

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

Formatted screenshot artifacts:
- `target/test-artifacts/<USER_STORY>/<TEST_SUITE>/passed/<story-category>/*.png`
- `target/test-artifacts/<USER_STORY>/<TEST_SUITE>/failed/<story-category>/*.png`

Example:
- `target/test-artifacts/US-201/supervisorregistrationtest/failed/client-side-validation/...png`

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

## Environment File Format

Use [.env.example](/Users/nimsara/Desktop/SuperviseSuite/SuperviseSuite-Selenium/.env.example) as the template.

Supported keys:
- `browser`
- `allowed.browsers`
- `base.url`
- `implicit.wait.seconds`
- `page.load.timeout.seconds`
- `script.timeout.seconds`
- `modal.wait.seconds`
- `speed.profile`
- `step.delay.ms`
- `char.delay.ms`
- `test.story.key`

Resolution priority for config values:
1. JVM system property (`-Dkey=value`)
2. OS environment variable (`KEY` or mapped `KEY_WITH_UNDERSCORES`)
3. `.env` file
4. Built-in default
