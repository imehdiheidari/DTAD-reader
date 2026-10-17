#!/bin/bash
set -e
cd "$(dirname "$0")"
SDK=/tmp/claude-0/sdk; BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
KS=${KEYSTORE:?set KEYSTORE to the release keystore path}; KS_PASS=${KS_PASS:?set KS_PASS}
rm -rf build && mkdir -p build/gen build/classes build/dex
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $JAR --manifest AndroidManifest.xml -A assets --java build/gen build/res.zip ${VERSION_CODE:+--version-code $VERSION_CODE} ${VERSION_NAME:+--version-name $VERSION_NAME}
javac -source 8 -target 8 -nowarn -cp $JAR -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v 'warning' || true
$BT/d8 --min-api 24 --lib $JAR --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk && (cd build/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk
$BT/apksigner sign --ks $KS --ks-pass pass:$KS_PASS --out build/DTAD-Reader.apk build/aligned.apk
$BT/apksigner verify build/DTAD-Reader.apk && ls -la build/DTAD-Reader.apk
