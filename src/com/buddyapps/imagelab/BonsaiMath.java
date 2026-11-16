/* Adapted from Daisuke Majima's Apache-2.0 BonsaiMath.kt (2026).
 * Source: github.com/john-rocky/LiteRT-Models, bonsai_image/. See NOTICE.md. */
package com.buddyapps.imagelab;
public final class BonsaiMath {
 public static float[] sigmas(int steps,int tokens) {
  if(steps<1||tokens<1)throw new IllegalArgumentException("Invalid schedule");
  double hi=.00016927*tokens+.45666666,lo=8.73809524e-5*tokens+1.89833333;
  double a=(hi-lo)/190,mu=a*steps+hi-200*a,e=Math.exp(mu);
  float[] out=new float[steps+1];
  for(int i=0;i<steps;i++){double t=1.-i*(1.-1./steps)/Math.max(steps-1,1);out[i]=(float)(e/(e+1./t-1));}
  return out;
 }
 public static float[] noise(long seed,int tokens) {
  long[] s={seed};float[] out=new float[tokens*128];
  for(int i=0;i<out.length;i+=2){double r=Math.sqrt(-2*Math.log(uniform(s))),theta=2*Math.PI*uniform(s);out[i]=(float)(r*Math.cos(theta));if(i+1<out.length)out[i+1]=(float)(r*Math.sin(theta));}
  return out;
 }
 private static double uniform(long[] state){state[0]+=0x9E3779B97F4A7C15L;long z=state[0];z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;z=(z^(z>>>27))*0x94D049BB133111EBL;z^=z>>>31;return ((z>>>11)+1.)/9007199254740993.;}
 public static float[] imageIds(int grid){float[] out=new float[grid*grid*4];for(int h=0;h<grid;h++)for(int w=0;w<grid;w++){out[(h*grid+w)*4+1]=h;out[(h*grid+w)*4+2]=w;}return out;}
 public static float[] textIds(){float[] out=new float[256*4];for(int i=0;i<256;i++)out[i*4+3]=i;return out;}
 public static float[] unpatchify(float[] x,float[] scale,float[] shift,int grid){
  if(x.length!=grid*grid*128||scale.length!=128||shift.length!=128)throw new IllegalArgumentException("Bad latent shape");
  int side=grid*2;float[] out=new float[32*side*side];
  for(int h=0;h<grid;h++)for(int w=0;w<grid;w++)for(int c=0;c<32;c++)for(int dy=0;dy<2;dy++)for(int dx=0;dx<2;dx++){int m=c*4+dy*2+dx;out[(c*side+2*h+dy)*side+2*w+dx]=scale[m]*x[(h*grid+w)*128+m]+shift[m];}
  return out;
 }
 public static void finite(float[] x,String where){for(int i=0;i<x.length;i++)if(!Float.isFinite(x[i]))throw new IllegalArgumentException(where+" non-finite at "+i);}
}
