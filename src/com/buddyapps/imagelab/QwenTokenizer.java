/* Adapted from Daisuke Majima's Apache-2.0 QwenTokenizer.kt (2026).
 * Source: github.com/john-rocky/LiteRT-Models, bonsai_image/. See NOTICE.md. */
package com.buddyapps.imagelab;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;
import org.json.*;
public final class QwenTokenizer {
 private static final int[] SUFFIX={151645,198,151644,77091,198,151667,271,151668,271};
 private static final Pattern PRE=Pattern.compile("(?i:'s|'t|'re|'ve|'m|'ll|'d)|[^\\r\\n\\p{L}\\p{N}]?\\p{L}+|\\p{N}| ?[^\\s\\p{L}\\p{N}]+[\\r\\n]*|\\s*[\\r\\n]+|\\s+(?!\\S)|\\s+",Pattern.UNIX_LINES);
 private static final char[] BYTE_CHAR=new char[256];
 static {boolean[] present=new boolean[256];for(int i=0;i<256;i++)if((i>=33&&i<=126)||(i>=161&&i<=172)||i>=174){present[i]=true;BYTE_CHAR[i]=(char)i;}int n=0;for(int i=0;i<256;i++)if(!present[i])BYTE_CHAR[i]=(char)(256+n++);}
 private final Map<String,Integer> vocab=new HashMap<>(170000),ranks=new HashMap<>(170000);
 private final Map<String,int[]> cache=new HashMap<>();
 public QwenTokenizer(File dir)throws Exception {
  JSONObject j=new JSONObject(read(new File(dir,"vocab.json")));Iterator<String> keys=j.keys();while(keys.hasNext()){String k=keys.next();vocab.put(k,j.getInt(k));}
  try(BufferedReader b=Files.newBufferedReader(new File(dir,"merges.txt").toPath(),StandardCharsets.UTF_8)){String line;while((line=b.readLine())!=null)if(!line.isEmpty()&&!line.startsWith("#"))ranks.put(line,ranks.size());}
 }
 static String read(File f)throws IOException{return new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);}
 public int[] encode(String text){List<Integer> out=new ArrayList<>();Matcher m=PRE.matcher(Normalizer.normalize(text,Normalizer.Form.NFC));while(m.find())for(int id:bpe(m.group()))out.add(id);return out.stream().mapToInt(Integer::intValue).toArray();}
 private int[] bpe(String text){
  int[] found=cache.get(text);if(found!=null)return found;
  List<String> w=new ArrayList<>();for(byte b:text.getBytes(StandardCharsets.UTF_8))w.add(String.valueOf(BYTE_CHAR[b&255]));
  while(w.size()>1){int best=Integer.MAX_VALUE,at=-1;for(int i=0;i<w.size()-1;i++){Integer r=ranks.get(w.get(i)+" "+w.get(i+1));if(r!=null&&r<best){best=r;at=i;}}if(at<0)break;String a=w.get(at),b=w.get(at+1);List<String> merged=new ArrayList<>();for(int i=0;i<w.size();){if(i+1<w.size()&&w.get(i).equals(a)&&w.get(i+1).equals(b)){merged.add(a+b);i+=2;}else merged.add(w.get(i++));}w=merged;}
  int[] ids=new int[w.size()];for(int i=0;i<w.size();i++){Integer v=vocab.get(w.get(i));if(v==null)throw new IllegalArgumentException("Unknown BPE symbol");ids[i]=v;}cache.put(text,ids);return ids;
 }
 public int[][] prompt(String text){int[] body=encode("user\n"+text);int len=Math.min(body.length,256-1-SUFFIX.length);int[] ids=new int[256],mask=new int[256];Arrays.fill(ids,151643);ids[0]=151644;System.arraycopy(body,0,ids,1,len);System.arraycopy(SUFFIX,0,ids,1+len,SUFFIX.length);Arrays.fill(mask,0,1+len+SUFFIX.length,1);return new int[][]{ids,mask};}
 public int checkGolden(File f)throws Exception{JSONArray cases=new JSONObject(read(f)).getJSONArray("cases");for(int i=0;i<cases.length();i++){JSONObject c=cases.getJSONObject(i);int[] actual=encode("user\n"+c.getString("prompt"));JSONArray expected=c.getJSONArray("body_ids");if(actual.length!=expected.length())throw new IllegalStateException("Tokenizer fixture length "+i);for(int j=0;j<actual.length;j++)if(actual[j]!=expected.getInt(j))throw new IllegalStateException("Tokenizer fixture mismatch "+i+":"+j);}return cases.length();}
}
