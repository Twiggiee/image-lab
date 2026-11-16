#!/usr/bin/env python3
"""Fetch official LiteRT 2.2.0 and Kotlin runtime dependencies for the CLI/APK."""
from pathlib import Path
import urllib.request,zipfile,json,hashlib
root=Path(__file__).resolve().parents[1];out=root/'runtime';out.mkdir(exist_ok=True)
urls={name+f'-2.2.0.aar':f'https://dl.google.com/dl/android/maven2/com/google/ai/edge/litert/{name}/2.2.0/{name}-2.2.0.aar' for name in ['litert','litert-api']}
urls['kotlin-stdlib.jar']='https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/2.2.10/kotlin-stdlib-2.2.10.jar'
records=[]
for name,url in urls.items():
 p=out/name
 if not p.is_file():
  with urllib.request.urlopen(url) as r,p.open('wb') as f:
   while b:=r.read(1024*1024):f.write(b)
 records.append({'file':name,'url':url,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 if name.endswith('.aar'):
  z=zipfile.ZipFile(p);(out/name.replace('-2.2.0.aar','.jar')).write_bytes(z.read('classes.jar'))
  for n in z.namelist():
   if n.startswith('jni/arm64-v8a/') and not n.endswith('/'):
    q=out/'native'/Path(n).name;q.parent.mkdir(exist_ok=True);q.write_bytes(z.read(n))
(root/'reference/runtime-download-manifest.json').write_text(json.dumps(records,indent=2))
print('Runtime ready')
