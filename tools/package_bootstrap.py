"""Package the small, hash-pinned tokenizer/fixture assets without model weights."""
from pathlib import Path
import json,hashlib,urllib.request
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'apk/assets/model-manifest.json').read_text())
base=f"https://huggingface.co/{manifest['repo']}/resolve/{manifest['revision']}/"
for entry in manifest['files']:
 if entry['bytes']>=10*1024*1024:continue
 target=root/'apk/assets/bootstrap'/entry['path'];source=root/'models/bonsai'/entry['path']
 target.parent.mkdir(parents=True,exist_ok=True)
 if source.is_file():data=source.read_bytes()
 elif target.is_file():data=target.read_bytes()
 else:data=urllib.request.urlopen(base+entry['path']).read()
 if len(data)!=entry['bytes'] or hashlib.sha256(data).hexdigest()!=entry['sha256']:raise RuntimeError('Bootstrap asset does not match pinned identity: '+entry['path'])
 target.write_bytes(data)
