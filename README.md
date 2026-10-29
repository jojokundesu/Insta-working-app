# InstaScope — Android Instagram & username OSINT

Paste an **Instagram link or @handle** → tap **SCAN**. No accounts, no tokens, no setup.

**What it does**
1. **Instagram profile** — full public profile via the same `web_profile_info` endpoint instagram.com itself loads (permanent ID, name, bio, business/category, follower/following/post counts, HD profile-pic link), with the public profile page as fallback.
2. **Post analysis** — hashtag frequency, displayed location names and recent-post stats from the profile's post feed.
3. **Username enumeration** — 70+ embedded site rules (status-code & soft-404 detection), 16 parallel connections.
4. **Page summaries** — each found page's public title/description extracted in-app, no manual link-hopping.
5. **Exportable report** — on-screen + Share/Export as text/JSON.

**Download:** GitHub Releases → `InstaScope.apk` (debug-signed for sideloading).
**Build:** GitHub Actions — JDK 17, Android SDK 34, Gradle 8.7. Offline logic test: `python3 tools/logic_test.py`

**v1.2:** HD profile picture and post photos are now rendered inside the app (not just
links), cross-platform hits are verified (status + final-URL + page-content checks,
so soft-404 false positives are filtered), and an in-app browser button opens the
profile where you can log in yourself to view stories/full posts.
