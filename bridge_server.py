from http.server import ThreadingHTTPServer,SimpleHTTPRequestHandler
from pathlib import Path
import os
root=Path(__file__).parent
os.chdir(root)
class Handler(SimpleHTTPRequestHandler):
 def do_POST(self):
  name=Path(self.path).name
  if self.path!='/results/'+name or not name.endswith(('.json','.jsonl','.log','.png')):
   self.send_error(400);return
  n=int(self.headers.get('Content-Length','0'))
  if n<1 or n>64*1024*1024:self.send_error(413);return
  target=root/'results'/name;target.parent.mkdir(exist_ok=True)
  with target.open('wb') as f:
   while n:
    b=self.rfile.read(min(n,1024*1024))
    if not b:raise EOFError('Incomplete upload')
    f.write(b);n-=len(b)
  self.send_response(200);self.end_headers();self.wfile.write(b'OK')
ThreadingHTTPServer(('127.0.0.1',18747),Handler).serve_forever()
