#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${1:-.env}"

get_value() {
  local key="$1"
  local default="${2:-}"
  if [[ -f "${ENV_FILE}" ]]; then
    local line
    line=$(grep -E "^${key}=" "${ENV_FILE}" | tail -n 1 || true)
    if [[ -n "${line}" ]]; then
      echo "${line#*=}"
      return 0
    fi
  fi
  echo "${default}"
}

trim() {
  local v="$1"
  v="${v#"${v%%[![:space:]]*}"}"
  v="${v%"${v##*[![:space:]]}"}"
  echo "${v}"
}

RUN_MODE=$(trim "$(get_value "run.mode" "single")")
RUN_BROWSERS=$(trim "$(get_value "run.browsers" "chrome,firefox,safari")")
SINGLE_BROWSER=$(trim "$(get_value "browser" "chrome")")
SPEED_PROFILE=$(trim "$(get_value "speed.profile" "normal")")
PROOFS_ENABLED=$(trim "$(get_value "proofs.enabled" "true")")
PROOFS_DIR=$(trim "$(get_value "proofs.dir" "proofs")")
STORY_KEY=$(trim "$(get_value "test.story.key" "UNASSIGNED")")
RUN_ID=$(trim "$(get_value "proofs.run.id" "")")
PREFLIGHT_ENABLED=$(trim "$(get_value "preflight.enabled" "true")")
BASE_URL=$(trim "$(get_value "base.url" "http://localhost:5173")")
BACKEND_BASE_URL=$(trim "$(get_value "backend.base.url" "http://localhost:8080")")

COMMON_ARGS=(
  "-Dspeed.profile=${SPEED_PROFILE}"
  "-Dproofs.enabled=${PROOFS_ENABLED}"
  "-Dproofs.dir=${PROOFS_DIR}"
  "-Dtest.story.key=${STORY_KEY}"
  "-Dpreflight.enabled=${PREFLIGHT_ENABLED}"
  "-Dbase.url=${BASE_URL}"
  "-Dbackend.base.url=${BACKEND_BASE_URL}"
)

if [[ -n "${RUN_ID}" ]]; then
  COMMON_ARGS+=("-Dproofs.run.id=${RUN_ID}")
fi

if [[ "${RUN_MODE}" == "multi" ]]; then
  IFS=',' read -r -a BROWSERS <<< "${RUN_BROWSERS}"
  for b in "${BROWSERS[@]}"; do
    BROWSER=$(trim "${b}")
    [[ -z "${BROWSER}" ]] && continue
    echo "Running tests for browser=${BROWSER}"
    mvn test "-Dbrowser=${BROWSER}" "${COMMON_ARGS[@]}"
  done
else
  echo "Running tests for browser=${SINGLE_BROWSER}"
  mvn test "-Dbrowser=${SINGLE_BROWSER}" "${COMMON_ARGS[@]}"
fi
