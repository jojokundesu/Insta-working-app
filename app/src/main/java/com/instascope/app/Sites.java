package com.instascope.app;
import java.util.*;
/** Sherlock-style site rules: template + detection mode. Public username-presence checks only. */
public class Sites {
    public static class Site { public final String name, tpl, mode, err;
        Site(String n,String t,String m,String e){name=n;tpl=t;mode=m;err=e;} }
    public static final List<Site> ALL = new ArrayList<>();
    private static void s(String n,String t){ ALL.add(new Site(n,t,"status",null)); }
    private static void m(String n,String t,String e){ ALL.add(new Site(n,t,"message",e)); }
    static {
        s("GitHub","https://github.com/{}"); s("GitLab","https://gitlab.com/{}");
        s("Reddit","https://www.reddit.com/user/{}"); s("YouTube","https://www.youtube.com/@{}");
        s("TikTok","https://www.tiktok.com/@{}"); s("X/Twitter","https://x.com/{}");
        s("Twitch","https://www.twitch.tv/{}"); s("Steam","https://steamcommunity.com/id/{}");
        s("Pinterest","https://www.pinterest.com/{}/"); s("Medium","https://medium.com/@{}");
        s("Dev.to","https://dev.to/{}"); s("Keybase","https://keybase.io/{}"); s("Tumblr","https://{}.tumblr.com/");
        s("Flickr","https://www.flickr.com/people/{}"); s("Vimeo","https://vimeo.com/{}");
        s("SoundCloud","https://soundcloud.com/{}"); s("Spotify","https://open.spotify.com/user/{}");
        s("Patreon","https://www.patreon.com/{}"); s("Trello","https://trello.com/{}");
        s("Bitbucket","https://bitbucket.org/{}"); s("Kaggle","https://www.kaggle.com/{}");
        s("HackerRank","https://hackerrank.com/{}"); s("LeetCode","https://leetcode.com/{}");
        s("Codeforces","https://codeforces.com/profile/{}"); s("Replit","https://replit.com/@{}");
        s("CodePen","https://codepen.io/{}"); s("Dribbble","https://dribbble.com/{}");
        s("Behance","https://www.behance.net/{}"); s("DeviantArt","https://www.deviantart.com/{}");
        s("Goodreads","https://www.goodreads.com/{}"); s("Letterboxd","https://letterboxd.com/{}");
        s("Last.fm","https://www.last.fm/user/{}"); s("Chess.com","https://www.chess.com/member/{}");
        s("Duolingo","https://www.duolingo.com/profile/{}"); s("TryHackMe","https://tryhackme.com/p/{}");
        s("HackTheBox","https://app.hackthebox.com/profile/{}"); s("DockerHub","https://hub.docker.com/u/{}");
        s("NPM","https://www.npmjs.com/~{}"); s("PyPI","https://pypi.org/user/{}");
        s("ProductHunt","https://www.producthunt.com/@{}"); s("About.me","https://about.me/{}");
        s("Gravatar","https://gravatar.com/{}"); s("WordPress","https://{}.wordpress.com/");
        s("Blogger","https://{}.blogspot.com/"); s("Substack","https://{}.substack.com/");
        s("Linktree","https://linktr.ee/{}"); s("Telegram","https://t.me/{}");
        s("Snapchat","https://www.snapchat.com/add/{}");
        s("Threads","https://www.threads.net/@{}"); s("Mastodon.social","https://mastodon.social/@{}");
        s("Bluesky","https://bsky.app/profile/{}.bsky.social"); s("Quora","https://www.quora.com/profile/{}");
        s("Wikipedia","https://en.wikipedia.org/wiki/User:{}"); s("Archive.org","https://archive.org/details/@{}");
        s("itch.io","https://{}.itch.io/"); s("Roblox","https://www.roblox.com/users/{}");
        s("PSNProfiles","https://psnprofiles.com/{}");
        s("Speedrun","https://www.speedrun.com/user/{}"); s("Tinder(Trovo)","https://trovo.live/{}");
        s("Kick","https://kick.com/{}"); s("Rumble","https://rumble.com/user/{}");
        s("DailyMotion","https://www.dailymotion.com/{}"); s("Mixcloud","https://www.mixcloud.com/{}/");
        s("Bandcamp","https://bandcamp.com/{}"); s("Etsy","https://www.etsy.com/shop/{}");
        s("eBay","https://www.ebay.com/usr/{}"); s("Poshmark","https://poshmark.com/closet/{}");
        // message-based (SPA / soft-404)
        m("Instagram","https://www.instagram.com/{}/","Sorry, this page isn");
        m("Facebook","https://www.facebook.com/{}","This content isn't available");
        m("LinkedIn","https://www.linkedin.com/in/{}","Page not found");
        m("OnlyFans-like(Fansly)","https://fansly.com/{}","not found");
        m("7 Cups","https://www.7cups.com/@{}","not found");
        m("Airliners","https://www.airliners.net/user/{}/photos/","Page Not Found");
    }
}
