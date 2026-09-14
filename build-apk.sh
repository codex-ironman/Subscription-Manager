#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
command -v javac >/dev/null || { echo 'Install JDK 17 first.'; exit 1; }
command -v gradle >/dev/null || { echo 'Install Gradle 8.9 first.'; exit 1; }
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo 'Set ANDROID_HOME to your Android SDK directory (platform 35 and build tools 35.0.0).'
  exit 1
fi
mkdir -p signing
if [[ ! -f signing/subtrack.jks ]]; then
  echo 'Create your release signing key. Keep the key and passwords for all future updates.'
  keytool -genkeypair -v -keystore signing/subtrack.jks -alias subtrack -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=SubTrack Owner' 
fi
export SUBTRACK_KEYSTORE="$PWD/signing/subtrack.jks"
export SUBTRACK_KEY_ALIAS=subtrack
read -r -s -p 'Keystore password: ' SUBTRACK_STORE_PASSWORD
printf '\n'
read -r -s -p 'Key password (Enter if same): ' SUBTRACK_KEY_PASSWORD
printf '\n'
SUBTRACK_KEY_PASSWORD="${SUBTRACK_KEY_PASSWORD:-$SUBTRACK_STORE_PASSWORD}"
export SUBTRACK_STORE_PASSWORD SUBTRACK_KEY_PASSWORD
gradle --no-daemon :app:assembleRelease
printf '\nAPK: %s/app/build/outputs/apk/release/app-release.apk\n' "$PWD"
