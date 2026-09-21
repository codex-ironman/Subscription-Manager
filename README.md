# My Subscription Manager 1.3

An offline subscription manager for the owner, with received-payment earnings. Source code for the Android app and its matching browser version. GitHub Actions compiles an unsigned release APK; the private release key stays outside this public repository.

## Version 1.3

- ID/profile labels and ID search distinguish multiple accounts of the same app sold to one customer. Each retains its own validity, credentials and payment records.
- Expanded app label catalog for Indian/regional OTT, international streaming, music, software, games and VPN subscriptions. Other accepts any custom app; labels do not imply availability, integration or exhaustive current market coverage.
- Days/refund calculator: ₹300 / 30 days, 25 days used = ₹50 suggested refund. Uses 24-hour days and integer paise; supports dates-to-days calculation.
- Stop one ID, review the plan-period amount, calculate the suggested refund, and record the agreed amount. Refund statuses: pending, paid, waived; mistaken statuses can return to pending. Paid action records today's date. Refunds are manually tracked, not transferred.
- Stopped IDs have a separate filter and no expiry notifications. Extend validity restarts a stopped ID; previous refund history stays intact.
- My income shows monthly receipts, expenses, paid refunds and net cash income, plus all pending refunds. Pending/waived refunds do not reduce cash. Expenses must be recorded manually; this is cash tracking, not accrual profit.
- Schema 4 migrates schemas 1–3 without inventing receipts, expenses or refunds. Encrypted backups retain all ledgers. Deleting an ID preserves its financial history.
- Consistent narrow-screen cards, wrapping actions, grid sizing and scrollable dialogs. Native Android system insets are retained.
- Application ID remains `in.subtrack.app`; version code 4 / 1.3.0. The user authorized a new permanent key for the first published APK, v1.3.0. Its SHA-256 fingerprint is pinned in `signing/release-certificate.sha256`. Private recovery material is saved separately as `Subscription-Manager-Permanent-Signing-Backup-PRIVATE.zip`; never commit it. All subsequent releases must reuse that exact key. The compile workflow produces an unsigned build; the release workflow verifies and publishes only a separately signed APK.

### Verification for 1.3

Core tests cover migration, refund rounding, invalid amounts, stopped/restarted IDs, duplicate refund protection and cash totals. Browser tests exercise multiple IDs for the same customer/app, stop at day 25, ₹50 refund, supplier expense, paid refund, reload and encrypted backup, and layout at 320/360/412px. Android device installation remains untested. The first published APK was signed with the permanent RSA-3072 key and verified with APK Signature Schemes v2 and v3. The release job enforces the pinned certificate, APK checksum, package/version and successful source-build provenance.

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

The script requires the permanent keystore (default `signing/subscription-manager-release.p12`, alias `subscription-manager`, overridable with `SUBTRACK_KEYSTORE` and `SUBTRACK_KEY_ALIAS`), prompts for passwords, verifies the built APK against the pinned certificate, and builds `app/build/outputs/apk/release/app-release.apk`. Preserve `signing/subscription-manager-release.p12` and its password for future updates. Do not commit it or publish passwords. The source has no embedded signing key.

In Android Studio: open this folder, install the requested SDK, sync Gradle, then use **Build > Generate Signed App Bundle / APK > APK**. Use the original signing key for updates; do not create a replacement. The Gradle project targets Android 15 with minimum Android 8.0 and no native CPU-specific libraries.

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


## Signed releases and permanent key

`release-assets/release.json` identifies the signed APK, checksum, version and successful compile run. Updating this manifest and signed APK triggers `.github/workflows/release.yml`. Only the signed public APK is stored in Git; no signing secrets are committed. The job validates the pinned certificate and previous release certificate, checks package/version and source-build provenance, then creates a GitHub Release with a direct installable APK asset. Existing releases are never overwritten.

Future update procedure: recover the same private signing backup, increment version code, build/test, sign with the permanent key, update the release manifest and signed APK, and publish. Missing or mismatched keys must block signing/publication; never automatically generate another key.
