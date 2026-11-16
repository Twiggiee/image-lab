#!/usr/bin/env python3
"""Stream a complete two-project archive into <1 GiB chunks with hashes."""
from pathlib import Path
import tarfile,gzip,hashlib,json,time
root=Path(__file__).resolve().parents[2]
out=root/'image-lab/backups';out.mkdir(exist_ok=True)
class Chunks:
 def __init__(self):self.f=None;self.count=0;self.n=0;self.hash=None;self.parts=[];self.limit=1000*1024*1024;self.total=0;self.last=time.time()
 def write(self,data):
  view=memoryview(data);length=len(view)
  while view:
   if self.f is None:
    self.path=out/f'workspace-snapshot.tar.gz.part-{self.count:03d}';self.f=self.path.open('wb');self.hash=hashlib.sha256();self.n=0;self.count+=1
   n=min(len(view),self.limit-self.n);self.f.write(view[:n]);self.hash.update(view[:n]);self.n+=n;self.total+=n;view=view[n:]
   if self.n==self.limit:self.finish()
  if time.time()-self.last>20:print('archived compressed bytes',self.total,flush=True);self.last=time.time()
  return length
 def finish(self):
  if self.f:self.f.close();self.parts.append({'file':self.path.name,'bytes':self.n,'sha256':self.hash.hexdigest()});self.f=None
 def flush(self):
  if self.f:self.f.flush()
writer=Chunks()
def keep(info):
 parts=Path(info.name).parts
 if '.git' in parts or (len(parts)>1 and parts[0]=='image-lab' and parts[1]=='backups'):return None
 return info
with gzip.GzipFile(fileobj=writer,mode='wb',compresslevel=1,mtime=0) as gz:
 with tarfile.open(fileobj=gz,mode='w|') as tar:
  for name in ['image-lab','gestura-research']:tar.add(root/name,arcname=name,filter=keep)
writer.finish()
manifest={'format':'concatenated gzip-compressed tar split into numbered parts','roots':['image-lab','gestura-research'],'excludes':['.git','image-lab/backups'],'parts':writer.parts,'restore':'Verify SHA-256 then concatenate numbered parts in order and extract with tar -xz in an empty directory.'}
(out/'snapshot-manifest.json').write_text(json.dumps(manifest,indent=2))
(out/'SHA256SUMS').write_text(''.join(p['sha256']+'  '+p['file']+'\n' for p in writer.parts))
print(json.dumps(manifest,indent=2),flush=True)
