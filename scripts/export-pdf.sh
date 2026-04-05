#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# export-pdf.sh — Convert every REPORT.md in the proofs tree to REPORT.pdf.
#
# Usage:
#   bash scripts/export-pdf.sh                        # scans proofs/ by default
#   bash scripts/export-pdf.sh proofs/US-201          # specific story
#   bash scripts/export-pdf.sh proofs/US-201/<RUN_ID> # specific run
#
# Requires: md-to-pdf  (install once with: PUPPETEER_SKIP_DOWNLOAD=true npm install -g md-to-pdf)
#           System Chrome (already installed for Selenium tests)
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCAN_DIR="${1:-proofs}"

if [[ ! -d "${SCAN_DIR}" ]]; then
  echo "Directory not found: ${SCAN_DIR}"
  exit 1
fi

# ── Locate system Chrome ──────────────────────────────────────────────────────
find_chrome() {
  local candidates=(
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    "/Applications/Chromium.app/Contents/MacOS/Chromium"
  )
  for c in "${candidates[@]}"; do
    [[ -x "${c}" ]] && echo "${c}" && return
  done
  for cmd in google-chrome chromium-browser chromium; do
    command -v "${cmd}" &>/dev/null && echo "$(command -v "${cmd}")" && return
  done
  echo ""
}

CHROME_EXEC=$(find_chrome)
if [[ -z "${CHROME_EXEC}" ]]; then
  echo "Chrome not found. Install Google Chrome and re-run."
  exit 1
fi
echo "Chrome: ${CHROME_EXEC}"

# ── Locate md-to-pdf binary ───────────────────────────────────────────────────
find_md_to_pdf() {
  # 1. already on PATH
  command -v md-to-pdf &>/dev/null && command -v md-to-pdf && return

  # 2. npm global bin directory
  local npm_bin
  npm_bin=$(npm bin -g 2>/dev/null || true)
  [[ -n "${npm_bin}" && -f "${npm_bin}/md-to-pdf" ]] && echo "${npm_bin}/md-to-pdf" && return

  # 3. nvm node installations
  for p in ~/.nvm/versions/node/*/bin/md-to-pdf; do
    [[ -L "${p}" ]] && echo "${p}" && return
  done

  echo ""
}

MD_TO_PDF=$(find_md_to_pdf)

if [[ -z "${MD_TO_PDF}" ]]; then
  echo "md-to-pdf not found — installing (skipping bundled Chrome download)..."
  PUPPETEER_SKIP_DOWNLOAD=true npm install -g md-to-pdf
  MD_TO_PDF=$(find_md_to_pdf)
fi

if [[ -z "${MD_TO_PDF}" ]]; then
  echo "Could not locate md-to-pdf after install. Run manually:"
  echo "  PUPPETEER_SKIP_DOWNLOAD=true npm install -g md-to-pdf"
  exit 1
fi

echo "md-to-pdf: ${MD_TO_PDF}"
echo ""

# ── Build the --launch-options JSON ──────────────────────────────────────────
LAUNCH_OPTIONS="{\"executablePath\":\"${CHROME_EXEC}\",\"args\":[\"--no-sandbox\",\"--disable-dev-shm-usage\"]}"
PDF_OPTIONS='{"format":"A4","margin":{"top":"20mm","bottom":"20mm","left":"18mm","right":"18mm"}}'

# ── Convert each REPORT.md ────────────────────────────────────────────────────
CONVERTED=0
FAILED=0

while IFS= read -r -d '' report; do
  dir=$(dirname "${report}")
  pdf="${dir}/REPORT.pdf"

  echo "  → ${report}"

  pushd "${dir}" > /dev/null

  STATUS=0
  # Redirect stdin explicitly to /dev/null so that Puppeteer/Chrome
  # (launched inside md-to-pdf) cannot consume the process substitution
  # fd that the while-read loop is reading from.
  "${MD_TO_PDF}" REPORT.md \
    --launch-options "${LAUNCH_OPTIONS}" \
    --pdf-options "${PDF_OPTIONS}" \
    < /dev/null > /dev/null 2>&1 || STATUS=$?

  popd > /dev/null

  if [[ ${STATUS} -eq 0 && -f "${pdf}" ]]; then
    echo "     ✅ ${pdf}"
    (( CONVERTED++ )) || true
  else
    echo "     ❌ Failed (exit ${STATUS})"
    (( FAILED++ )) || true
  fi

done < <(find "${SCAN_DIR}" -name "REPORT.md" -print0)

echo ""
echo "Done — ${CONVERTED} PDF(s) written, ${FAILED} failed."
[[ ${FAILED} -eq 0 ]] || exit 1
