# Binder 0.5

A list app for Android (Kotlin + Jetpack Compose), in the family with Paperback and Scrapbook.
Everything lives on the phone. No account, no internet.

## Build it
Push this folder to GitHub and run **Binder** from the Actions tab (or **Android (backup)** if the first one fails).
The APK is under Artifacts. Or open the folder in Android Studio and press Run.
Updates install over the old version and keep your lists, as long as every build is signed with the same key
(see the `DEBUG_KEYSTORE_BASE64` note in the workflow files).

## New in 0.5
- **Pictures**: add up to 8 photos to any item (Item page > Photos). The first one becomes the cover.
- **Layouts**: Grid, List, Shelf, Sections, Cards for every list, plus Manual / Newest / A–Z / Rating sorting
  (grid button at the top of a list).
- **Home layouts**: Pinned, Tiles, Rows, Stacks, Bento (More > Home layout).
- **Headers** (4) and **navigation bars** (10, including none, drawer and side rail) (More).
- **Adding items**: quick sheet, full editor or an add bar. Press and hold the + button for the other one.
- **Item page**: Standard or Collage.
- **Gestures**: press and hold a list or item for a menu; swipe a row right (next status / pin) or left (delete);
  drag the grip to reorder; delete can be undone.
- **Fonts**: Outfit is the standard. Options with Lora, Crimson Pro or Instrument Serif for titles, or Instrument Sans / Work Sans for everything.
- **Binder / Catalog**: More > App name changes the name at the top and in the drawer.
- **Zip backup**: More > Save backup / Restore. One .zip with all lists, text and pictures.
- New list templates: Series, Games, Places, Anything.

Old 0.1 data is read automatically.

## Where things are
- `Models.kt` – lists, items, list templates (`Kind`), look options
- `Store.kt` – saving, undo, zip backup, navigation
- `Images.kt` – picking, shrinking and showing pictures
- `Theme.kt` – colours, fonts, cover colours
- `Components.kt` – shared pieces and gestures (tap/hold, swipe, drag to reorder)
- `Navigation.kt` – headers, navigation bars, drawer
- `Overlays.kt` – hold menus, quick add, undo bar
- `Screens.kt` – home, list and item screens
- `Settings.kt` – new list, search, More
- Fonts are in `app/src/main/res/font/`, licences in `FONT-LICENSES/`.
