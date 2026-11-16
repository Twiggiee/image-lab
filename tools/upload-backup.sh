#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
python3 - <<'PY'
import subprocess,json
from pathlib import Path
repo='Twiggiee/image-lab';tag='workspace-backup-2026-11-16'
releases=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/releases']))
release=next(r for r in releases if r['tag_name']==tag)
remote={a['name']:a for a in release['assets']}
m=json.loads(Path('backups/snapshot-manifest.json').read_text())
for part in m['parts']:
 name=part['file'];found=remote.get(name)
 if found and found['size']==part['bytes'] and found['state']=='uploaded':print('Already uploaded:',name,flush=True);continue
 print('Uploading:',name,flush=True)
 subprocess.run(['gh','release','upload',tag,'--repo',repo,str(Path('backups')/name)],check=True)
 print('Uploaded:',name,flush=True)
print('All workspace backup parts uploaded',flush=True)
PY
