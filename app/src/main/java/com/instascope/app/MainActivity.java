package com.instascope.app;
import android.app.*; import android.content.*; import android.graphics.*; import android.net.Uri; import android.os.*; import android.view.*; import android.widget.*;
import java.io.*; import java.net.*;
import org.json.*; import java.util.*; import java.util.concurrent.*;
public class MainActivity extends Activity {
    EditText input; TextView status,result,postsLabel; ProgressBar progress; Button scan,share,openig; ImageView profilePic; GridLayout postsGrid;
    String report="", lastUser=""; ExecutorService bg=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main);
        input=findViewById(R.id.input); status=findViewById(R.id.status);
        result=findViewById(R.id.result); progress=findViewById(R.id.progress); scan=findViewById(R.id.scan);
        share=findViewById(R.id.share); openig=findViewById(R.id.openig); profilePic=findViewById(R.id.profilePic); postsLabel=findViewById(R.id.postsLabel); postsGrid=findViewById(R.id.postsGrid);
        // accept a shared Instagram link
        if(getIntent()!=null && Intent.ACTION_SEND.equals(getIntent().getAction()))
            input.setText(String.valueOf(getIntent().getStringExtra(Intent.EXTRA_TEXT)));
        scan.setOnClickListener(v->run());
        share.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,report); startActivity(Intent.createChooser(i,"Export report")); });
        openig.setOnClickListener(v->{ Intent i=new Intent(this,BrowserActivity.class); i.putExtra("url","https://www.instagram.com/"+lastUser+"/"); startActivity(i); });
    }
    void ui(Runnable r){ runOnUiThread(r); }
    static Bitmap loadImg(String url){ try{ HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestProperty("User-Agent",Engine.UA); c.setConnectTimeout(10000); c.setReadTimeout(10000);
        InputStream in=c.getInputStream(); Bitmap b=BitmapFactory.decodeStream(in); in.close(); return b; }catch(Exception e){ return null; } }
    void renderImages(JSONObject ig){
        String pic=ig.optString("profile_pic_hd_link","");
        JSONArray posts=ig.optJSONArray("post_image_urls");
        if(!pic.isEmpty()) new Thread(()->{ Bitmap b=loadImg(pic); if(b!=null) ui(()->{ profilePic.setImageBitmap(b); profilePic.setVisibility(View.VISIBLE); }); }).start();
        if(posts!=null&&posts.length()>0) ui(()->{ postsLabel.setText("Posts ("+posts.length()+") — tap the in-app browser button for stories / full view"); postsLabel.setVisibility(View.VISIBLE); postsGrid.removeAllViews(); });
        if(posts!=null) for(int i=0;i<Math.min(posts.length(),12);i++){ final String u=posts.optString(i); if(u.isEmpty()) continue;
            new Thread(()->{ Bitmap b=loadImg(u); if(b==null) return; ui(()->{ ImageView iv=new ImageView(this);
                int w=getResources().getDisplayMetrics().widthPixels/3-16; iv.setLayoutParams(new GridLayout.LayoutParams(new ViewGroup.LayoutParams(w,w)));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP); iv.setPadding(3,3,3,3); iv.setImageBitmap(b); postsGrid.addView(iv); }); }).start(); }
    }
    void run(){
        String user=Engine.parseUsername(input.getText().toString());
        if(user.isEmpty()){ status.setText("⚠ Enter a valid Instagram link or @handle."); return; }
        lastUser=user;        scan.setEnabled(false); progress.setVisibility(View.VISIBLE); result.setText(""); share.setEnabled(false); openig.setEnabled(true);
        bg.execute(()->{
            StringBuilder sb=new StringBuilder(); JSONObject rep=new JSONObject();
            try{
                rep.put("username",user); rep.put("generated","InstaScope 1.0 (public data only)");
                ui(()->status.setText("① Instagram public profile…"));
                JSONObject ig=Engine.instagramPublic(user);
                rep.put("instagram",ig); renderImages(ig);
                sb.append("════ INSTAGRAM : @").append(user).append(" ════\n");
                Iterator<String> ks=ig.keys(); while(ks.hasNext()){ String k=ks.next(); sb.append(k).append(": ").append(ig.opt(k)).append("\n"); }
                ui(()->status.setText("② Cross-platform username scan ("+Sites.ALL.size()+" sites)…"));
                List<Engine.Hit> hits=Engine.enumerate(user,s->ui(()->status.setText(s)));
                JSONArray arr=new JSONArray(); for(Engine.Hit h:hits){ JSONObject o=new JSONObject(); o.put("site",h.site); o.put("url",h.url); o.put("summary",h.extra); arr.put(o); }
                rep.put("username_found_on",arr);
                sb.append("\n════ USERNAME FOUND ON ").append(hits.size()).append(" SITES ════\n");
                for(Engine.Hit h:hits) sb.append("• ").append(h.site).append("  ").append(h.url).append(h.extra.isEmpty()?"":"\n    "+h.extra).append("\n");
                               report="InstaScope report for @"+user+"\n\n"+sb+"\n── JSON ──\n"+rep.toString(2);
                ui(()->{ result.setText(report); status.setText("✅ Done — "+hits.size()+" sites. Share/export below."); });
            }catch(Exception e){ ui(()->status.setText("❌ "+e)); }
            ui(()->{ progress.setVisibility(View.GONE); scan.setEnabled(true); share.setEnabled(true); });
        });
    }
}
