#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp /tmp/sinalbt-model-XXXXXX.jar)"
trap 'rm -f "$OUT"' EXIT
kotlinc app/src/main/java/br/com/vertice81/sinalbt/model/Models.kt app/src/main/java/br/com/vertice81/sinalbt/model/DeveloperInfo.kt tools/ModelSmoke.kt -include-runtime -d "$OUT"
java -jar "$OUT"
