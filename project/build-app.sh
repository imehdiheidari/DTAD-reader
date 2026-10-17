#!/bin/bash
# Build the DTAD Project APK: toolkit/app plus the overlay in project/app (name, icon, update URL),
# under its own package so it installs next to DTAD Theory. Needs KEYSTORE and KS_PASS like toolkit/app/build.sh.
set -e
cd "$(dirname "$0")"
OUT=$(mktemp -d)/app
cp -r ../toolkit/app "$OUT"
cp -r app/. "$OUT"/
cp content.html version.json "$OUT"/assets/
sed -i 's/package="ir.mehdi.dtadreader"/package="ir.mehdi.dtadproject"/' "$OUT"/AndroidManifest.xml
mkdir -p "$OUT"/src/ir/mehdi/dtadproject
sed 's/^package ir.mehdi.dtadreader;/package ir.mehdi.dtadproject;/' "$OUT"/src/ir/mehdi/dtadreader/MainActivity.java > "$OUT"/src/ir/mehdi/dtadproject/MainActivity.java
rm -r "$OUT"/src/ir/mehdi/dtadreader
sed -i "s/android:versionCode=\"[0-9]*\" android:versionName=\"[^\"]*\"/android:versionCode=\"${VERSION_CODE:-1}\" android:versionName=\"${VERSION_NAME:-1.0}\"/" "$OUT"/AndroidManifest.xml
"$OUT"/build.sh
cp "$OUT"/build/DTAD-Reader.apk ./DTAD-Project.apk
echo "-> $(pwd)/DTAD-Project.apk"
