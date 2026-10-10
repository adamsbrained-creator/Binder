# Binder 1.0.0

A list app for Android (Kotlin + Jetpack Compose), in the family with Paperback and Scrapbook.

**Privacy:** your lists and pictures stay on the phone. Only the text you search for is sent, and only when
*More > Fetch details automatically* is on (it is off by default). With it off, Binder makes no network calls at all.

## Build it
Push this folder to GitHub and run **Binder** from the Actions tab (or **Android (backup)** if the first one fails).
The APK is under Artifacts. The **Binder** workflow also runs the unit tests after saving the APK; if that last
step is red, open it to see which test failed (the APK is still there).
Updates install over the old version and keep your lists, as long as every build is signed with the same key
(see the `DEBUG_KEYSTORE_BASE64` note in the workflow files).

## New in 1.0.0
- **New shell**: round header buttons, a black/white **menu button** at the bottom (tap = menu, press and hold = pops out a +),
  and a menu sheet with Lists / Pinned / Recently edited, Search, More and About.
- **Name for lists**: Lists, Binds, Catalogs or Sleeves (More > Name for lists). Used everywhere.
- **Albums can fetch details** (More > Fetch details automatically): type a title and pick a result, or paste an
  Apple Music link. Cover, artist, year, genre and track count are saved on the phone. If you are offline the item is
  saved as typed with "Details pending" and filled in when you open the list later.
- **Home**: pinned cards with a count and progress bar, list rows with a progress ring, dashed "+" slots.
- **List page**: progress bar, "done" check and progress on covers, ratings under covers, square covers for Albums and Games.
  View sheet: show Title / Rating / Added / Progress under covers, 2 / 3 / 4 columns.
- **Item page**: title in the header, status switch, details card (released, genre, source) and an "Open in Apple Music" button.
- **About** page, new text-only list templates, new icon.
- Old data is read automatically (0.1 files and 0.5 backups).

## Not in 1.0
Paperback / Scrapbook sync, "Up next" from Paperback, Spotify links. Other sources (TMDB for movies and series,
RAWG for games, Open Library for books) come later, one at a time.

## Fetching and keys
Albums use Apple's free iTunes Search API (no key). Future sources that need a key (TMDB, RAWG) will read it from
`local.properties` or a GitHub secret into `BuildConfig`. Never commit keys; `local.properties` is already ignored.
Apple limits the search service to about 20 calls a minute; Binder stops itself at 15.

## Where things are
- `Models.kt` – lists, items, list templates (`Kind`), look options
- `Store.kt` – saving, undo, zip backup, navigation, fetching and retry
- `Persist.kt` – reading and writing the JSON file (old files still load)
- `Meta.kt`, `Http.kt` – the source interface, Apple Music provider, link parsing, the small HTTP helper
- `Images.kt` – picking, downloading, shrinking and showing pictures
- `Theme.kt` – colours, fonts, cover colours
- `Components.kt` – shared pieces and gestures
- `Shell.kt` – menu button, menu sheet, About page
- `Navigation.kt` – headers, optional navigation bars, drawer
- `Overlays.kt` – hold menus, quick add (with search), undo bar
- `Screens.kt` – home, list and item screens
- `Settings.kt` – new list, search, More
- `app/src/test/` – unit tests (link parsing, Apple answers to items, saving and loading old files)
- Fonts are in `app/src/main/res/font/`, licences in `FONT-LICENSES/`.
