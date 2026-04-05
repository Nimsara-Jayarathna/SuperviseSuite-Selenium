#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <story-key> <run-id> [proofs-dir]"
  exit 1
fi

STORY_KEY="$1"
RUN_ID="$2"
PROOFS_DIR="${3:-proofs}"
TARGET_DIR="${PROOFS_DIR}/${STORY_KEY}/${RUN_ID}"

if [[ ! -d "${TARGET_DIR}" ]]; then
  echo "Proof directory not found: ${TARGET_DIR}"
  exit 2
fi

ARCHIVE_NAME="${STORY_KEY}-${RUN_ID}-proofs.zip"
zip -r "${ARCHIVE_NAME}" "${TARGET_DIR}"
echo "Created ${ARCHIVE_NAME}"
