package com.instascope.app;
import org.json.*;
import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets;
import java.util.*; import java.util.concurrent.*; import java.util.regex.*;
/** Public-data OSINT engine: token-free Instagram web profile + cross-platform username scan. */
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

    /** Token-free: uses the same web_profile_info endpoint instagram.com calls for logged-out profile views, HTML page as fallback. */
    public static JSONObject instagramPublic(String user) throws Exception {
        JSONObject o=new JSONObject(); o.put("username",user);
        try{
            Map<String,String> h=new HashMap<>();
            h.put("X-IG-App-ID","936619743392459"); h.put("Referer","https://www.instagram.com/"+user+"/");
            JSONObject root=new JSONObject(http("https://i.instagram.com/api/v1/users/web_profile_info/?username="+URLEncoder.encode(user,"UTF-8"),h));
            JSONObject u=root.getJSONObject("data").getJSONObject("user");
            o.put("source","instagram web profile");
            for(String k:new String[]{"id","full_name","biography","external_url","is_business_account","is_private","is_verified","category_name","business_category_name"})
                if(u.has(k)&&!u.isNull(k)) o.put(k.equals("id")?"pk":k,u.get(k));
            if(u.has("profile_pic_url_hd")) o.put("profile_pic_hd_link",u.get("profile_pic_url_hd"));
            else if(u.has("profile_pic_url")) o.put("profile_pic_hd_link",u.get("profile_pic_url"));
            if(u.has("edge_followed_by")) o.put("followers",u.getJSONObject("edge_followed_by").optLong("count"));
            if(u.has("edge_follow")) o.put("following",u.getJSONObject("edge_follow").optLong("count"));
            JSONObject tl=u.optJSONObject("edge_owner_to_timeline_media");
            if(tl!=null){ o.put("posts",tl.optLong("count"));
                Map<String,Integer> tags=new TreeMap<>(); JSONArray locs=new JSONArray(), recent=new JSONArray(), imgs=new JSONArray();
                JSONArray edges=tl.optJSONArray("edges");
                if(edges!=null) for(int i=0;i<Math.min(edges.length(),50);i++){
                    JSONObject node=edges.getJSONObject(i).optJSONObject("node"); if(node==null) continue;
                    JSONObject cap=node.optJSONObject("edge_media_to_caption"); String text="";
                    if(cap!=null&&cap.optJSONArray("edges")!=null&&cap.getJSONArray("edges").length()>0)
                        text=cap.getJSONArray("edges").getJSONObject(0).getJSONObject("node").optString("text");
                    Matcher mm=Pattern.compile("#(\\w+)").matcher(text);
                    while(mm.find()) tags.merge(mm.group(1).toLowerCase(),1,Integer::sum);
                    JSONObject loc=node.optJSONObject("location"); if(loc!=null&&loc.has("name")) locs.put(loc.optString("name"));
                    if(node.has("display_url")) imgs.put(node.optString("display_url"));
                    else if(node.has("thumbnail_src")) imgs.put(node.optString("thumbnail_src"));
                    JSONObject r=new JSONObject(); r.put("taken_at",node.optLong("taken_at_timestamp"));
                    if(loc!=null) r.put("location_name",loc.optString("name"));
                    r.put("likes",node.optJSONObject("edge_liked_by")!=null?node.getJSONObject("edge_liked_by").optLong("count"):0);
                    recent.put(r);
                }
                o.put("hashtags",new JSONObject(tags)); o.put("displayed_location_names",locs); o.put("recent_posts",recent); o.put("post_image_urls",imgs);
            }
            return o;
        }catch(Exception apiErr){
            String html=http("https://www.instagram.com/"+user+"/",null);
            String desc=meta(html,"og:description"), title=meta(html,"og:title"), img=meta(html,"og:image");
            o.put("source","instagram web page"); o.put("display_title",title); o.put("public_summary",desc); o.put("profile_pic_hd_link",img);
            Matcher f=Pattern.compile("([\\d,.]+[KMB]?) Followers, ([\\d,.]+[KMB]?) Following, ([\\d,.]+[KMB]?) Posts").matcher(desc);
            if(f.find()){ o.put("followers",f.group(1)); o.put("following",f.group(2)); o.put("posts",f.group(3)); }
            if(desc.isEmpty()) o.put("note","Instagram did not return profile data to this device/network right now. Retry, switch network (mobile data vs Wi-Fi), or try again later.");
            return o;
        }
    }
    public static class Hit { public String site,url,extra=""; }
    static final String[] NOT_FOUND={"user not found","page not found","profile not found","account not found","doesn't exist","does not exist","couldn't find","could not find","no longer exists","this account is private","sorry, this page isn","404 not found","content isn't available","user_not_found","not be found"};
    static boolean looksReal(String body,String finalUrl,String user,String siteErr){
        if(body==null) return false; String b=body.toLowerCase();
        if(siteErr!=null && b.contains(siteErr.toLowerCase())) return false;
        for(String nf:NOT_FOUND) if(b.contains(nf)) return false;
        // real profile pages virtually always mention the handle somewhere (title/meta/body)
        return b.contains(user.toLowerCase()) || finalUrl.toLowerCase().contains(user.toLowerCase());
    }
    public static List<Hit> enumerate(String user,Cb cb) throws Exception {
        List<Hit> hits=Collections.synchronizedList(new ArrayList<>());
        ExecutorService ex=Executors.newFixedThreadPool(16); List<Future<?>> fs=new ArrayList<>();
        final int[] done={0};
        for(Sites.Site st: Sites.ALL) fs.add(ex.submit(()->{
            String url=st.tpl.replace("{}",user);
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setRequestProperty("User-Agent",UA); c.setConnectTimeout(9000); c.setReadTimeout(9000); c.setInstanceFollowRedirects(true);
                int code=c.getResponseCode();
                InputStream is=code>=400?c.getErrorStream():c.getInputStream(); String body="";
                if(is!=null){ ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] bf=new byte[8192]; int n,t=0;
                    while((n=is.read(bf))>0){b.write(bf,0,n);t+=n; if(t>250_000)break;} body=b.toString("UTF-8"); }
                String finalUrl=c.getURL().toString();
                boolean found = code==200 && looksReal(body,finalUrl,user,st.err);
                // redirect-away (to home/login) = not a real profile, even with 200
                if(found && !finalUrl.toLowerCase().contains(user.toLowerCase()) && !"message".equals(st.mode)) found=looksReal(body,finalUrl,user,st.err) && meta(body,"og:title").toLowerCase().contains(user.toLowerCase());
                if(found){ Hit hit=new Hit(); hit.site=st.name; hit.url=finalUrl;
                    String t=meta(body,"og:title"), d=meta(body,"og:description");
                    hit.extra=(t.isEmpty()?"":t)+(d.isEmpty()?"":" \u2014 "+(d.length()>180?d.substring(0,180)+"\u2026":d)); hits.add(hit); }
            }catch(Exception ignored){}
            synchronized(done){ done[0]++; if(cb!=null&&done[0]%10==0) cb.status("Username scan: "+done[0]+"/"+Sites.ALL.size()+" sites checked, "+hits.size()+" verified\u2026"); }
        }));
        for(Future<?> f:fs) f.get(); ex.shutdown();
        hits.sort(Comparator.comparing(a->a.site.toLowerCase())); return hits;
    }
}
