package com.buddyapps.imagelab;
import com.google.ai.edge.litert.*;
import java.util.*;
public final class RuntimeProbe {
 public static void main(String[] args) throws Exception {
  System.out.println("DEVICE="+android.os.Build.MODEL+" SDK="+android.os.Build.VERSION.SDK_INT);
  Map<Environment.Option,String> options=new HashMap<>();
  options.put(Environment.Option.DispatchLibraryDir,args[0]);
  options.put(Environment.Option.CompilerPluginLibraryDir,args[0]);
  try(Environment env=Environment.create(options)) {
   System.out.println("AVAILABLE="+env.getAvailableAccelerators());
   System.out.println("PROBE_PASS");
  }
 }
}
