#!/usr/bin/env python3
from pathlib import Path
import subprocess,json,hashlib,datetime
root=Path(__file__).resolve().parents[1];repo='Twiggiee/image-lab';tag='workspace-backup-2026-11-16'
releases=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/releases']))
release=next(r for r in releases if r['tag_name']==tag);remote={a['name']:a for a in release['assets']}
m=json.loads((root/'backups/snapshot-manifest.json').read_text());checked=[]
expected=list(m['parts'])
for filename in ['snapshot-manifest.json','SHA256SUMS']:
 p=root/'backups'/filename;expected.append({'file':filename,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
p=root/'apk/build/app.apk';expected.append({'file':'app.apk','bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
for item in expected:
 asset=remote.get(item['file'])
 if not asset or asset['state']!='uploaded':raise SystemExit('Pending: '+item['file'])
 if asset['size']!=item['bytes'] or asset.get('digest')!='sha256:'+item['sha256']:raise SystemExit('Remote identity mismatch: '+item['file'])
 checked.append({'name':item['file'],'bytes':item['bytes'],'sha256':item['sha256']})
r={'repo':repo,'release_tag':tag,'release_id':release['id'],'verified_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'all_assets_verified':True,'assets':checked,'snapshot_archive_entries':len((root/'backups/filelist.txt').read_text().splitlines())}
(root/'research/remote-backup-verification.json').write_text(json.dumps(r,indent=2))
print(json.dumps(r,indent=2))
