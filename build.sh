#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
: "${KEYSTORE_FILE:?Set KEYSTORE_FILE to your private signing keystore}"
: "${KEYSTORE_PASSWORD:?Set KEYSTORE_PASSWORD}"
BT="$ANDROID_HOME/build-tools/${BUILD_TOOLS_VERSION:-34.0.0}"
JAR="$ANDROID_HOME/platforms/android-${ANDROID_API:-34}/android.jar"
rm -rf out
mkdir -p out/classes out/dex
"$BT/aapt2" compile --dir res -o out/res.zip
"$BT/aapt2" link out/res.zip -I "$JAR" --manifest AndroidManifest.xml --java out/gen -o out/base.apk
javac -source 8 -target 8 -nowarn -cp "$JAR" -d out/classes $(find src out/gen -name '*.java')
"$BT/d8" --min-api 24 --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
cp out/dex/classes.dex out/
(cd out && zip -q base.apk classes.dex)
"$BT/zipalign" -f -p 4 out/base.apk out/aligned.apk
"$BT/apksigner" sign --ks "$KEYSTORE_FILE" --ks-pass env:KEYSTORE_PASSWORD --out tv-search-handoff.apk out/aligned.apk
"$BT/apksigner" verify tv-search-handoff.apk
printf 'Built tv-search-handoff.apk\n'
