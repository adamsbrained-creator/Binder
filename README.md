# Binder 1.0.0

A list app for Android (Kotlin + Jetpack Compose), in the family with Paperback and Scrapbook.

**Privacy:** your lists and pictures stay on the phone. Only the text you search for is sent, and only when
*More > Fetch details automatically* is on (it is off by default). With it off, Binder makes no network calls at all.

## Build it
Push this folder to GitHub. Use the **Binder (signed)** workflow for the APK you install: it signs every build with
your own key, so a new APK installs **over** the old one and keeps your lists (no uninstalling).

**One-time setup**
1. Actions > **Create signing key** > Run workflow.
2. Open that run, download the artifact `signing-secrets` and open the text file inside.
3. Settings > Secrets and variables > Actions: add the four secrets it lists (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`). Optional: `TMDB_KEY` and `RAWG_KEY`.
4. Delete the artifact on the run page and keep the text file somewhere safe. Lose the key and you can only reinstall.
5. Actions > **Binder (signed)** > Run workflow. The APK is under Artifacts > `binder-apk`.

Every later push (or "Run workflow") makes a new signed APK. Its version code rises with each run, so Android accepts it
as an update. The run's summary shows the key's SHA-256 fingerprint; it must be the same every time.
The **Binder** and **Android (backup)** workflows still make debug APKs, signed with a throwaway key: fine for a quick
test, but do not mix them with signed builds on the same phone.
The signed workflow runs the unit tests after saving the APK; if that last step is red, open it to see which test failed
(the APK is still there).

## New in 1.0.0
- **New shell**: round header buttons, a black/white **menu button** at the bottom (tap = menu, press and hold = pops out a +),
  and a menu sheet with Lists / Pinned / Recently edited, Search, More and About.
- **App name**: Binder or Catalog (More > App name). One switch sets the name, the launcher icon and label (two activity-aliases, see `Launcher.kt`), and the word for your lists everywhere: Bind/Binds or Catalog/Catalogs.
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
Paperback / Scrapbook sync, "Up next" from Paperback, Spotify links.

## Fetching and keys
Everything below runs only when *More > Fetch details automatically* is on, and only for what you type or paste.

| Kind | Sources (first is the default) | Key |
|---|---|---|
| Albums | Apple Music (iTunes Search) | none |
| Movies | TMDB | `TMDB_KEY` |
| Series | TMDB, AniList (anime) | `TMDB_KEY` for TMDB |
| Games | RAWG | `RAWG_KEY` |
| Books | Open Library, Google Books (also the automatic fallback) | none |
| Manga | AniList | none |
| Comics | Google Books | none |
| Anything | Wikipedia | none |
| Places | OpenStreetMap (Nominatim, searches when you press enter) | none |

Any pasted link is routed to the source that recognises it; any other web page is read through its Open Graph tags.
A source's own rating is never imported.

**Keys.** Put `TMDB_KEY=...` and `RAWG_KEY=...` in `local.properties` (already git-ignored) for a local build, or add
them as repository secrets named `TMDB_KEY` and `RAWG_KEY` (Settings > Secrets and variables > Actions) for the
workflows. Nothing is committed. A build without a key still works: that source says it isn't set up and is hidden in
More > Sources.

Limits kept by the app: Apple 15 calls a minute, TMDB 30 per 10 s, RAWG 10 per 10 s, Open Library 3 per 3 s,
Nominatim 1 per 1.1 s, AniList 40 per minute.

## Manual checks (things the unit tests can't see)
1. **Binder to Catalog and back** (More > App name): the wordmark, the Bind/Binds or Catalog/Catalogs wording
   (menu, empty screen, New..., Add to..., Rename, About), the About icon, and after a moment the launcher icon and label.
   You may need to re-add the icon to the home screen. Switch back and check again.
2. **Restore a backup** (More > Backup > Restore): lists and pictures come back; the launcher entry still matches the
   App name; nothing else changes.
3. **Upgrade from the current build**: install over 1.0.0 with the same signing key. Lists, options and pinned lists
   survive; More has a Rating style row; "listName" is simply ignored.
4. **Fetching off** (the default): no network calls at all. Try a link paste and a search: nothing is sent. Turn it on
   and the sources appear.
5. **Ratings**: set 0.5, 2.5, 4.75 in both styles; large font size (Settings > Display) keeps stars aligned.
6. **Bento and Stacks** with covers, without covers (checklists), and with an empty list.

## Licences and credits
About > Licences has the TMDB notice, RAWG, Open Library, AniList, Wikipedia (CC BY-SA) and OpenStreetMap
credits, and a "Data from RAWG" link.

## Where things are
- `Models.kt` – lists, items, list templates (`Kind`), look options
- `Store.kt` – saving, undo, zip backup, navigation, fetching and retry
- `Persist.kt` – reading and writing the JSON file (old files still load)
- `Meta.kt`, `Http.kt` – the source interface, one provider per source, link parsing, the small HTTP helper (GET and POST)
- `Rating.kt`, `RatingUi.kt` – rating maths (pure) and the drawn stars, number style and rating sheet
- `Launcher.kt` – keeps the launcher icon and label in step with the App name
- `tools/make_icons.py` – makes the launcher icons from the Outfit font
- `Images.kt` – picking, downloading, shrinking and showing pictures
- `Theme.kt` – colours, fonts, cover colours
- `Components.kt` – shared pieces and gestures
- `Shell.kt` – menu button, menu sheet, About page
- `Navigation.kt` – headers, optional navigation bars, drawer
- `Overlays.kt` – hold menus, quick add (with search), undo bar
- `Screens.kt` – home, list and item screens
- `Settings.kt` – new list, search, More
- `app/src/test/` – unit tests (one file per source with canned answers, ratings, saving and loading old files)
- Fonts are in `app/src/main/res/font/`, licences in `FONT-LICENSES/`.
