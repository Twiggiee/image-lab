#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 tools/fetch_runtime.py
./build-runner.sh
mkdir -p apk/assets/licenses apk/jniLibs/arm64-v8a
cp build/dex/classes.dex apk/assets/runtime.dex
cp runtime/native/*.so apk/jniLibs/arm64-v8a/
cp LICENSE NOTICE.md reference/LiteRT-LICENSE apk/assets/licenses/
python3 tools/package_bootstrap.py
bash tools/android-build-base.sh apk
python3 tools/package_native.py
apksigner sign --ks "$HOME/.buddy/keys/apps.jks" --ks-key-alias apps --ks-pass "file:$HOME/.buddy/keys/password" --out apk/build/app.apk apk/build/unsigned-with-native.apk
apksigner verify apk/build/app.apk
python3 - <<'PY'
from pathlib import Path
import hashlib,json,zipfile
p=Path('apk/build/app.apk');z=zipfile.ZipFile(p)
assert 'assets/runtime.dex' in z.namelist()
for n in ['libLiteRt.so','liblitert_jni.so','libLiteRtClGlAccelerator.so']:assert 'lib/arm64-v8a/'+n in z.namelist()
assert z.testzip() is None
r={'file':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
Path('apk/build/apk-manifest.json').write_text(json.dumps(r,indent=2));print(json.dumps(r))
PY
