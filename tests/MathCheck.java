import com.buddyapps.imagelab.BonsaiMath;
public final class MathCheck {
 static void require(boolean value,String what){if(!value)throw new AssertionError(what);}
 public static void main(String[] args){
  float[] sigmas=BonsaiMath.sigmas(4,256);require(sigmas[0]==1&&sigmas[4]==0,"endpoints");float delta=0;for(int i=0;i<4;i++){require(sigmas[i]>sigmas[i+1],"descending");delta+=sigmas[i+1]-sigmas[i];}require(Math.abs(delta+1)<1e-6,"Euler sign");
  float[] noise=BonsaiMath.noise(7,256),large=BonsaiMath.noise(7,1024);for(int i=0;i<noise.length;i++)require(noise[i]==large[i],"noise stream prefix");double mean=0,square=0;for(float x:noise){mean+=x;square+=x*x;}require(Math.abs(mean/noise.length)<.04,"noise mean");require(Math.abs(square/noise.length-1)<.05,"noise variance");
  float[] ones=new float[128],shift=new float[128],lat=new float[256*128];java.util.Arrays.fill(ones,1);for(int i=0;i<lat.length;i++)lat[i]=i;float[] z=BonsaiMath.unpatchify(lat,ones,shift,16);require(z[0]==0&&z[1]==1&&z[32]==2&&z[33]==3&&z[2]==128,"patch layout");
  boolean failed=false;try{BonsaiMath.finite(new float[]{Float.NaN},"fixture");}catch(IllegalArgumentException e){failed=true;}require(failed,"NaN rejected");
  System.out.println("PASS: scheduler, deterministic noise, noise statistics, latent layout, finite-value guard");System.out.println("SIGMAS="+java.util.Arrays.toString(sigmas));System.out.println("NOISE_HEAD="+java.util.Arrays.toString(java.util.Arrays.copyOf(noise,8)));
 }
}
