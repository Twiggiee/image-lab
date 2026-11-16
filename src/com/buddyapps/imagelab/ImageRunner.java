package com.buddyapps.imagelab;
import com.google.ai.edge.litert.*;
import android.graphics.Bitmap;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.json.*;
/** Standalone Android inference harness. No app installation or Gestura binaries.
 * Model-specific host math is adapted under Apache-2.0; see NOTICE.md. */
public final class ImageRunner {
 static final String SIGNATURE="serving_default";
 static File modelDir,outputDir;
 static volatile boolean cancelled;
 static long started=System.nanoTime();
 static java.util.function.Consumer<String> progress;
 static String nativeDirectory;
 static void event(String stage,Object...pairs)throws Exception{JSONObject j=new JSONObject().put("event",stage).put("elapsed_ms",(System.nanoTime()-started)/1e6);for(int i=0;i<pairs.length;i+=2)j.put(pairs[i].toString(),pairs[i+1]);System.out.println(j);System.out.flush();if(progress!=null)progress.accept(j.toString());}
 static void admission()throws Exception{
  if(new File(outputDir,"CANCEL").exists())throw new InterruptedException("Cancelled at stage boundary");
  long available=-1;for(String line:Files.readAllLines(new File("/proc/meminfo").toPath()))if(line.startsWith("MemAvailable:"))available=Long.parseLong(line.replaceAll("[^0-9]",""));
  event("memory", "available_kib",available);
  if(available<1024*1024)throw new IllegalStateException("Less than 1 GiB available before model load; no inference started");
 }
 static final class Graph implements AutoCloseable {
  CompiledModel model;final Map<String,TensorBuffer> in=new LinkedHashMap<>(),out=new LinkedHashMap<>();final List<AutoCloseable> owned=new ArrayList<>();final String name;final int[][] shapes;final int[] outputShape;
  Graph(Environment env,File f,String backend,int[][] shapes,int[] outputShape)throws Exception{
   this.name=f.getName();this.shapes=shapes;this.outputShape=outputShape;admission();if(!f.isFile())throw new FileNotFoundException(f.toString());
   Accelerator a=Accelerator.valueOf(backend.toUpperCase(Locale.ROOT));CompiledModel.Options opt=new CompiledModel.Options(a);
   opt.setCpuOptions(new CompiledModel.CpuOptions(4,null,null));
   if(a==Accelerator.GPU)opt.setGpuOptions(new CompiledModel.GpuOptions(null,null,null,CompiledModel.GpuOptions.Precision.FP32,null,null,null,null,false,null,null,null,null,null,null));
   event("load_begin","graph",name,"requested_backend",backend,"file_bytes",f.length());long t=System.nanoTime();
   try{
    model=CompiledModel.create(f.getAbsolutePath(),opt,env);
    for(int i=0;i<shapes.length;i++){String n="args_"+i;validateType(model.getInputTensorType(n,SIGNATURE),shapes[i],name+":"+n);TensorBuffer b=model.createInputBuffer(n,SIGNATURE);in.put(n,b);owned.add(b);}
    validateType(model.getOutputTensorType("output_0",SIGNATURE),outputShape,name+":output_0");if(model.getOutputTensorType("output_0",SIGNATURE).getElementType()!=TensorType.ElementType.FLOAT)throw new IllegalArgumentException("Expected FLOAT output");TensorBuffer b=model.createOutputBuffer("output_0",SIGNATURE);out.put("output_0",b);owned.add(b);
    event("load_complete","graph",name,"load_ms",(System.nanoTime()-t)/1e6);
   }catch(Throwable e){try{close();}catch(Throwable c){e.addSuppressed(c);}throw e;}
  }
  static void validateType(TensorType type,int[] shape,String where){List<Integer> dims=type.getLayout().getDimensions();if(dims.size()!=shape.length)throw new IllegalArgumentException(where+" rank: "+dims);for(int i=0;i<shape.length;i++)if(dims.get(i)!=shape[i])throw new IllegalArgumentException(where+" shape: "+dims+" expected "+Arrays.toString(shape));}
  static int count(int[] shape){int n=1;for(int x:shape)n=Math.multiplyExact(n,x);return n;}
  float[] run(Object...args)throws Exception{
   if(args.length!=in.size())throw new IllegalArgumentException("Input count");long t=System.nanoTime();
   for(int i=0;i<args.length;i++){Object a=args[i];String n="args_"+i;int count=a instanceof int[]?((int[])a).length:((float[])a).length;if(count!=count(shapes[i]))throw new IllegalArgumentException(name+" "+n+" element count");if(model.getInputBufferRequirements(n,SIGNATURE).getBufferSize()!=Math.multiplyExact(count,4))throw new IllegalArgumentException("Buffer byte count "+n);
    String dtype=model.getInputTensorType(n,SIGNATURE).getElementType().toString();
    if(a instanceof int[]){if(!dtype.equals("INT"))throw new IllegalArgumentException(n+" expected INT32, got "+dtype);in.get(n).writeInt((int[])a);}else{if(!dtype.equals("FLOAT"))throw new IllegalArgumentException(n+" expected FLOAT32, got "+dtype);BonsaiMath.finite((float[])a,n);in.get(n).writeFloat((float[])a);}
   }
   model.run(in,out,SIGNATURE);float[] y=out.get("output_0").readFloat();if(y.length!=count(outputShape))throw new IllegalArgumentException("Output count");BonsaiMath.finite(y,name+" output");event("run_complete","graph",name,"write_run_read_ms",(System.nanoTime()-t)/1e6,"elements",y.length);return y;
  }
  public void close()throws Exception{Exception failure=null;for(int i=owned.size()-1;i>=0;i--)try{owned.get(i).close();}catch(Exception e){if(failure==null)failure=e;else failure.addSuppressed(e);}owned.clear();if(failure!=null)throw failure;if(model!=null){model.close();model=null;}event("closed","graph",name);}
 }
 static float[] array(JSONObject meta,String key)throws Exception{JSONArray a=meta.getJSONArray(key);float[] x=new float[a.length()];for(int i=0;i<x.length;i++)x[i]=(float)a.getDouble(i);return x;}
 static File artifact(String name)throws Exception{File f=new File(modelDir,name).getCanonicalFile();if(!f.toPath().startsWith(modelDir.getCanonicalFile().toPath()))throw new IllegalArgumentException("Artifact escapes package");return f;}
 static void writeImage(float[] y,int size,File output)throws Exception{
  int plane=size*size;int[] pixels=new int[plane];for(int i=0;i<plane;i++){int rgb=0xff000000;for(int c=0;c<3;c++){int v=Math.round(Math.max(0,Math.min(255,(y[c*plane+i]*.5f+.5f)*255)));rgb|=v<<(16-c*8);}pixels[i]=rgb;}
  Bitmap bitmap=Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888);File part=new File(output.getPath()+".part");try(FileOutputStream f=new FileOutputStream(part)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,f))throw new IOException("PNG failed");}finally{bitmap.recycle();}if(!part.renameTo(output))throw new IOException("Image publish failed");
 }
 static void generate(Environment env,JSONObject meta,String prompt,long seed,String[] backend)throws Exception{
  int size=256,grid=16,tokens=256;JSONObject files=meta.getJSONObject("files"),variant=meta.getJSONObject("variants").getJSONObject("256");
  for(String n:new String[]{files.getString("textenc"),variant.getString("dit"),variant.getString("vae")})if(!artifact(n).isFile())throw new FileNotFoundException(n);
  QwenTokenizer tok=new QwenTokenizer(artifact("tokenizer"));int fixtureCount=tok.checkGolden(artifact("android/app/src/test/resources/tok_golden.json"));event("tokenizer_pass","cases",fixtureCount);int[][] enc=tok.prompt(prompt);event("prompt","attended_tokens",Arrays.stream(enc[1]).sum(),"seed",seed);
  float[] embeds;try(Graph te=new Graph(env,artifact(files.getString("textenc")),backend[0],new int[][]{{1,256},{1,256}},new int[]{1,256,7680})){embeds=te.run(enc[0],enc[1]);}
  float[] lat=BonsaiMath.noise(seed,tokens),sigmas=BonsaiMath.sigmas(4,tokens),imgIds=BonsaiMath.imageIds(grid),txtIds=BonsaiMath.textIds();
  try(Graph dit=new Graph(env,artifact(variant.getString("dit")),backend[1],new int[][]{{1,tokens,128},{1,256,7680},{1},{tokens,4},{256,4}},new int[]{1,tokens,128})){
   for(int step=0;step<4;step++){admission();float[] v=dit.run(lat,embeds,new float[]{sigmas[step]},imgIds,txtIds);float ds=sigmas[step+1]-sigmas[step];for(int i=0;i<lat.length;i++)lat[i]+=ds*v[i];BonsaiMath.finite(lat,"Euler update");event("step_complete","step",step+1,"sigma",sigmas[step],"delta",ds);}
  }
  float[] z=BonsaiMath.unpatchify(lat,array(meta,"latent_bn_scale"),array(meta,"latent_bn_shift"),grid);float[] y;
  try(Graph vae=new Graph(env,artifact(variant.getString("vae")),backend[2],new int[][]{{1,32,32,32}},new int[]{1,3,size,size})){y=vae.run(z);}
  writeImage(y,size,new File(outputDir,"baseline.png"));JSONObject receipt=new JSONObject().put("model","litert-community/Bonsai-Image-ternary-4B").put("size",size).put("seed",seed).put("prompt",prompt).put("requested_backends",new JSONArray(Arrays.asList(backend))).put("completed_steps",4).put("tokenizer_cases",fixtureCount).put("elapsed_ms",(System.nanoTime()-started)/1e6);
  Files.write(new File(outputDir,"receipt.json").toPath(),receipt.toString(2).getBytes("UTF-8"));event("generation_complete","output",new File(outputDir,"baseline.png").getAbsolutePath());
 }
 public static synchronized void runForApp(String models,String outputs,String lib,String prompt,long seed,java.util.function.Consumer<String> listener)throws Exception {
  progress=listener;nativeDirectory=lib;started=System.nanoTime();try {execute(new String[]{"generate",models,outputs,"CPU,CPU,CPU",prompt,Long.toString(seed)});} finally {progress=null;nativeDirectory=null;}
 }
 private static void execute(String[] args)throws Exception {
   if(args.length<3)throw new IllegalArgumentException("Usage: command modelDir outputDir [cpu,cpu,cpu] [prompt] [seed]");
   modelDir=new File(args[1]);outputDir=new File(args[2]);if(!outputDir.isDirectory()&&!outputDir.mkdirs())throw new IOException("Output directory");String lib=nativeDirectory!=null?nativeDirectory:System.getProperty("java.library.path").split(":")[0];Map<Environment.Option,String> opts=new HashMap<>();opts.put(Environment.Option.DispatchLibraryDir,lib);opts.put(Environment.Option.CompilerPluginLibraryDir,lib);
   JSONObject meta=new JSONObject(QwenTokenizer.read(artifact("pipeline_meta.json")));
   if(args[0].equals("tokenizer")){int n=new QwenTokenizer(artifact("tokenizer")).checkGolden(artifact("android/app/src/test/resources/tok_golden.json"));event("tokenizer_pass","cases",n);return;}
   try(Environment env=Environment.create(opts)){
    event("environment","device",android.os.Build.MODEL,"available_interfaces",env.getAvailableAccelerators().toString());
    if(args[0].equals("vae")){String backend=args.length>3?args[3]:"CPU";JSONObject v=meta.getJSONObject("variants").getJSONObject("256");try(Graph g=new Graph(env,artifact(v.getString("vae")),backend,new int[][]{{1,32,32,32}},new int[]{1,3,256,256})){float[] z=new float[32*32*32];float[] a=g.run(z);z[0]=1;float[] b=g.run(z);double diff=0;for(int i=0;i<a.length;i++)diff+=Math.abs(a[i]-b[i]);if(diff==0)throw new IllegalStateException("VAE output insensitive to changed input");event("vae_smoke_pass","absolute_difference",diff);writeImage(b,256,new File(outputDir,"vae-smoke.png"));}}
    else if(args[0].equals("generate")){String[] backend=(args.length>3?args[3]:"CPU,CPU,CPU").split(",");if(backend.length!=3)throw new IllegalArgumentException("Three stage backends required");generate(env,meta,args.length>4?args[4]:"a small bonsai tree in a blue ceramic pot, studio lighting",args.length>5?Long.parseLong(args[5]):7,backend);}
    else throw new IllegalArgumentException("Unknown command");
   }
 }
 public static void main(String[] args){try{started=System.nanoTime();execute(args);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
