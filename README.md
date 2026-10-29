# InstaScope — Android public-profile & username OSINT

One-tap Android version of the public parts of the workflow in the video:
paste an **Instagram link or @handle** → tap **SCAN**.

**What it automates**
1. **Instagram public profile** — parses the public profile page (`og:` meta: display name, public follower/following/post counts, HD profile-pic link).
2. **Optional HikerAPI path** — if you paste *your own* HikerAPI token, it uses `/v1/user/by/username` + `/v1/user/medias` (your token, your quota) for the structured profile, hashtag frequency and displayed location *names*. No Instagram login, no password.
3. **Sherlock-style username enumeration** — 70+ embedded site rules (status-code & soft-404 message detection), 16 parallel connections.
4. **Maigret-lite summaries** — for each hit it fetches the page and extracts its public title/description so you don't have to open every link.
5. **Exportable report** — on-screen + Share/Export as text/JSON.

**Deliberate privacy scope (differences from the video):** only data already public
to a browser/token is shown. The app does **not** log in to Instagram, bypass
anti-bot systems, harvest other people's `public_email`/phone numbers from the
following graph, or extract exact GPS coordinates / street addresses hidden in
location objects. Same-username hits may be different people — verify first.
Use only on your own accounts or with authorisation.

**Download:** GitHub Releases → `InstaScope.apk` (debug-signed for sideloading).
**Build:** GitHub Actions (`.github/workflows/build.yml`) — JDK 17, Android SDK 34, Gradle 8.7, `assembleRelease`.
Offline logic mirror-test: `python3 tools/logic_test.py`
