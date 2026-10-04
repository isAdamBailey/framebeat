# FrameBeat for Android

A native Kotlin and Jetpack Compose port of FrameBeat for phones and tablets.
Not a WebView. The web app in `src/` is the behavior reference; see the repo
root's `README.md`, `AGENTS.md`, and `DESIGN.md`. The port plan is
[issue #16](https://github.com/isAdamBailey/framebeat/issues/16).

All audio is synthesized live through a low-latency `AudioTrack`. There is no
backend, persistence, network, analytics, or third-party SDK, and the manifest
asks for no permissions.

## What's here

```
android/engine/   pure JVM: types, geometry, voices, mixer, sequencer, live scheduler.
                  Unit tests run every spec/ case with no emulator.
android/app/      Compose UI, theme, session state, and the AudioTrack output
```

Application id `io.adambailey.framebeat`, `minSdk` 26, `targetSdk` at Play's
current floor (see `app/build.gradle.kts`).

## Build and test

Needs JDK 17 and the Android SDK (`local.properties` points Gradle at the SDK).

```bash
./gradlew test           # :engine and :app JVM unit tests
./gradlew lint           # Android Lint, warnings are errors
./gradlew assembleDebug  # debug APK, signed with the local debug key
```

CI runs all three on Ubuntu. Debug builds need nothing below.

## Signing for Google Play

FrameBeat uses **Play App Signing**: Google holds the key that signs what
users install, and we sign each upload with a separate **upload key**. Accept
Play App Signing (Google-generated app signing key) when creating the first
release in Play Console. If the upload key is lost or leaked, Play Console can
reset it; the app signing key is never on this machine.

### Create the upload key, once

Keep the keystore outside the repo, and back up the file and both passwords
somewhere safe, such as a password manager:

```bash
keytool -genkeypair -v -keystore ~/keys/framebeat-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Then create `android/keystore.properties`:

```properties
storeFile=/Users/you/keys/framebeat-upload.jks
storePassword=...
keyAlias=upload
keyPassword=...
```

`storeFile` may also be relative to `android/`. `keystore.properties`,
`*.jks`, and `*.keystore` are gitignored. Never commit the keystore or its
passwords, and never put them in CI.

### Build the bundle

```bash
./gradlew bundleRelease
```

The signed bundle is `app/build/outputs/bundle/release/app-release.aab`.
`bundleRelease` (and `assembleRelease`) first runs `checkReleaseBundle`, which
fails if:

- the release application id is not `io.adambailey.framebeat`. Play locks the
  id on the first upload, so a wrong one cannot be fixed afterward.
- `keystore.properties` is missing, so the bundle would be unsigned.

Check the signature with `jarsigner -verify -verbose -certs` on the `.aab`.

### Versions

- The first bundle is `versionName` 1.0.0, `versionCode` 1.
- `versionCode` must go up on **every** upload to Play, to any track, even a
  rejected or internal-only one. Play refuses a code it has already seen.
- `versionName` is what users see. It can repeat across uploads;
  `versionCode` cannot.

Both live in `defaultConfig` in `app/build.gradle.kts`. Bump them in the PR
that prepares the release.

### targetSdk

Play sets a minimum `targetSdk` for new apps and updates and raises it each
year. Before building a release, check the floor in Play Console (it warns
on upload) and raise `targetSdk` to it if needed. Do not go below it, and do
not guess.
