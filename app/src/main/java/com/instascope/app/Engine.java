package com.instascope.app;
import org.json.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets;
import java.util.*; import java.util.concurrent.*; import java.util.regex.*;
/** Public-data OSINT engine. Deliberately excludes: private contact harvesting,
 *  exact GPS/street pins, credential login / anti-bot bypass. Only data a browser
 *  (or the user's own licensed HikerAPI token) publicly returns is parsed. */
public class Engine {
    public interface Cb { void status(String s); }
    static final String UA="Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36";

    public static String parseUsername(String in){
        if(in==null) return "";
        in=in.trim();
        in=in.replaceFirst("(?i)^https?://(www\\.)?instagram\\.com/","").replaceFirst("(?i)^instagram\\.com/","");
        in=in.split("[/?#]")[0].replace("@","");
        return in.matches("[A-Za-z0-9._]{1,30}")?in:"";
    }
    static String http(String url, Map<String,String> hdr) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setRequestProperty("User-Agent",UA);
        c.setRequestProperty("Accept-Language","en-US,en;q=0.9");
        if(hdr!=null) hdr.forEach(c::setRequestProperty);
        c.setConnectTimeout(12000); c.setReadTimeout(12000); c.setInstanceFollowRedirects(true);
        int code=c.getResponseCode();
        InputStream is=code>=400?c.getErrorStream():c.getInputStream();
        if(is==null) return "";
        ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[8192]; int n,total=0;
        while((n=is.read(buf))>0){ b.write(buf,0,n); total+=n; if(total>600_000) break; }
        return b.toString(StandardCharsets.UTF_8.name());
    }
    static String meta(String html,String prop){
        Matcher m=Pattern.compile("(?i)<meta[^>]+(?:property|name)=\""+Pattern.quote(prop)+"\"[^>]+content=\"([^\"]*)\"").matcher(html);
        if(m.find()) return decode(m.group(1));
        m=Pattern.compile("(?i)<meta[^>]+content=\"([^\"]*)\"[^>]+(?:property|name)=\""+Pattern.quote(prop)+"\"").matcher(html);
        return m.find()?decode(m.group(1)):"";
    }
    static String decode(String s){ return s.replace("&amp;","&").replace("&quot;","\"").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">"); }

    public static JSONObject instagramPublic(String user) throws Exception {
        JSONObject o=new JSONObject(); o.put("username",user);
        String html=http("https://www.instagram.com/"+user+"/",null);
        String desc=meta(html,"og:description"), title=meta(html,"og:title"), img=meta(html,"og:image");
        o.put("display_title",title); o.put("public_summary",desc); o.put("profile_pic_hd_link",img);
        Matcher f=Pattern.compile("([\\d,.]+[KMB]?) Followers, ([\\d,.]+[KMB]?) Following, ([\\d,.]+[KMB]?) Posts").matcher(desc);
        if(f.find()){ o.put("followers",f.group(1)); o.put("following",f.group(2)); o.put("posts",f.group(3)); }
        if(desc.isEmpty()) o.put("note","Instagram served a login/consent wall to this device/IP (common without a session). Add your own HikerAPI token below for the API path, or retry on another network.");
        return o;
    }
    public static JSONObject instagramViaHiker(String user,String token,Cb cb) throws Exception {
        Map<String,String> h=new HashMap<>(); h.put("x-access-key",token); h.put("accept","application/json");
        String base="https://api.hikerapi.com/v1";
        JSONObject root=new JSONObject(http(base+"/user/by/username?username="+URLEncoder.encode(user,"UTF-8"),h));
        JSONObject u=root.optJSONObject("user"); if(u==null) u=root;
        JSONObject o=new JSONObject();
        // public profile fields only — public_email/public_phone_number are intentionally NOT read
        for(String k:new String[]{"pk","username","full_name","biography","is_business","is_private","is_verified","category","external_url","follower_count","following_count","media_count"})
            if(u.has(k)) o.put(k,u.get(k));
        JSONObject hd=u.optJSONObject("hd_profile_pic_url_info"); if(hd!=null) o.put("profile_pic_hd_link",hd.optString("url"));
        else if(u.has("profile_pic_url_hd")) o.put("profile_pic_hd_link",u.get("profile_pic_url_hd"));
        long pk=u.optLong("pk",0);
        if(pk>0 && cb!=null){ cb.status("Fetching recent posts for hashtag / displayed-location analysis…");
            try{
                JSONObject med=new JSONObject(http(base+"/user/medias?user_id="+pk,h));
                JSONArray items=med.optJSONArray("items"); if(items==null && med.has("response")) items=med.getJSONObject("response").optJSONArray("items");
                Map<String,Integer> tags=new TreeMap<>(); JSONArray locs=new JSONArray();
                if(items!=null) for(int i=0;i<Math.min(items.length(),50);i++){
                    JSONObject it=items.getJSONObject(i);
                    JSONObject cap=it.optJSONObject("caption"); String text=cap!=null?cap.optString("text"):"";
                    Matcher mm=Pattern.compile("#(\\w+)").matcher(text);
                    while(mm.find()) tags.merge(mm.group(1).toLowerCase(),1,Integer::sum);
                    JSONObject loc=it.optJSONObject("location"); // name as displayed on the post only; lat/lng/address dropped on purpose
                    if(loc!=null && loc.has("name")) locs.put(loc.optString("name"));
                }
                o.put("hashtags",new JSONObject(tags)); o.put("displayed_location_names",locs);
            }catch(Exception ignored){ o.put("posts_note","media endpoint unavailable for this token/plan"); }
        }
        return o;
    }
    public static class Hit { public String site,url,extra=""; }
    public static List<Hit> enumerate(String user,Cb cb) throws Exception {
        List<Hit> hits=Collections.synchronizedList(new ArrayList<>());
        ExecutorService ex=Executors.newFixedThreadPool(16); List<Future<?>> fs=new ArrayList<>();
        final int[] done={0};
        for(Sites.Site st: Sites.ALL) fs.add(ex.submit(()->{
            String url=st.tpl.replace("{}",user); String finalUrl=url;
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setRequestProperty("User-Agent",UA); c.setConnectTimeout(9000); c.setReadTimeout(9000); c.setInstanceFollowRedirects(true);
                int code=c.getResponseCode(); boolean found;
                if("message".equals(st.mode)){
                    InputStream is=code>=400?c.getErrorStream():c.getInputStream(); String body="";
                    if(is!=null){ ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] bf=new byte[8192]; int n,t=0;
                        while((n=is.read(bf))>0){b.write(bf,0,n);t+=n; if(t>200_000)break;} body=b.toString("UTF-8"); }
                    found=code==200 && (st.err==null||!body.toLowerCase().contains(st.err.toLowerCase()));
                    if(found){ Hit hit=new Hit(); hit.site=st.name; hit.url=url;
                        String t=meta(body,"og:title"), d=meta(body,"og:description");
                        hit.extra=(t.isEmpty()?"":t)+(d.isEmpty()?"":" — "+(d.length()>180?d.substring(0,180)+"…":d)); hits.add(hit); }
                } else {
                    found=(code==200);
                    if(found){ Hit hit=new Hit(); hit.site=st.name; hit.url=c.getURL().toString();
                        try{ String body=http(hit.url,null); String t=meta(body,"og:title"), d=meta(body,"og:description");
                            hit.extra=(t.isEmpty()?"":t)+(d.isEmpty()?"":" — "+(d.length()>180?d.substring(0,180)+"…":d)); }catch(Exception ignored){}
                        hits.add(hit); }
                }
            }catch(Exception ignored){}
            synchronized(done){ done[0]++; if(cb!=null&&done[0]%10==0) cb.status("Username scan: "+done[0]+"/"+Sites.ALL.size()+" sites checked, "+hits.size()+" found…"); }
        }));
        for(Future<?> f:fs) f.get(); ex.shutdown();
        hits.sort(Comparator.comparing(a->a.site.toLowerCase())); return hits;
    }
}
