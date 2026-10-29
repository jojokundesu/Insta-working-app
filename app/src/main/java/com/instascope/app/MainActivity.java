package com.instascope.app;
import android.app.*; import android.content.*; import android.net.Uri; import android.os.*; import android.view.View; import android.widget.*;
import org.json.*; import java.util.*; import java.util.concurrent.*;
public class MainActivity extends Activity {
    EditText input,token; TextView status,result; ProgressBar progress; Button scan,share,openig;
    String report="", lastUser=""; ExecutorService bg=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_main);
        input=findViewById(R.id.input); token=findViewById(R.id.token); status=findViewById(R.id.status);
        result=findViewById(R.id.result); progress=findViewById(R.id.progress); scan=findViewById(R.id.scan);
        share=findViewById(R.id.share); openig=findViewById(R.id.openig);
        // accept a shared Instagram link
        if(getIntent()!=null && Intent.ACTION_SEND.equals(getIntent().getAction()))
            input.setText(String.valueOf(getIntent().getStringExtra(Intent.EXTRA_TEXT)));
        scan.setOnClickListener(v->run());
        share.setOnClickListener(v->{ Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,report); startActivity(Intent.createChooser(i,"Export report")); });
        openig.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.instagram.com/"+lastUser+"/"))));
    }
    void ui(Runnable r){ runOnUiThread(r); }
    void run(){
        String user=Engine.parseUsername(input.getText().toString());
        if(user.isEmpty()){ status.setText("⚠ Enter a valid Instagram link or @handle."); return; }
        lastUser=user; String tok=token.getText().toString().trim();
        scan.setEnabled(false); progress.setVisibility(View.VISIBLE); result.setText(""); share.setEnabled(false); openig.setEnabled(true);
        bg.execute(()->{
            StringBuilder sb=new StringBuilder(); JSONObject rep=new JSONObject();
            try{
                rep.put("username",user); rep.put("generated","InstaScope 1.0 (public data only)");
                ui(()->status.setText("① Instagram public profile…"));
                JSONObject ig;
                if(!tok.isEmpty()){ try{ ig=Engine.instagramViaHiker(user,tok,s->ui(()->status.setText(s))); }
                    catch(Exception e){ ig=Engine.instagramPublic(user); ig.put("hiker_error",String.valueOf(e.getMessage())); } }
                else ig=Engine.instagramPublic(user);
                rep.put("instagram",ig);
                sb.append("════ INSTAGRAM : @").append(user).append(" ════\n");
                Iterator<String> ks=ig.keys(); while(ks.hasNext()){ String k=ks.next(); sb.append(k).append(": ").append(ig.opt(k)).append("\n"); }
                ui(()->status.setText("② Cross-platform username scan ("+Sites.ALL.size()+" sites)…"));
                List<Engine.Hit> hits=Engine.enumerate(user,s->ui(()->status.setText(s)));
                JSONArray arr=new JSONArray(); for(Engine.Hit h:hits){ JSONObject o=new JSONObject(); o.put("site",h.site); o.put("url",h.url); o.put("summary",h.extra); arr.put(o); }
                rep.put("username_found_on",arr);
                sb.append("\n════ USERNAME FOUND ON ").append(hits.size()).append(" SITES ════\n");
                for(Engine.Hit h:hits) sb.append("• ").append(h.site).append("  ").append(h.url).append(h.extra.isEmpty()?"":"\n    "+h.extra).append("\n");
                sb.append("\nNote: same username ≠ same person. Verify each page.\n");
                report="InstaScope report for @"+user+"\n\n"+sb+"\n── JSON ──\n"+rep.toString(2);
                ui(()->{ result.setText(report); status.setText("✅ Done — "+hits.size()+" sites. Share/export below."); });
            }catch(Exception e){ ui(()->status.setText("❌ "+e)); }
            ui(()->{ progress.setVisibility(View.GONE); scan.setEnabled(true); share.setEnabled(true); });
        });
    }
}
