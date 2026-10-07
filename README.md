# Binder

A simple, good-looking list app for Android (Kotlin + Jetpack Compose), in the family with Paperback and Scrapbook.

## Build it
1. Install **Android Studio** (current version) and open this folder (`File > Open`).
2. Let it sync (it downloads Gradle and the libraries the first time).
3. Plug in your phone (USB debugging on) or start an emulator, press the green **Run** button.
   To get an installable file instead: `Build > Build App Bundle(s) / APK(s) > Build APK(s)` → `app/build/outputs/apk/debug/app-debug.apk`.

Tip: always build from the same computer, so updates install over the old version and keep your data.

## What's in v1
- Lists on a home screen (tiles or rows), filter by Media / Checklists
- List templates: Books, Movies, Albums, Checklist
- Each list has three looks: Grid, List, Shelf (checklists are a plain tick list)
- Items: title, author/year/artist, status, half-star rating, notes, cover colour
- Search across all lists, light/dark/system theme
- Everything is saved on the phone in `binder.json` (plain JSON, easy to sync later)

## Where things are
- `Models.kt` – lists, items, templates (add a new template in `Kind`)
- `Store.kt` – saving/loading + navigation
- `Theme.kt` – colours, fonts, cover colours
- `Components.kt` – reusable pieces (tiles, buttons, stars…)
- `Screens.kt` – all screens
- Fonts: Outfit is bundled. For Newsreader (book titles in the mockups) add `newsreader_regular.ttf` to `app/src/main/res/font/` and edit the `Serif` line in `Theme.kt`.
