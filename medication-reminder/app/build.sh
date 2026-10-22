#!/bin/bash
# Build the Dose Clock APK: ../index.html with its CDN scripts and fonts swapped for the offline copies in vendor/.
# Needs the Android SDK at $SDK (build-tools 34, platform 34) and KEYSTORE / KS_PASS like toolkit/app/build.sh.
set -e
cd "$(dirname "$0")"
SDK=${SDK:-/tmp/claude-0/sdk}; BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
KS=${KEYSTORE:?set KEYSTORE to the release keystore path}; KS_PASS=${KS_PASS:?set KS_PASS}
rm -rf build assets && mkdir -p build/gen build/classes build/dex assets/vendor
cp vendor/* assets/vendor/
python3 -I - ../index.html assets/index.html <<'PY'
import re, sys
src, out = sys.argv[1], sys.argv[2]
s = open(src, encoding='utf-8').read()
fonts = """<style>
  @font-face { font-family: 'Jost'; src: url('vendor/jost.woff2') format('woff2'); font-weight: 100 900; font-display: swap; }
  @font-face { font-family: 'Vazirmatn'; src: url('vendor/vazirmatn.woff2') format('woff2'); font-weight: 100 900; font-display: swap; }
</style>
<script src="vendor/tailwind.js"></script>"""
js = """<script src="vendor/react.production.min.js"></script>
<script src="vendor/react-dom.production.min.js"></script>
<script src="vendor/htm.umd.js"></script>"""
s, n1 = re.subn(r'<!-- vendor:start.*?<!-- vendor:end -->', lambda m: fonts, s, flags=re.S)
s, n2 = re.subn(r'<!-- vendor-js:start -->.*?<!-- vendor-js:end -->', lambda m: js, s, flags=re.S)
assert n1 == 1 and n2 == 1, 'vendor markers missing in index.html'
assert 'https://' not in re.sub(r'<a [^>]*>', '', s).split('<style>')[0], 'page head still loads from the network'
open(out, 'w', encoding='utf-8').write(s)
PY
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I $JAR --manifest AndroidManifest.xml -A assets --java build/gen build/res.zip ${VERSION_CODE:+--version-code $VERSION_CODE} ${VERSION_NAME:+--version-name $VERSION_NAME}
javac -source 8 -target 8 -nowarn -cp $JAR -d build/classes $(find src build/gen -name '*.java') 2>&1 | grep -v 'warning' || true
$BT/d8 --min-api 24 --lib $JAR --output build/dex $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk && (cd build/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk
$BT/apksigner sign --ks $KS --ks-pass pass:$KS_PASS --out build/Dose-Clock.apk build/aligned.apk
$BT/apksigner verify build/Dose-Clock.apk && cp build/Dose-Clock.apk ../Dose-Clock.apk && echo "-> $(cd .. && pwd)/Dose-Clock.apk"
