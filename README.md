# MISI SI CILIK – 3D ADVENTURE WORLD

Native Android (Kotlin) game for children 4–12. 3D worlds use a small custom **OpenGL ES 2.0 toon renderer**;
menus, rewards and the 5 mini-games use **Jetpack Compose**. Fully offline, no permissions, no ads, no Firebase.
All sound/music is synthesized in code (no audio files needed).

## Build the APK (GitHub Actions)
1. Create a GitHub repo and upload **everything in this folder** (keep the `.github/workflows/android.yml` path).
2. Open **Actions → Build Android APK → Run workflow**.
3. When it finishes, download artifact **MisiSiCilik-debug-apk** → `app-debug.apk` (built to `app/build/outputs/apk/debug/`).
4. Install on the phone (allow "install unknown apps").

If the build fails, open the failed step, copy the red error lines and send them back – they point to the exact file/line.

## Characters (up to 20)
Folder: `app/src/main/assets/characters/childN/` (N = 1..20, picked up automatically; fewer than 5 are padded with fallbacks)
- `portrait.jpg` (or .png/.webp) – square, ~256×256. Missing → cartoon fallback face is drawn.
- `profile.json` (optional):
```json
{"name":"Aisyah","skin":"#EBC3A0","hair":"#2B1B14","hairStyle":"long|short|bangs|spiky|none",
 "shirt":"#1F2A4D","pants":"#1B2340","headwear":"none|hijab|cap|hijabcap","headwearColor":"#F4B6C2","size":1.0}
```
Names can also be changed in the app (Pilih Watak → ✏️). Photos never leave the device and are not converted to
real rigged 3D models: the 3D avatar is a procedural cartoon built from the colours/style in `profile.json`.
Seven starter folders (child1–child7) were pre-filled from the supplied photos; rename/delete as needed.

## Features
4 worlds × 3 levels (12 total, procedurally generated, add more by raising the level count), run/jump/stomp, moving platforms,
stars, treasure chests; mini-games: Adventure Run, Treasure Hunt, Balloon Pop, Memory Master (3 levels), Magic Maze (8 levels);
per-child coins, stars, XP, badges, daily reward, costumes, stats; parent-gated settings (music, sound, graphics Low/Med/High, break reminder).

## Status
Written without access to an Android SDK: **not compiled or run on a device yet**. First GitHub Actions run is the real test.
