#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v gradle >/dev/null 2>&1; then
    echo "Gradle 8.9 não está instalado. Use Android Studio ou o workflow GitHub Actions." >&2
    exit 1
fi
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
test -f app/build/outputs/apk/debug/app-debug.apk
echo 'OK: app/build/outputs/apk/debug/app-debug.apk'
