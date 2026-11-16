#!/usr/bin/env python3
from pathlib import Path
import zipfile
root=Path(__file__).resolve().parents[1];base=root/'apk/build/aligned.apk';out=root/'apk/build/unsigned-with-native.apk'
with zipfile.ZipFile(base) as zin,zipfile.ZipFile(out,'w') as zout:
 for i in zin.infolist():zout.writestr(i,zin.read(i.filename))
 for f in sorted((root/'apk/jniLibs/arm64-v8a').glob('*.so')):zout.write(f,'lib/arm64-v8a/'+f.name,compress_type=zipfile.ZIP_DEFLATED)
print(out)
