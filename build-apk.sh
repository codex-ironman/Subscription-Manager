#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v javac >/dev/null || { echo 'Install JDK 17 first.'; exit 1; }
command -v gradle >/dev/null || { echo 'Install Gradle 8.9 first.'; exit 1; }
subtrack_sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
subtrack_signer="$subtrack_sdk/build-tools/35.0.0/apksigner"
[[ -x "$subtrack_signer" ]] || { echo 'Install Android SDK platform 35 and build tools 35.0.0.'; exit 1; }
export SUBTRACK_KEYSTORE="${SUBTRACK_KEYSTORE:-$PWD/signing/subtrack.jks}"
export SUBTRACK_KEY_ALIAS="${SUBTRACK_KEY_ALIAS:-subtrack}"
[[ -f "$SUBTRACK_KEYSTORE" ]] || { echo 'Original release keystore required. No replacement key will be generated.'; exit 1; }
[[ -f "${SUBTRACK_PREVIOUS_APK:-}" ]] || { echo 'Set SUBTRACK_PREVIOUS_APK to the previously installed signed release APK for certificate verification.'; exit 1; }
subtrack_previous_cert="$("$subtrack_signer" verify --print-certs "$SUBTRACK_PREVIOUS_APK" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
[[ -n "$subtrack_previous_cert" ]] || { echo 'Could not verify the previous APK certificate.'; exit 1; }
if [[ -z "${SUBTRACK_STORE_PASSWORD:-}" ]]; then
  read -r -s -p 'Original keystore password: ' SUBTRACK_STORE_PASSWORD
  printf '\n'
fi
if [[ -z "${SUBTRACK_KEY_PASSWORD:-}" ]]; then
  read -r -s -p 'Key password (Enter if same): ' SUBTRACK_KEY_PASSWORD
  printf '\n'
fi
export SUBTRACK_STORE_PASSWORD
export SUBTRACK_KEY_PASSWORD="${SUBTRACK_KEY_PASSWORD:-$SUBTRACK_STORE_PASSWORD}"
gradle --no-daemon :app:assembleRelease
subtrack_apk="$PWD/app/build/outputs/apk/release/app-release.apk"
subtrack_new_cert="$("$subtrack_signer" verify --print-certs "$subtrack_apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')"
[[ "$subtrack_new_cert" == "$subtrack_previous_cert" ]] || { echo 'Signing certificate mismatch. Do not distribute this APK as an update.'; exit 1; }
printf '\nSame signing certificate verified. APK: %s\n' "$subtrack_apk"
