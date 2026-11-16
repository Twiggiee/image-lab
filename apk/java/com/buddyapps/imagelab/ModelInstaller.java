package com.buddyapps.imagelab;
import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.database.Cursor;
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
   if(bundled(c,e,f))continue;
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
   if(!completed)throw new IOException("Network unavailable. Import a local model folder or retry the download.",last);
  }
  try(FileOutputStream out=new FileOutputStream(new File(root(c),".verified.json"))){out.write(m.toString(2).getBytes("UTF-8"));}
 }
 static boolean bundled(Context c,JSONObject entry,File target)throws Exception {
  InputStream input;try{input=c.getAssets().open("bootstrap/"+entry.getString("path"));}catch(FileNotFoundException missing){return false;}
  try(InputStream in=input){copy(c,in,target,entry,ignored->{});}return true;
 }
 static void copy(Context c,InputStream input,File target,JSONObject entry,Consumer<String> status)throws Exception {
  target.getParentFile().mkdirs();File temp=new File(target.getPath()+".import");long expected=entry.getLong("bytes"),total=0,last=0;
  try(OutputStream out=new FileOutputStream(temp)){byte[] b=new byte[1024*1024];int n;while((n=input.read(b))!=-1){checkCancel();total+=n;if(total>expected)throw new IOException("Imported file is larger than expected");out.write(b,0,n);long now=System.currentTimeMillis();if(now-last>1000){status.accept("Importing model files · "+(total*100/expected)+"%");last=now;}}}
  if(total!=expected||!hash(temp).equals(entry.getString("sha256")))throw new IOException("Imported model file did not match the verified package: "+entry.getString("path"));
  if(target.exists()&&!target.delete())throw new IOException("Cannot replace model file");if(!temp.renameTo(target))throw new IOException("Cannot finalize imported model");
 }
 static Uri find(Context c,Uri tree,String relative)throws Exception {
  String documentId=DocumentsContract.getTreeDocumentId(tree);
  for(String name:relative.split("/")){
   Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,documentId);String next=null;
   try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){
    if(cursor!=null)while(cursor.moveToNext())if(name.equals(cursor.getString(0))){next=cursor.getString(1);break;}
   }
   if(next==null)throw new FileNotFoundException("Choose the Bonsai-Image-256 folder containing the model files");documentId=next;
  }
  return DocumentsContract.buildDocumentUriUsingTree(tree,documentId);
 }
 static void importFolder(Context c,Uri tree,Consumer<String> status)throws Exception {
  JSONObject m=manifest(c);JSONArray entries=m.getJSONArray("files");
  for(int i=0;i<entries.length();i++){
   checkCancel();JSONObject e=entries.getJSONObject(i);File target=target(c,e.getString("path"));status.accept("Checking import "+(i+1)+"/"+entries.length());
   if(target.isFile()&&target.length()==e.getLong("bytes")&&hash(target).equals(e.getString("sha256")))continue;
   if(bundled(c,e,target))continue;
   try(InputStream input=c.getContentResolver().openInputStream(find(c,tree,e.getString("path")))){if(input==null)throw new IOException("Cannot open imported model");copy(c,input,target,e,status);}
  }
  try(OutputStream out=new FileOutputStream(new File(root(c),".verified.json"))){out.write(m.toString(2).getBytes("UTF-8"));}
 }

}
