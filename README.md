# Book Awards

A Kotlin Multiplatform (Compose Multiplatform) app for browsing books nominated for
literary awards, with a filter to pick which awards to show. **Home** lists the current year and the
two before it; the **Library** tab shelves every earlier year as a sideways-scrolling row of covers
(`HOME_YEARS` in `data/YearWindow.kt`). Tap a book for its cover, description and every nomination it
received; mark it Want to read / Reading / Read; and turn on the bell to get a notification on award
announcement days. The **Theatre** tab lists upcoming Black theatre in London (on now, opening within 30
days, later), filterable by venue; mark a show Want to see / Booked / Seen, open the venue's page to book,
and turn on its bell to be reminded on opening day and a week before it closes.

## Status

- `composeApp` (commonMain / androidMain / iosMain) builds successfully — verified with
  `./gradlew :composeApp:assembleDebug` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
  on this machine.
- Data comes from `data/awards.json`, built from Wikidata and Wikipedia by `data/fetch-awards.mjs` — see "Data".
- Android's UI is Compose (Material 3); iOS's is native SwiftUI over the same Kotlin data layer — see "iOS app".

## Structure

```
book-awards/
  composeApp/                 KMP module: shared logic + Compose UI
    src/commonMain/           Data layer (data/), screens (ui/), navigation (App.kt), theme
    src/androidMain/          MainActivity, WorkManager-based announcement reminders
    src/iosMain/               BookAwardsModel (the data layer shaped for Swift), UserNotifications reminders
  iosApp/iosApp/               SwiftUI app: one view per file
  iosApp/iosAppUITests/        XCUITest E2E check (run by e2e/ios.sh)
  data/                       Award and theatre pipelines (fetch-awards.mjs, fetch-shows.mjs, theatre/) and their output
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

Last, it records each book's Open Library cover ID (`coverId`; `null` means Open Library has none) so the
Library can show thumbnails without a lookup per book (`data/covers.mjs`). IDs from the previous file are
reused, so only books never looked up cost a request, at about one a second; the first full run took
about 30 minutes. A failed lookup leaves the field out and is retried next run. `--skip-covers` only
reuses known IDs; `--retry-missing-covers` looks up books recorded as having no cover again (Open Library's
search results vary between runs). Lookups try a title-and-author search, then a general search whose
result must match the title.

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

A book's description and first-published year are looked up from Open Library's search and works APIs
when it's opened, as is the cover for a book the pipeline found none for.

Reading status is stored on the device, keyed by Wikidata ID (or title and author for hand-added
entries), so it follows a book across awards and years. See "On-device storage" for everything the app
saves.

## Theatre data

`data/fetch-shows.mjs` builds `data/shows.json`, separately from the awards, so a broken theatre run
can't touch `awards.json`:

```
node data/fetch-shows.mjs              # writes data/shows.json and data/show-candidates.md
```

It reads the "what's on" pages of the Royal Court, Bush, Almeida and Stratford East, and Official
London Theatre's `TheaterEvent` JSON-LD for the West End (`data/theatre/sources/`). It sends this
project's own user agent and waits a second between requests to a host. The National Theatre, Talawa,
Young Vic and Eclipse refuse scripted requests, and we don't disguise the pipeline as a browser, so their
shows are entered by hand. Venues' date ranges ("Wed 21 – Sat 24 Oct", "Until 24 Oct", "Tue 20th Oct -
Sat 31st Oct") are read by `data/theatre/dates.mjs`. Anything it isn't sure of is left undated rather
than guessed. Image URLs keep the venue CDN's sizing query: the Royal Court's and Stratford East's are
signed and refused without it.

Nothing is published without being chosen in **`data/shows-curated.json`**:

- `include`: scraped listings by ID (`venue:slug`), each with an optional `note` (shown in the app as
  why it's listed) and any fields to override, e.g. an `endDate` the venue hasn't printed.
- `exclude`: listings decided against, so they stop appearing as candidates.
- `companies`: names that include a listing automatically when its title, credits or description
  mention them (whole words), e.g. a Talawa co-production at another venue.
- `manual`: full entries (`id`, `title`, `venue`, `url`, and `startDate` and/or `endDate`, optionally
  `imageUrl`) for venues we don't scrape.

To curate, run the script, read `data/show-candidates.md` (git-ignored: every running listing not yet
included or excluded), add IDs to `include` or `exclude`, and run it again. A null `startDate` means
already running; a null `endDate` means no end announced. Only shows that haven't ended are written.

The script refuses to write if a source returns no listings (its page layout has probably changed), an
included show can't be found anywhere, an included show's dates can't be read, an end comes before a
start, or an ID appears twice. If a venue drops a show it still lists as running (some remove sold-out
runs), the copy in the previous `shows.json` is kept with a warning. Official London Theatre's copy of a
production a venue also lists is dropped. There's no shrink check like the awards have, because the list
gets shorter every time a run ends.

Publishing works like the awards: push `data/shows.json`, and apps fetch it from `main`
(`PUBLISHED_SHOWS_DATA_URL`, overridden in debug builds with `-PshowsDataUrl`) with the same ETag,
newer-`generatedAt` and offline-cache rules. Any non-empty file is accepted. The app hides ended runs
itself too, so a stale file never shows a finished show. `syncCommonResources` bundles the file into the
app at build time.

## Announcement reminders

The bell in the top bar schedules a 9am local notification for each date in
`data/Announcements.kt` (Android: WorkManager; iOS: `UNCalendarNotificationTrigger`). Only dates the
prizes have published belong there — update the list when each season's dates are announced. The
app reschedules on launch, so new dates reach people who already turned reminders on.

The Theatre tab's bell is separate (`notifications/ShowReminders.kt`). It covers shows marked **Want to
see**: 9am on opening day, and 9am a week before closing (runs shorter than a week only get the opening
reminder). Booked and Seen shows get none. Reminders are rescheduled whenever the listings or your
statuses change. The IDs last scheduled are saved, so reminders for a dropped show or a changed status are
cancelled.

## On-device storage

There's no database. Everything the app keeps goes through
[multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) (the `no-arg` artifact),
which wraps **SharedPreferences** on Android and **NSUserDefaults** on iOS. Maps are stored as JSON
strings made with kotlinx.serialization. All of it lives in shared Kotlin (`commonMain`), so both
platforms use the same keys:

| Key | What | Written by |
|---|---|---|
| `readingStatuses` | Book key → Want to read / Reading / Read, as JSON | `data/ReadingList.kt` |
| `showStatuses` | Show ID (`venue:slug`) → Want to see / Booked / Seen, as JSON | `data/ShowList.kt` |
| `announcementNotifications` | Whether the Home bell is on | `NotificationPreferences` |
| `showReminders.enabled` | Whether the Theatre bell is on | `ShowReminderPreferences` |
| `showReminders.scheduledIds` | The theatre reminder IDs scheduled last, one per line, so stale ones can be cancelled | `ShowReminderPreferences` |
| `nominationCache.text` / `.etag` | The last good download of `awards.json` and its ETag | `data/DownloadCache.kt` |
| `showCache.text` / `.etag` | The same for `shows.json` | `data/DownloadCache.kt` |

Other places data lives:

- **Bundled data:** `data/awards.json` and `data/shows.json` are copied into the Compose resources at
  build time (`syncCommonResources`), so the first launch works offline. The cached download is used only
  when it's newer than the bundled copy.
- **Scheduled reminders:** handed to the OS. Android uses WorkManager, which keeps its own SQLite
  database; iOS uses `UNUserNotificationCenter`. The app reschedules on launch (awards) and whenever
  listings or show statuses change (theatre).
- **Images:** Coil's cache on Android; on iOS, `AsyncImage` goes through the system URL cache. Neither
  is managed by the app.
- **Not saved:** the award and venue filters are in memory only and reset when the app restarts.

**Keys that carry history.** A reading status saved under a book's `title|author` fallback key moves to
its Wikidata ID once the data has one (`withCurrentBookKeys`). The awards cache keeps the prefix
`nominationCache` from before `DownloadCache` was shared with shows, so existing caches still load.
Rename or reshape a key only with a migration like those, or people lose their statuses.

**Resetting.** Clearing the app's data (`adb shell pm clear com.theseuntaylor.bookawards`, which the
Android E2E scripts do) or launching with `-resetState` on iOS (which the UI test does) wipes all of the
above except images and the OS's own copy of scheduled reminders.

**Size.** Both stores load their whole contents into memory. Caching the 261 KB `awards.json` as one
string is fine at that size. If the data grows to a few MB, move the cached downloads to files in the
app's files directory and keep the preferences for the small values.

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

### App icon

The "Laurel & Book" icon (gold laurel, star and open book on the theme's deep green) is drawn in code by
`design/generate-icons.mjs`, so both platforms come from one geometry:

```
cd design && npm run icons
```

It writes the Android adaptive icon (vector foreground, background colour, monochrome layer for themed icons)
under `composeApp/src/androidMain/res/`, the iOS `AppIcon` (light, dark and tinted 1024px PNGs, rendered by
`design/render-png.swift`, so it needs macOS) under `iosApp/iosApp/Assets.xcassets/`, and the light master
as `design/icon/app-icon.svg`. The green follows the seed; the gold is fixed in the script.

## Award scope

Currently modeled: Booker Prize, Pulitzer Prize for Fiction, National Book Award for Fiction
(`data/Models.kt` — `enum class Award`). To add one, add its Wikidata ID to `awards` in
`data/fetch-awards.mjs` and a matching entry to the enum.

## iOS app

The iOS UI is SwiftUI, so it looks and behaves like an iOS app: tab bar, large-title navigation, swipe
back, pull to refresh, toolbar menus. It shares everything below the UI with Android through the
`ComposeApp` framework: data, the refresh from GitHub, the reading list and reminders.
`composeApp/src/iosMain/.../ios/BookAwardsModel.kt` is the only Kotlin written for Swift. It turns flows
into main-thread callbacks and catches its own errors, so Swift only sees plain `async` calls.
`iosApp/iosApp/AppModel.swift` holds that as SwiftUI state.

The Xcode project is generated from `iosApp/project.yml` by XcodeGen (`cd iosApp && xcodegen generate`).
Change settings there, not in Xcode, or they'll be lost the next time it's generated. The app builds the
Kotlin framework itself in a pre-build step. The UI test runs under the `iosAppE2E` scheme; it has its own
name so a personal `iosApp` scheme that Xcode creates can't hide it.

## Known warnings

The build succeeds but Gradle 9.4.0 prints deprecation warnings about the `compose.runtime` /
`compose.material3` / etc. dependency accessors in `composeApp/build.gradle.kts` — non-fatal,
still the documented Compose Multiplatform API for this version, left as-is.

## E2E checks

`pipeline.sh` and `shows-pipeline.sh` only need Node and network access. For the others, with an Android emulator or device connected, each script builds and installs the app, drives it
with `adb`, and exits non-zero on failure:

```
e2e/core-flow.sh           # open a book, details load, mark "Want to read", chip shows in the list
e2e/sticky-header-tap.sh   # tapping a pinned year header doesn't open the book under it
e2e/data-refresh.sh        # published data arrives, ETag/304, offline cache, bad publishes rejected
e2e/pipeline.sh            # rebuilds the dataset from live sources and checks announced lists are present
e2e/library.sh             # Home shows only recent years; Library shelves scroll, show covers, open books
e2e/shows-pipeline.sh      # theatre: unit tests, then a live run of every source, checking what would publish
e2e/theatre.sh [serial]    # Theatre tab: sections, ended runs hidden, venue filter, status, reminders, booking link
e2e/ios.sh [udid]          # iOS: the same journeys on a simulator via XCUITest; screenshots exported
```

`theatre.sh` serves test shows dated from the day it runs (`e2e/serve_awards.py` with a path argument),
so every run sees the same sections. It installs on the named device only (via `ANDROID_SERIAL`), unlike
the older scripts, which install on every connected device.

Unit tests cover the parts that can be tested on their own: `node --test data/theatre/` (date ranges and
curation) and `./gradlew :composeApp:testDebugUnitTest` (shared Kotlin, including the Theatre sections,
labels and reminder scheduling).

Pass a device serial (e.g. `emulator-5554`) if more than one is connected. Each run leaves its
screenshots, UI dumps and `result.txt` in `e2e/artifacts/<check>/<timestamp>/` (git-ignored).
`core-flow.sh` and `data-refresh.sh` clear the app's data, which also removes any saved reading
statuses and reminders. `data-refresh.sh` serves test data from a local server (`e2e/serve_awards.py`)
via `adb reverse`, using a debug build pointed at it with `-PawardsDataUrl`; debug builds allow
plain HTTP to `localhost` only.
