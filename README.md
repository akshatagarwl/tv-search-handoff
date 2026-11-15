# TV Search Handoff

Small, no-root Android TV integration: Google Assistant recognizes a search, then TizenTube Cobalt displays the results and plays the selected video.

Built for a Sony KD-55X75K / BRAVIA VU2 running Android 11. Experimental and firmware-specific, not a universal Assistant replacement.

## Why

Google Assistant's YouTube result cards can target the official YouTube TV package even when it is uninstalled for the current user. Installing a replacement-package bridge can fail on TVs retaining a system YouTube package. Setting Cobalt as the handler for YouTube URLs does not necessarily change Google's package-targeted intents.

This app instead reads Google's completed search header and opens an explicit Cobalt search URL. It does **not** repair Google's video-card intents or select the same video from those cards. You choose a result inside Cobalt.

## Privacy and permissions

- No Internet, microphone, storage, notification, or background-recording permissions.
- Google Assistant still processes your speech under Google's policies. YouTube receives the search text through Cobalt. This is **not offline voice search**.
- Accessibility access is required. Only Google's TV search and Cobalt window events are requested; content is inspected only for Google's search-header ID. Queries are not persisted or logged.
- No key-event filtering capability, no `onKeyEvent`, no remote-button remapping. An older key-filtering service interfered with held-volume on the test Sony; this app deliberately cannot request that capability.
- All Google result-screen searches are redirected to YouTube, including non-video topics. Assistant actions that never show that screen are not handled.
- Depends on Google's current `com.google.android.katniss:id/super_header` UI ID. An update can break it. Failure generally leaves Google's existing results available.

## Build

JDK 17, Android platform 34 and build-tools 34.0.0; no Gradle or third-party runtime libraries.

Set `ANDROID_HOME`, `KEYSTORE_FILE`, and `KEYSTORE_PASSWORD`, then run `bash build.sh`. Use your own signing key. Generated APKs, signing material and passwords do not belong in Git.

Install `tv-search-handoff.apk` with ADB and enable **TizenTube voice search handoff** in Android's Accessibility settings. Keep Google Assistant selected. Install TizenTube Cobalt first. The personal PR-720 blocklist build (`io.gh.akshat.tizentube.cobalt`) is preferred when present; official Cobalt (`io.gh.reisxd.tizentube.cobalt`) remains the fallback. To roll back to official Cobalt without losing app data, disable the personal build in TV app settings if supported, or rebuild the handoff with the preferred target set to the official package. Do not uninstall or clear app data unless you explicitly accept losing its local data.

Do not replace an existing enabled-accessibility-services list blindly. Preserve other services if enabling via ADB.

## Verification

`python3 tests/privacy_guards.py` checks manifest/capability boundaries, not functional behavior.

On the Sony test device, completed Google global searches for `stock investment` and `relaxing piano music`, including a repeated identical query, handed off to Cobalt. Selecting Cobalt results produced active media sessions with `state=3`, speed 1.0, and no playback error. Back did not immediately reopen Google results. The final build's physical mic-to-playback path and held-volume still need a human hardware check; ADB cannot reproduce the remote's physical push-to-talk audio gate.

Interactive-window retrieval is necessary on this Sony because `getRootInActiveWindow()` intermittently returns null despite visible results. Retries are bounded to approximately ten seconds after an event. No always-on polling is used.

## Disable

Turn off **TizenTube voice search handoff** in Accessibility. Google Assistant remains selected, and Cobalt remains installed. No root, firmware changes, system YouTube replacement or signature bypass is involved.

## Related projects

- [Android/iPhone setup preview](https://github.com/akshatagarwl/tv-setup)
- [Public curated channel lists and builder](https://github.com/akshatagarwl/tv-channel-lists)
- [Permission-free TV shortcut](https://github.com/akshatagarwl/tv-youtube-shortcut)
- [TizenTube channel-filter fork](https://github.com/akshatagarwl/TizenTube/tree/feat/pr720-channel-block)

Publishing or building these projects never changes an already-running TV session. Installation and permission changes need a separately approved setup window.

## License

MIT. Not affiliated with Google, YouTube, Sony or TizenTube.
