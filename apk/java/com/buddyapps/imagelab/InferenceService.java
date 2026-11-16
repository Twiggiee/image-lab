package com.buddyapps.imagelab;
import android.app.*;
import android.content.*;
import android.os.*;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.*;
public final class InferenceService extends Service {
 static final String ACTION="com.buddyapps.imagelab.STATE",CHANNEL="generation";
 static volatile boolean busy,cancelled;
 static volatile String status="Prepare the model to begin.",imagePath="",jobPath="";
 static volatile StringBuilder log=new StringBuilder();
 static final ExecutorService worker=Executors.newSingleThreadExecutor();
 PowerManager.WakeLock wake;
 void update(String text){status=text;sendBroadcast(new Intent(ACTION).setPackage(getPackageName()));}
 void note(String line){synchronized(log){if(log.length()>100000)log.delete(0,50000);log.append(line).append('\n');}sendBroadcast(new Intent(ACTION).setPackage(getPackageName()));}
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent==null)return START_NOT_STICKY;
  if("cancel".equals(intent.getAction())){cancelled=true;if(!jobPath.isEmpty())try{new File(jobPath,"CANCEL").createNewFile();}catch(IOException ignored){}update("Stopping after the current stage…");return START_NOT_STICKY;}
  if(busy)return START_NOT_STICKY;busy=true;cancelled=false;log=new StringBuilder();String action=intent.getAction(),prompt=intent.getStringExtra("prompt");long seed=intent.getLongExtra("seed",7);
  NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel(CHANNEL,"Image generation",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  startForeground(1,new Notification.Builder(this,CHANNEL).setContentTitle("Image Lab").setContentText("Processing on this phone").setSmallIcon(android.R.drawable.ic_menu_gallery).setContentIntent(open).setOngoing(true).build());
  wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ImageLab:inference");wake.acquire(60*60*1000L);
  worker.execute(()->{try{
   if("prepare".equals(action)){update("Preparing model files…");ModelInstaller.prepare(this,this::update);update("Model ready · Bonsai Image 256×256 · CPU");}
   else if("generate".equals(action))generate(prompt,seed);
   else throw new IllegalArgumentException("Unknown action");
  }catch(Throwable ex){Throwable e=ex instanceof InvocationTargetException?((InvocationTargetException)ex).getTargetException():ex;StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));note(sw.toString());update(cancelled?"Stopped.":"Could not complete: "+e.getMessage());}
  finally{busy=false;if(wake!=null&&wake.isHeld())wake.release();sendBroadcast(new Intent(ACTION).setPackage(getPackageName()));stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();}});
  return START_NOT_STICKY;
 }
 void generate(String prompt,long seed)throws Exception{
  if(prompt==null||prompt.trim().isEmpty())throw new IllegalArgumentException("Enter a prompt");
  update("Verifying model files…");ModelInstaller.validate(this,this::update);
  File job=new File(getFilesDir(),"outputs/"+UUID.randomUUID());if(!job.mkdirs())throw new IOException("Cannot create output directory");jobPath=job.getAbsolutePath();
  PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
  JSONObject conditions=new JSONObject().put("model",Build.MODEL).put("sdk",Build.VERSION.SDK_INT).put("interactive",pm.isInteractive()).put("device_idle",pm.isDeviceIdleMode()).put("power_save",pm.isPowerSaveMode()).put("wake_lock_held",wake.isHeld()).put("battery_level",battery!=null?battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1):-1).put("plugged",battery!=null?battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,0):0).put("run_kind","APK functional check; uncontrolled performance conditions");
  if(Build.VERSION.SDK_INT>=29)conditions.put("thermal_status",pm.getCurrentThermalStatus());
  try(FileOutputStream out=new FileOutputStream(new File(job,"conditions.json"))){out.write(conditions.toString(2).getBytes("UTF-8"));}
  File dex=new File(getCodeCacheDir(),"runtime.dex");
  // Asset is code shipped inside this signed APK, never downloaded executable code.
  if(dex.exists()&&!dex.delete())throw new IOException("Cannot refresh packaged runtime");
  try(InputStream in=getAssets().open("runtime.dex");FileOutputStream out=new FileOutputStream(dex)){
   if(!dex.setReadOnly())throw new IOException("Cannot protect packaged code");byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
  }
  String lib=getApplicationInfo().nativeLibraryDir;DexClassLoader loader=new DexClassLoader(dex.getAbsolutePath(),getCodeCacheDir().getAbsolutePath(),lib,getClassLoader());
  Class<?> runner=loader.loadClass("com.buddyapps.imagelab.ImageRunner");Method run=runner.getMethod("runForApp",String.class,String.class,String.class,String.class,long.class,Consumer.class);
  update("Starting Bonsai Image…");
  try(FileOutputStream file=new FileOutputStream(new File(job,"events.jsonl"))){Consumer<String> listener=line->{try{file.write((line+"\n").getBytes("UTF-8"));file.flush();note(line);JSONObject e=new JSONObject(line);String event=e.optString("event");if(event.equals("load_begin")){String graph=e.optString("graph");update(graph.startsWith("textenc")?"Loading prompt encoder…":graph.startsWith("dit")?"Loading image model…":"Loading image decoder…");}else if(event.equals("step_complete"))update("Generating image · step "+e.optInt("step")+"/4");else if(event.equals("generation_complete"))update("Image complete.");}catch(Exception ex){throw new RuntimeException(ex);}};
   run.invoke(null,ModelInstaller.root(this).getAbsolutePath(),job.getAbsolutePath(),lib,prompt,seed,listener);
  }
  File image=new File(job,"baseline.png");if(!image.isFile())throw new IOException("No image produced");imagePath=image.getAbsolutePath();getSharedPreferences("state",0).edit().putString("image",imagePath).putString("job",jobPath).apply();
  JSONObject receipt=new JSONObject(new String(java.nio.file.Files.readAllBytes(new File(job,"receipt.json").toPath()),"UTF-8"));update(String.format(Locale.ROOT,"Image complete · %.1f seconds · CPU",receipt.getDouble("elapsed_ms")/1000));
 }
}
