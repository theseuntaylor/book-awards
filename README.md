# Book Awards

A Kotlin Multiplatform (Compose Multiplatform) app for browsing books nominated for
literary awards, year by year, with a filter to pick which awards to show. Tap a book for its
cover, description and every nomination it received; mark it Want to read / Reading / Read; and
turn on the bell to get a notification on award announcement days.

## Status

- `composeApp` (commonMain / androidMain / iosMain) builds successfully — verified with
  `./gradlew :composeApp:assembleDebug` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
  on this machine.
- Data comes from `data/awards.json`, built from Wikidata by `data/fetch-awards.mjs` — see "Data".
- The iOS host app (`iosApp/`) has the Swift source (`iOSApp.swift`, `ContentView.swift`)
  but **no `.xcodeproj` yet** — Xcode isn't installed on this machine, so there was no way
  to generate/verify one. See "iOS project" below.

## Structure

```
book-awards/
  composeApp/                 KMP module: shared logic + Compose UI
    src/commonMain/           Data layer (data/), screens (ui/), navigation (App.kt), theme
    src/androidMain/          MainActivity, WorkManager-based announcement reminders
    src/iosMain/               MainViewController, UserNotifications-based announcement reminders
  iosApp/iosApp/               Swift host app source (needs an Xcode project — see below)
```

## Running on Android

```
./gradlew :composeApp:installDebug
```

or open the `book-awards` folder in Android Studio and run the `composeApp` configuration.

## Data

`data/fetch-awards.mjs` builds `data/awards.json` from two sources:

- **Wikidata** (SPARQL) for the long history.
- **Wikipedia**'s prize tables, which list new longlists and shortlists within days while Wikidata can lag
  by months: `List_of_winners_and_nominated_authors_of_the_Booker_Prize`, `National_Book_Award_for_Fiction`
  (Hardcover only for 1980–83) and `Pulitzer_Prize_for_Fiction` (bold title = winner, the rest finalists).
  Parsing is in `data/wikitext.mjs` and `data/wikipedia-sources.mjs`; columns are found by header name.

Where both sources have a book (same prize and year, and the same Wikidata ID, the same author, or the same
title and surname) the better status wins. Then the fixes in `data/corrections.json` apply. A correction
targets a book by `wikidataId` or `title`, and `"until": "YYYY-MM-DD"` makes it temporary — for a source
error expected to be fixed by then, like Wikipedia calling the 2026 National Book Award longlist
"finalists" before October 6. The script refuses to write if it finds a duplicate or if any award loses more
than 10% of its entries (`--allow-shrink` to override); `--out <path>` writes somewhere else.

The app doesn't keep a copy: the `syncCommonResources` Gradle task merges
`data/awards.json` into the compose resources at build time.

**Publishing without an app update:** on launch (and on pull to refresh) the app fetches
`data/awards.json` from `main` on GitHub (`PUBLISHED_AWARDS_DATA_URL` in `data/AwardsDataUrl.kt`).
Re-run the script and push `data/awards.json`, and installed apps pick it up. Rules:

- The check uses the ETag, so an unchanged file costs a 304 and no download.
- A download replaces the data only if its `generatedAt` is newer than the data shown (GitHub's CDN can
  serve a stale copy for a few minutes, and an app update can bundle newer data than what's published),
  it parses, and it has at least half as many entries; entries this app version can't read (e.g. a newly
  added award) are skipped individually.
- The last good download is cached on the device and used offline, while it's newer than the bundled data.
- The list stays at the top when new nominations arrive, and a snackbar says how many are new.

Book details (cover, first-published year, description) are looked up from Open Library's search
and works APIs when a book is opened. They aren't bundled.

Reading status is stored on the device, keyed by Wikidata ID (or title and author for hand-added
entries), so it follows a book across awards and years.

## Announcement reminders

The bell in the top bar schedules a 9am local notification for each date in
`data/Announcements.kt` (Android: WorkManager; iOS: `UNCalendarNotificationTrigger`). Only dates the
prizes have published belong there — update the list when each season's dates are announced. The
app reschedules on launch, so new dates reach people who already turned reminders on.

## Design seed

The theme is derived from the 64-character hex string in `design/seed`:

- Characters 1–6 are the M3 seed color. It runs through Material's tonal spot algorithm to produce the light and dark color schemes.
- Characters 7–8 pick the heading font (display, headline, and title roles) from a fixed list of serif fonts in `design/generate-theme.mjs`.
- Characters 9–10 pick the body font (body and label roles) from a fixed list of sans-serif fonts.

To change the design, edit the seed and regenerate:

```
cd design && npm install && npm run generate
```

This rewrites `theme/Color.kt` and the font files in `composeResources/font/`. Don't edit those by hand.

## Award scope

Currently modeled: Booker Prize, Pulitzer Prize for Fiction, National Book Award for Fiction
(`data/Models.kt` — `enum class Award`). To add one, add its Wikidata ID to `awards` in
`data/fetch-awards.mjs` and a matching entry to the enum.

## iOS project

Xcode isn't installed in this environment, so the `.xcodeproj` for `iosApp/` wasn't generated —
hand-writing a `project.pbxproj` without being able to open/validate it in Xcode is more likely
to produce a broken project than a working one. What's already in place and verified:

- `composeApp`'s `iosMain` source set and iOS targets (`iosX64`, `iosArm64`, `iosSimulatorArm64`)
  compile cleanly and produce a `ComposeApp` framework.
- `iosApp/iosApp/iOSApp.swift` and `ContentView.swift` are written against that framework's
  expected API (`MainViewControllerKt.MainViewController()`).

To finish the iOS side once you have Xcode available, either:
1. Open Android Studio with the Kotlin Multiplatform plugin and use its wizard to generate
   just the `iosApp` Xcode project pointed at this `composeApp` module, or
2. Create a new Xcode iOS App project named `iosApp` inside `iosApp/`, delete its generated
   Swift files, and drop in the two Swift files already here, then add a "Run Script" build
   phase (or use the KMP Gradle plugin's Xcode integration) to embed the `ComposeApp` framework
   from `composeApp/build/bin/iosSimulatorArm64/debugFramework`.

## Known warnings

The build succeeds but Gradle 9.4.0 prints deprecation warnings about the `compose.runtime` /
`compose.material3` / etc. dependency accessors in `composeApp/build.gradle.kts` — non-fatal,
still the documented Compose Multiplatform API for this version, left as-is.

## E2E checks

`pipeline.sh` only needs Node and network access. For the others, with an Android emulator or device connected, each script builds and installs the app, drives it
with `adb`, and exits non-zero on failure:

```
e2e/core-flow.sh           # open a book, details load, mark "Want to read", chip shows in the list
e2e/sticky-header-tap.sh   # tapping a pinned year header doesn't open the book under it
e2e/data-refresh.sh        # published data arrives, ETag/304, offline cache, bad publishes rejected
e2e/pipeline.sh            # rebuilds the dataset from live sources and checks announced lists are present
```

Pass a device serial (e.g. `emulator-5554`) if more than one is connected. Each run leaves its
screenshots, UI dumps and `result.txt` in `e2e/artifacts/<check>/<timestamp>/` (git-ignored).
`core-flow.sh` and `data-refresh.sh` clear the app's data, which also removes any saved reading
statuses and reminders. `data-refresh.sh` serves test data from a local server (`e2e/serve_awards.py`)
via `adb reverse`, using a debug build pointed at it with `-PawardsDataUrl`; debug builds allow
plain HTTP to `localhost` only.
