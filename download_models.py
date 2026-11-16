from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import urllib.request,json,hashlib,time
root=Path(__file__).parent
repo='litert-community/Bonsai-Image-ternary-4B'
rev=json.loads((root/'reference/bonsai-model-info.json').read_text())['sha']
entries=json.load(urllib.request.urlopen(f'https://huggingface.co/api/models/{repo}/tree/{rev}?recursive=true&limit=1000'))
keep={'dit_256_int4b32.tflite','textenc_int4.tflite','vae_dec_256_fp32.tflite','pipeline_meta.json','tokenizer/vocab.json','tokenizer/merges.txt','tokenizer/tokenizer_config.json','tokenizer/chat_template.jinja','android/app/src/test/resources/tok_golden.json'}
selected=[x for x in entries if x['path'] in keep]
assert len(selected)==len(keep),(len(selected),keep-{x['path'] for x in selected})
(root/'reference/bonsai-download-manifest.json').write_text(json.dumps({'repo':repo,'revision':rev,'files':selected},indent=2))
def get(e):
 name=e['path'];dst=root/'models/bonsai'/name;dst.parent.mkdir(parents=True,exist_ok=True)
 sha=e.get('lfs',{}).get('oid');part=dst.with_name(dst.name+'.part')
 def valid(p):
  if not p.exists() or p.stat().st_size!=e['size']:return False
  if not sha:return True
  h=hashlib.sha256()
  with p.open('rb') as f:
   while b:=f.read(4*1024*1024):h.update(b)
  return h.hexdigest()==sha
 if valid(dst):print('cached',name,flush=True);return
 offset=part.stat().st_size if part.exists() else 0
 req=urllib.request.Request(f'https://huggingface.co/{repo}/resolve/{rev}/{name}',headers={'Range':f'bytes={offset}-'} if offset else {})
 with urllib.request.urlopen(req,timeout=120) as r:
  if offset and r.status!=206:offset=0
  with part.open('ab' if offset else 'wb') as f:
   n=offset;t=time.time()
   while b:=r.read(4*1024*1024):
    f.write(b);n+=len(b)
    if time.time()-t>20:print('progress',name,n,e['size'],flush=True);t=time.time()
 if not valid(part):raise RuntimeError('size/hash mismatch '+name)
 part.rename(dst);print('verified',name,e['size'],flush=True)
with ThreadPoolExecutor(max_workers=3) as ex:list(ex.map(get,selected))
print('ALL_MODELS_VERIFIED',flush=True)
