# Book Awards

A Kotlin Multiplatform (Compose Multiplatform) app for browsing books nominated for
literary awards, year by year, with a filter to pick which awards to show.

## Status

- `composeApp` (commonMain / androidMain / iosMain) builds successfully — verified with
  `./gradlew :composeApp:assembleDebug` and `./gradlew :composeApp:compileKotlinIosSimulatorArm64`
  on this machine.
- Data is a hand-written Kotlin list (`data/SampleNominations.kt`), not JSON/CSV — see
  "Data" below for why, and how to swap it out.
- The iOS host app (`iosApp/`) has the Swift source (`iOSApp.swift`, `ContentView.swift`)
  but **no `.xcodeproj` yet** — Xcode isn't installed on this machine, so there was no way
  to generate/verify one. See "iOS project" below.

## Structure

```
book-awards/
  composeApp/                 KMP module: shared logic + Compose UI
    src/commonMain/           Data models, sample data, UI (App.kt) — shared by Android & iOS
    src/androidMain/          MainActivity, AndroidManifest
    src/iosMain/               MainViewController (entry point Swift calls into)
  iosApp/iosApp/               Swift host app source (needs an Xcode project — see below)
```

## Running on Android

```
./gradlew :composeApp:installDebug
```

or open the `book-awards` folder in Android Studio and run the `composeApp` configuration.

## Data

The award/year/book list lives in `composeApp/src/commonMain/kotlin/.../data/SampleNominations.kt`
as a plain `List<Nomination>` — no JSON parsing, no serialization library, just a Kotlin file.
That was a deliberate simplification for this scaffold: it's editable with IDE autocomplete,
type-checked at compile time, and needs zero extra dependencies. It currently has a small
seed set (a few recent Booker/Pulitzer/National Book Award winners) — replace/expand it with
a full per-year dataset for each award you care about.

If the list grows large enough that editing Kotlin gets unwieldy, the natural next step is
moving it to a bundled JSON/CSV resource file parsed with `kotlinx-serialization` — the
`Award`, `NominationStatus`, and `Nomination` types in `Models.kt` are already shaped for
that.

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
(`data/Models.kt` — `enum class Award`). Add more by adding entries to that enum and to the
sample data.

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
