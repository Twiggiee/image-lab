package com.buddyapps.imagelab;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
public final class MainActivity extends Activity {
 TextView status,logs;EditText prompt,seed;Button prepare,importModel,generate,cancel;ImageView image;String shown="";
 final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){refresh();}};
 @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView scroll=new ScrollView(this);LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);int p=dp(20);layout.setPadding(p,p,p,p);scroll.setOnApplyWindowInsetsListener((v,insets)->{layout.setPadding(p,p+insets.getSystemWindowInsetTop(),p,p+insets.getSystemWindowInsetBottom());return insets;});scroll.addView(layout);setContentView(scroll);
  TextView title=new TextView(this);title.setText("Image Lab");title.setTextSize(30);layout.addView(title);
  TextView subtitle=new TextView(this);subtitle.setText("Bonsai Image · 256×256 · on-device CPU");subtitle.setPadding(0,dp(8),0,dp(16));layout.addView(subtitle);
  status=new TextView(this);status.setTextSize(17);layout.addView(status);
  prepare=new Button(this);prepare.setText("Prepare model · 4.3 GB");layout.addView(prepare);prepare.setOnClickListener(v->start("prepare"));
  importModel=new Button(this);importModel.setText("Import local model folder");layout.addView(importModel);importModel.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);if(Build.VERSION.SDK_INT>=26)pick.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,android.net.Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FBuddy%2FBonsai-Image-256"));startActivityForResult(pick,10);});
  prompt=new EditText(this);prompt.setHint("Describe your image");prompt.setMinLines(3);prompt.setGravity(Gravity.TOP);prompt.setText("a small bonsai tree in a blue ceramic pot, studio lighting");layout.addView(prompt);
  seed=new EditText(this);seed.setHint("Seed");seed.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);seed.setText("7");layout.addView(seed);
  generate=new Button(this);generate.setText("Generate image");layout.addView(generate);generate.setOnClickListener(v->start("generate"));
  cancel=new Button(this);cancel.setText("Stop after current stage");layout.addView(cancel);cancel.setOnClickListener(v->startService(new Intent(this,InferenceService.class).setAction("cancel")));
  image=new ImageView(this);image.setAdjustViewBounds(true);layout.addView(image,new LinearLayout.LayoutParams(-1,-2));
  Button diagnostics=new Button(this);diagnostics.setText("Show diagnostic log");layout.addView(diagnostics);logs=new TextView(this);logs.setTextSize(11);logs.setTextIsSelectable(true);logs.setVisibility(View.GONE);layout.addView(logs);diagnostics.setOnClickListener(v->{logs.setVisibility(logs.getVisibility()==View.GONE?View.VISIBLE:View.GONE);refresh();});refresh();
 }
 int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 void start(String action){if(InferenceService.busy)return;Intent i=new Intent(this,InferenceService.class).setAction(action);if(action.equals("generate")){if(prompt.getText().toString().trim().isEmpty()){prompt.setError("Enter a prompt");return;}try{i.putExtra("seed",Long.parseLong(seed.getText().toString()));}catch(NumberFormatException e){seed.setError("Enter a whole-number seed");return;}i.putExtra("prompt",prompt.getText().toString());}getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);startForegroundService(i);}
 void refresh(){status.setText(InferenceService.status);boolean busy=InferenceService.busy;prepare.setEnabled(!busy);importModel.setEnabled(!busy);generate.setEnabled(!busy&&ModelInstaller.ready(this));cancel.setEnabled(busy);prompt.setEnabled(!busy);seed.setEnabled(!busy);if(!busy)getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);synchronized(InferenceService.log){logs.setText(InferenceService.log.toString());}String path=InferenceService.imagePath;if(path.isEmpty())path=getSharedPreferences("state",0).getString("image","");if(!path.isEmpty()&&!path.equals(shown)){Bitmap bitmap=BitmapFactory.decodeFile(path);if(bitmap!=null){image.setImageBitmap(bitmap);shown=path;}}if(!busy&&ModelInstaller.ready(this)&&InferenceService.status.startsWith("Prepare"))status.setText("Model ready · Bonsai Image 256×256 · CPU");}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==10&&result==RESULT_OK&&data!=null&&data.getData()!=null){android.net.Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}startForegroundService(new Intent(this,InferenceService.class).setAction("import").putExtra("tree",uri.toString()));}}
 @Override public void onStart(){super.onStart();IntentFilter f=new IntentFilter(InferenceService.ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);refresh();}
 @Override public void onStop(){unregisterReceiver(receiver);super.onStop();}
}
