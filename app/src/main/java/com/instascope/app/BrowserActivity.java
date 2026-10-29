package com.instascope.app;
import android.app.*; import android.os.Bundle; import android.webkit.*;
/** Plain in-app browser (your own session, like Chrome): log in yourself if you want stories/full posts for accounts you follow/own. */
public class BrowserActivity extends Activity {
    WebView wv;
    @Override public void onCreate(Bundle b){ super.onCreate(b);
        wv=new WebView(this); setContentView(wv);
        WebSettings st=wv.getSettings(); st.setJavaScriptEnabled(true); st.setDomStorageEnabled(true);
        wv.setWebViewClient(new WebViewClient());
        String u=getIntent().getStringExtra("url"); if(u==null) u="https://www.instagram.com/";
        wv.loadUrl(u);
    }
    @Override public void onBackPressed(){ if(wv!=null&&wv.canGoBack()) wv.goBack(); else super.onBackPressed(); }
}
