package com.buddyapps.imagelab;
import android.content.Context;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.function.Consumer;
import org.json.*;
final class ModelInstaller {
 static File root(Context c){return new File(c.getFilesDir(),"models/bonsai");}
 static JSONObject manifest(Context c)throws Exception{try(InputStream in=c.getAssets().open("model-manifest.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return new JSONObject(b.toString("UTF-8"));}}
 static File target(Context c,String name)throws Exception{File root=root(c).getCanonicalFile(),f=new File(root,name).getCanonicalFile();if(!f.toPath().startsWith(root.toPath()))throw new IOException("Invalid model path");return f;}
 static boolean ready(Context c){return new File(root(c),".verified.json").isFile();}
 static void checkCancel(){if(InferenceService.cancelled)throw new RuntimeException("Cancelled");}
 static String hash(File f)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))!=-1){checkCancel();md.update(b,0,n);}}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 static void validate(Context c,Consumer<String> status)throws Exception{JSONObject m=manifest(c);JSONArray files=m.getJSONArray("files");for(int i=0;i<files.length();i++){JSONObject e=files.getJSONObject(i);File f=target(c,e.getString("path"));status.accept("Checking model files "+(i+1)+"/"+files.length());if(!f.isFile()||f.length()!=e.getLong("bytes")||!hash(f).equals(e.getString("sha256")))throw new IOException("Model verification failed: "+e.getString("path"));}}
 static void prepare(Context c,Consumer<String> status)throws Exception{
  JSONObject m=manifest(c);JSONArray files=m.getJSONArray("files");String publicBase="https://huggingface.co/"+m.getString("repo")+"/resolve/"+m.getString("revision")+"/";
  String local="http://127.0.0.1:18747/models/bonsai/";
  for(int i=0;i<files.length();i++){
   checkCancel();JSONObject e=files.getJSONObject(i);String name=e.getString("path");File f=target(c,name);f.getParentFile().mkdirs();String sha=e.getString("sha256");long size=e.getLong("bytes");
   status.accept("Preparing model files "+(i+1)+"/"+files.length());
   if(f.isFile()&&f.length()==size&&hash(f).equals(sha))continue;
   File part=new File(f.getPath()+".part");Exception last=null;boolean completed=false;
   for(String base:new String[]{local,publicBase}){
    HttpURLConnection conn=null;
    try{
     long offset=part.isFile()?part.length():0;if(offset>size){if(!part.delete())throw new IOException("Cannot reset incomplete file");offset=0;}
     conn=(HttpURLConnection)new URL(base+name).openConnection();conn.setConnectTimeout(base.equals(local)?1500:30000);conn.setReadTimeout(60000);if(offset>0)conn.setRequestProperty("Range","bytes="+offset+"-");int code=conn.getResponseCode();if(code!=200&&code!=206)throw new IOException("HTTP "+code);
     if(code!=206)offset=0;long total=offset,lastUpdate=0;
     try(InputStream in=conn.getInputStream();OutputStream out=new FileOutputStream(part,offset>0)){
      byte[] buf=new byte[1024*1024];int n;while((n=in.read(buf))!=-1){checkCancel();total+=n;if(total>size)throw new IOException("Model file exceeds declared size");out.write(buf,0,n);long now=System.currentTimeMillis();if(now-lastUpdate>1000){status.accept("Preparing model files "+(i+1)+"/"+files.length()+" · "+(total*100/size)+"%");lastUpdate=now;}}
     }
     if(part.length()!=size||!hash(part).equals(sha))throw new IOException("Model file hash mismatch");
     if(f.exists()&&!f.delete())throw new IOException("Cannot replace model file");if(!part.renameTo(f))throw new IOException("Cannot finalize model file");completed=true;break;
    }catch(Exception ex){last=ex;checkCancel();}finally{if(conn!=null)conn.disconnect();}
   }
   if(!completed)throw new IOException("Unable to prepare "+name,last);
  }
  try(FileOutputStream out=new FileOutputStream(new File(root(c),".verified.json"))){out.write(m.toString(2).getBytes("UTF-8"));}
 }
}
