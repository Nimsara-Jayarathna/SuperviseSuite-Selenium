# Selenium Proof Report

| | |
|---|---|
| **Story** | `US-201` |
| **Suite** | `supervisorregistrationtest` |
| **Browser** | `chrome` |
| **Run ID** | `20260405-104042` |
| **Commit** | `9796bfd00cee5c96c11708b4710dcffd0cbf1aa5` |
| **Result** | 13 ✅ passed, 0 ❌ failed (13 total) |

## Summary

| # | Test Case | Outcome | Category | Severity |
|---|---|---|---|---|
| 1 | [TC-01: Valid data → success modal shown, then redirected to /login](#tc-01-valid-data-success-modal-shown-then-redirected-to-login-passed) | ✅ `passed` | `happy_path` | `critical` |
| 2 | [TC-02: Empty form submit → required-field errors for all five fields](#tc-02-empty-form-submit-required-field-errors-for-all-five-fields-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 3 | [TC-03: Gmail address → SLIIT institutional email domain error](#tc-03-gmail-address-sliit-institutional-email-domain-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 4 | [TC-04: @my.sliit.lk address → SLIIT institutional email domain error](#tc-04-mysliitlk-address-sliit-institutional-email-domain-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 5 | [TC-05: Malformed email string → valid email format error](#tc-05-malformed-email-string-valid-email-format-error-passed) | ✅ `passed` | `client-side_validation` | `minor` |
| 6 | [TC-06: Password < 8 chars → minimum length error](#tc-06-password-8-chars-minimum-length-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 7 | [TC-07: Password with no uppercase letter → uppercase required error](#tc-07-password-with-no-uppercase-letter-uppercase-required-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 8 | [TC-08: Password with no lowercase letter → lowercase required error](#tc-08-password-with-no-lowercase-letter-lowercase-required-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 9 | [TC-09: Password with no digit → digit required error](#tc-09-password-with-no-digit-digit-required-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 10 | [TC-10: Password with no special character → special character required error](#tc-10-password-with-no-special-character-special-character-required-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 11 | [TC-11: Mismatched passwords → passwords do not match error](#tc-11-mismatched-passwords-passwords-do-not-match-error-passed) | ✅ `passed` | `client-side_validation` | `normal` |
| 12 | [TC-12: Duplicate email → backend conflict error shown in general error banner](#tc-12-duplicate-email-backend-conflict-error-shown-in-general-error-banner-passed) | ✅ `passed` | `backend_validation` | `critical` |
| 13 | [TC-13: Register then login → authenticated as SUPERVISOR and redirected to supervisor dashboard](#tc-13-register-then-login-authenticated-as-supervisor-and-redirected-to-supervisor-dashboard-passed) | ✅ `passed` | `integration_with_authentication` | `blocker` |

## Test Results

### TC-01: Valid data → success modal shown, then redirected to /login — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `happy_path` |
| **Severity** | `critical` |
| **Captured** | `2026-04-05 10:40:54` |

**Test criteria:** Fill all fields with valid @sliit.lk credentials. Expect the success modal to appear, then an automatic redirect to /login.

![tc-01__valid_data___success_modal_shown__then_redirected_to__login](passed/happy_path/20260405-104054-tc-01__valid_data___success_modal_shown__then_redirected_to__login.png)

---

### TC-02: Empty form submit → required-field errors for all five fields — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:40:55` |

**Test criteria:** Submit the form without filling any field. All five required-field errors must appear.

![tc-02__empty_form_submit___required-field_errors_for_all_five_fields](passed/client-side_validation/20260405-104055-tc-02__empty_form_submit___required-field_errors_for_all_five_fields.png)

---

### TC-03: Gmail address → SLIIT institutional email domain error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:40:57` |

**Test criteria:** Enter a gmail.com address. The SLIIT domain validator must reject it.

![tc-03__gmail_address___sliit_institutional_email_domain_error](passed/client-side_validation/20260405-104057-tc-03__gmail_address___sliit_institutional_email_domain_error.png)

---

### TC-04: @my.sliit.lk address → SLIIT institutional email domain error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:40:59` |

**Test criteria:** Enter a @my.sliit.lk student portal address. Supervisors must use @sliit.lk only; the validator must reject the student domain.

![tc-04___my.sliit.lk_address___sliit_institutional_email_domain_error](passed/client-side_validation/20260405-104059-tc-04___my.sliit.lk_address___sliit_institutional_email_domain_error.png)

---

### TC-05: Malformed email string → valid email format error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `minor` |
| **Captured** | `2026-04-05 10:41:00` |

**Test criteria:** Enter a string that is not a valid email address. The format validator must reject it.

![tc-05__malformed_email_string___valid_email_format_error](passed/client-side_validation/20260405-104100-tc-05__malformed_email_string___valid_email_format_error.png)

---

### TC-06: Password < 8 chars → minimum length error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:02` |

**Test criteria:** Enter a password shorter than 8 characters. The minimum-length rule must fire.

![tc-06__password___8_chars___minimum_length_error](passed/client-side_validation/20260405-104102-tc-06__password___8_chars___minimum_length_error.png)

---

### TC-07: Password with no uppercase letter → uppercase required error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:03` |

**Test criteria:** Enter a password with no uppercase letter. The strength validator must require one.

![tc-07__password_with_no_uppercase_letter___uppercase_required_error](passed/client-side_validation/20260405-104103-tc-07__password_with_no_uppercase_letter___uppercase_required_error.png)

---

### TC-08: Password with no lowercase letter → lowercase required error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:04` |

**Test criteria:** Enter a password with no lowercase letter. The strength validator must require one.

![tc-08__password_with_no_lowercase_letter___lowercase_required_error](passed/client-side_validation/20260405-104104-tc-08__password_with_no_lowercase_letter___lowercase_required_error.png)

---

### TC-09: Password with no digit → digit required error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:06` |

**Test criteria:** Enter a password with no numeric digit. The strength validator must require one.

![tc-09__password_with_no_digit___digit_required_error](passed/client-side_validation/20260405-104106-tc-09__password_with_no_digit___digit_required_error.png)

---

### TC-10: Password with no special character → special character required error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:07` |

**Test criteria:** Enter a password with no special character. The strength validator must require one.

![tc-10__password_with_no_special_character___special_character_required_error](passed/client-side_validation/20260405-104107-tc-10__password_with_no_special_character___special_character_required_error.png)

---

### TC-11: Mismatched passwords → passwords do not match error — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `client-side_validation` |
| **Severity** | `normal` |
| **Captured** | `2026-04-05 10:41:08` |

**Test criteria:** Enter different values for password and confirm password. The mismatch error must appear.

![tc-11__mismatched_passwords___passwords_do_not_match_error](passed/client-side_validation/20260405-104108-tc-11__mismatched_passwords___passwords_do_not_match_error.png)

---

### TC-12: Duplicate email → backend conflict error shown in general error banner — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `backend_validation` |
| **Severity** | `critical` |
| **Captured** | `2026-04-05 10:41:16` |

**Test criteria:** Register successfully once, then attempt to register again with the same email. The backend must return a conflict error displayed in the general error banner.

![tc-12__duplicate_email___backend_conflict_error_shown_in_general_error_banner](passed/backend_validation/20260405-104115-tc-12__duplicate_email___backend_conflict_error_shown_in_general_error_banner.png)

---

### TC-13: Register then login → authenticated as SUPERVISOR and redirected to supervisor dashboard — passed

| Field | Value |
|---|---|
| **Outcome** | ✅ `PASSED` |
| **Category** | `integration_with_authentication` |
| **Severity** | `blocker` |
| **Captured** | `2026-04-05 10:41:22` |

**Test criteria:** Register a supervisor, then log in with the same credentials. Verify authentication succeeds, user role is SUPERVISOR, and app redirects to supervisor dashboard route.

![tc-13__register_then_login___authenticated_as_supervisor_and_redirected_to_supervisor_dashboard](passed/integration_with_authentication/20260405-104122-tc-13__register_then_login___authenticated_as_supervisor_and_redirected_to_supervisor_dashboard.png)

---

