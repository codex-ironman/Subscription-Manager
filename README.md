# My Subscription Manager 1.2

An offline subscription manager for the owner, with received-payment earnings. Source code for the Android app and its matching browser version. GitHub Actions compiles an unsigned release APK; the private release key stays outside this public repository.

## Version 1.2

- Android 11+ uses an inset outer layout to keep the WebView clear of system bars, cutouts and the keyboard.
- Choose an existing customer or tap Another service to reuse their name and WhatsApp number. Each subscription keeps separate validity and credentials.
- Optional service login ID/email and masked password; credentials never appear in reminder messages.
- Android saved records use Android Keystore AES-GCM encryption. Legacy records migrate on read.
- Portable backups are password-protected with AES-256-GCM and PBKDF2-SHA256 (210,000 iterations). Legacy unencrypted JSON backups can still be imported. Backup passwords cannot be recovered.
- The standalone browser version stores live records in its browser profile. Prefer Android for sensitive credentials.
- Online sync is not connected; encrypted backup/restore supports manual transfer between devices.
- Same application ID and signing key as 1.1.0; version code 3.
- The build checks mobile browser layout at 320/360/412px, multiple subscriptions per person, credential persistence and encrypted backups. Native Android inset/storage behavior still needs device testing.

## Version 1.1 additions

- Owner dashboard named **My Subscription Manager**.
- **Total received this month** in INR. Earnings are gross recorded receipts, not profit or unpaid subscription prices.
- A six-month earnings bar graph; choose the ending month to inspect earlier periods. Exact amounts are available in payment history. The current-month comparison is against the previous full month.
- Payment ledger with received date, customer/service snapshot, optional payment note and deletion of mistaken entries. Amounts are stored as integer paise to avoid rounding errors in totals.
- New subscription: optionally record the quoted amount as received. Otherwise it remains unpaid/unrecorded. Each subscription also has a separate **Payment** button.
- **Other** in the provider selector reveals a separate required custom-name field.
- **Extend validity** opens a form with quick +7/+30/+90-day choices and a preview of the new expiry. It does not automatically add income.
- Version 2 backups include payments. Version 1 backups migrate without inventing historical earnings. Deleting a subscription retains its received-payment history; restoring a backup replaces all records, payments and settings.

## Included

- Customer name, normalized WhatsApp number, service, start date/time, validity in whole days, optional renewal amount and notes.
- A live countdown recalculated from the expiry timestamp, so closing the app does not reset validity. One day means 24 hours. Uses the device clock.
- Green active plans, amber plans due within 3 days, red expired plans, and scheduled future plans.
- Active/expiring/expired filters and name/phone/service search. Nearest expiry first.
- Renew, edit and confirm-before-delete. Active renewals extend the existing expiry; expired renewals start now.
- Editable provider choices: Netflix, Prime Video, Hotstar, SonyLIV, ZEE5, Spotify, YouTube, regional OTT services, bundles, Nuvio, and custom names. This is an editable label catalog, not a verified exhaustive list of currently operating providers.
- WhatsApp reminders in Hinglish or English, with optional amount, sender name and PhonePe number. Opens a prepared message for review and manual Send. No automatic WhatsApp delivery or payment processing.
- Android local notification checks approximately hourly when enabled, for plans expiring within 3 days and expired plans. Deduplicates each plan/status/day. Reboot-persistent scheduling. Android can delay checks; force-stop pauses them until reopening. Notifications do not send WhatsApp messages.
- Export/restore through Android's file picker. Versioned JSON backup, validation, size limit, and replace confirmation. Settings are included. New backups are password-encrypted; keep the password safe.
- No login, ads, analytics, external SDKs or Internet permission. Android customer records are encrypted in app-private preferences. Android automatic cloud backup is disabled; export manually before uninstalling or changing phones.

## Browser version

Open `app/src/main/assets/index.html` in a browser that supports local storage. It contains the same UI and core features. Browser data belongs to that browser/device, so export a backup before clearing browser data. Background Android notifications are not available in this version. Browser downloads and WhatsApp opening depend on the browser's support.

## Build an installable signed APK

Requirements: JDK 17, Gradle 8.9, Android SDK platform 35 and build-tools 35.0.0. Set `ANDROID_HOME` to the SDK directory. No Gradle wrapper binary is bundled.

On Linux/macOS:

```bash
./build-apk.sh
```

The script creates a release signing key locally if absent, prompts for passwords, and builds `app/build/outputs/apk/release/app-release.apk`. Preserve `signing/subtrack.jks` and its password for future updates. Do not commit it or publish passwords. The source has no embedded signing key.

In Android Studio: open this folder, install the requested SDK, sync Gradle, then use **Build > Generate Signed App Bundle / APK > APK**. Create and preserve your own signing key. The Gradle project targets Android 15 with minimum Android 8.0 and no native CPU-specific libraries.

For a quick development build with a local SDK and Gradle:

```bash
gradle :app:assembleDebug
```

That uses a development key; use a release key for a lasting installation.

## GitHub Actions

The build runs on relevant pushes to `main`, or manually from Actions. It runs the core tests, compiles the release APK, and uploads an unsigned APK plus the standard Android signing tool for the owner to sign privately. Artifacts expire after one day. No signing key or password is stored in this public repository.

The unsigned build artifact must be signed before installation. Use the same private signing key for every update. The local `build-apk.sh` path can build a signed release when a local SDK is available.

## Verification performed

`node tests/core.test.cjs` passes:

- All embedded JavaScript parses.
- Indian/international phone normalization and invalid input rejection.
- Exactly-at-expiry and before-expiry states, scheduled status, countdown arithmetic.
- JSON backup round-trip, duplicate IDs, malformed expiry, invalid number and amount rejection.
- Received-payment totals, year boundaries, schema 1 migration, deletion corrections and payment-history retention.
- Active renewal extends expiry while keeping the current plan active; expired renewal starts now.

Android release compilation succeeded in GitHub Actions run 34832720514 (commit 6730dbf60ab2858b22fd1f3cc6671376a12f2aa0). The APK was signed privately and verified with Android apksigner (v2 and v3 signatures). Its packaged HTML matches the source and compiled DEX is present.

Not yet device-tested: installation, real-screen layout, Android WebView bridge, system file picker, notification delivery and WhatsApp intents.

## Device acceptance checks after building

1. Install APK; add a 30-day subscription; close and reopen to confirm persistence.
2. Edit the start date into the past to confirm red expiry; renew and confirm active countdown.
3. Save PhonePe settings; open WhatsApp reminder and confirm recipient/message before sending.
4. Export a backup; change a record; restore the backup and verify customer details and settings.
5. Enable phone notifications and allow Android permission; confirm alerts for a due plan.
6. Check layout with your system font size, keyboard open, and on narrow screens.
