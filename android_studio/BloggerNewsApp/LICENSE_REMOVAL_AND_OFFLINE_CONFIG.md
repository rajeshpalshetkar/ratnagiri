# License Verification Removal & Offline Configuration

## Changes made
- Removed the startup Access Key / Application ID verification gate.
- The app no longer exits because `Config.ACCESS_KEY` is blank or invalid.
- Added optional `Config.REMOTE_CONFIG_SOURCE` (Google Drive file ID or JSON URL).
- Stores the last valid remote configuration in private Android SharedPreferences.
- If Google Drive/hosting is unavailable, the app uses the last saved valid configuration.
- If no configuration has ever been downloaded, the app still opens instead of showing a license/configuration blocking dialog.
- Cached startup skips the online Blogger Labels request so it does not wait for the network.

## Required before first real use
Open:
`app/src/main/java/com/app/bloggernewsapp400/Config.java`

The supplied Google Drive file ID has now been placed in `REMOTE_CONFIG_SOURCE`:
`14NDMQqy10bQvUAmu5eocVEpgNdQNmmt1`

The share URL could not be opened from this environment, so public-read access and the actual JSON contents could not be independently verified. Ensure the file is shared as "Anyone with the link - Viewer" (or otherwise accessible to the app's unauthenticated request).

The ZIP originally contained only a sample `config.json` with a placeholder Blogger ID/API key. The API key in the message supplied with the audit was masked, and the live Google Drive file ID/link was not supplied. Therefore a clean install cannot load the actual Blogger feed until the real configuration source is entered.

The Google Drive JSON should be readable by the app (typically "Anyone with the link - Viewer"). Do not put private server secrets or license secrets in this JSON.

## Offline behavior
- First successful online configuration fetch is saved locally.
- Later starts can use that saved configuration when the remote file or hosting is down.
- Previously downloaded category labels remain in the app's local database.
- News content itself is not made fully offline by this change; only the configuration is cached. Full offline news reading requires a separate post/content cache implementation.
- On a brand-new install with no internet and no prior config, the app can open but cannot fetch Blogger news until a valid config has been loaded.

## Build verification
A Gradle build was attempted, but the environment could not download Gradle 8.13 because network DNS access to `services.gradle.org` was unavailable. The modified source therefore needs an Android Studio build on a machine with Gradle dependencies available before APK release.


## Build status
`./gradlew :app:assembleDebug` was attempted. It stopped before compilation because this environment could not resolve `services.gradle.org` to download Gradle 8.13 (`UnknownHostException`). This is an environment/network limitation, not a confirmed Java compilation result.
