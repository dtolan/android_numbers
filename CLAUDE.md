# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A native Android app (Kotlin + Jetpack Compose) that drills listening comprehension of spoken
numbers — integers, decimals, money, dates, percentages, phone numbers, times — in French,
Italian and English. It is an offline port of the web app at `../numbers` (a static HTML/JS page
using the browser's `speechSynthesis` API). This app uses Android's on-device `TextToSpeech`
engine instead, so it works with no network access once a language's voice data is installed
(Settings > Accessibility > Text-to-speech on the device, or via the in-app "Install voice" button).

## Commands

Build tooling (JDK 17, Android SDK, Gradle wrapper) is already installed on this machine.

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew assembleDebug          # build a debug APK (app/build/outputs/apk/debug/app-debug.apk)
./gradlew testDebugUnitTest       # run JVM unit tests (no device/emulator needed)
./gradlew lintDebug               # Android lint
./gradlew installDebug             # install to a connected device/emulator (adb required)
```

Run a single test class or method:
```bash
./gradlew testDebugUnitTest --tests "com.dtolan.numbers.logic.QuestionGeneratorsTest"
./gradlew testDebugUnitTest --tests "com.dtolan.numbers.logic.QuestionGeneratorsTest.integer question accepts the exact value*"
```

There is no emulator/system image installed in this environment (no KVM setup attempted) — verify
behavior via unit tests and `assembleDebug`/`lintDebug`, not by running the app here.

## Architecture

The core design principle: **question generation and answer-grading logic is pure Kotlin, fully
decoupled from Compose UI and from Android's TTS engine.** Everything speaks through plain,
locale-appropriate strings — the app never spells out number words itself; it hands the on-device
TTS voice a natural-language sentence (e.g. `"il est 15 heures 30"`, `"il primo gennaio 2020"`,
`"$12.50"`) and trusts that language's voice to pronounce the embedded digits correctly. This
mirrors the original web app's approach (`frNumberWords` there just returns a digit string).
The one exception is display-only: `data/NumberWords.kt` spells whole numbers out (French
traditional spelling, Italian single-word compounds, US English; `spellYear` for English
"nineteen fifty-four") via `LanguagePack.spellNumber`. It's shown in the passive Listen/Count
screens, the now-playing bar and the service notification, under whole-number answers after
answering (`ui/NumberWordsLine.kt`: Learn, Practice, Sequence, Stories), under Bingo's called
number, and inline in the Stories transcript (`annotateNumberWords`, which skips money, times,
phone/code digit groups and ordinals) — it is never handed to TTS.

- **`data/LanguagePack.kt`, `data/Languages.kt`** — the extensibility point. A `LanguagePack` bundles
  everything one language needs: locale, decimal separator, currency symbol/position, month names,
  date order (`DMY`/`MDY`), per-category spoken-sentence templates (`speakDate`, `speakTime`,
  `ordinalDay`), UI chrome strings (`UiStrings`), and a phone-number generator matching that
  country's numbering conventions. **To add a language, add one more `LanguagePack` instance and
  append it to `Languages.ALL`** — no other file needs to change.
- **`logic/QuestionGenerators.kt`** — one `genXxx` function per `Category` (integer, decimal, money,
  date, percent, phone, time), each taking a `LanguagePack` + `PracticeSettings` (+ an injectable
  `kotlin.random.Random` for deterministic tests) and returning a `Question` (what to speak, the
  canonical display answer, a placeholder, and a `checkAnswer` predicate). This is a fairly direct
  port of `numbers/app.js`'s `genInteger`/`genDecimal`/etc.
- **`logic/Formatting.kt`** — locale-agnostic helpers (`pad`, `englishOrdinal`).
- **`tts/TtsController.kt`** — thin wrapper around `android.speech.tts.TextToSpeech`. Exposes
  `hasUsableVoice(locale)` (checked via `isLanguageAvailable`) so the UI can show an "install voice
  data" prompt (`TtsController.installVoiceDataIntent()`) instead of silently failing when a
  language's offline voice isn't installed yet.
- **`data/SettingsStore.kt`** — Jetpack DataStore (Preferences) persistence for the selected
  language, enabled categories, min/max/decimal/year ranges and speech rate, so choices survive
  app restarts (the original web app did not persist these).
- **`viewmodel/PracticeViewModel.kt`** — holds `PracticeUiState` (current question, score, feedback,
  voice availability) as a `StateFlow`; mirrors the web app's `state` object and `nextQuestion` /
  `submitAnswer` / `resetScore` flow, wired to `SettingsStore` and `TtsController`.
- **`ui/`** — Compose screens (`NumbersApp`, `SettingsPanel`, `PracticeCard`, `ScoreBar`, `Theme`),
  one-to-one with the original `index.html` sections (settings panel, practice panel, score panel).

### Category-specific notes

- **Dates**: `LanguagePack.dateOrder` drives both the spoken word order and the canonical
  `displayAnswer`/input-parsing format (`DMY` for French/Italian, `MDY` for English) — see
  `genDate` in `QuestionGenerators.kt`.
- **Time**: kept in 24-hour `HH:MM` for `displayAnswer`/grading across all languages; only the
  spoken phrasing differs per language (`speakTime`).
- **Phone numbers**: each `LanguagePack.phoneNumber(random)` generates country-plausible numbers
  (French `0X XX XX XX XX`, Italian mobile prefixes, US `XXX-XXX-XXXX`); grading strips
  non-digits and compares against the generated digit string.
- **Stories (word problems)**: 6 `StoryTheme`s (shopping, café, travel, appointments, health,
  home), each with its own ladder of levels in `logic/StoryGenerator.kt` (recall → arithmetic →
  corrections "6… no, 8" → distractor numbers → two-voice dialogues). Sentences are templates in
  each language's `StoryPack` (`data/Stories.kt`), keyed by the `StoryKey` enum with `{name}`
  placeholders, plus a few grammar lambdas (`howMany`, `the`, `clock`, `duration`). A new language
  must define every `StoryKey` (StoryGeneratorTest enforces it). Dialogue lines carry a `Speaker`
  and are voiced at different pitches via `TtsController.speakLines`, whose word-progress callback
  drives `pictureCues`: "3 pommes" flashes 🍎🍎🍎 (from `StoryPack.pictures`) as the voice says it. Levels are gated per theme:
  5 correct in a row on the highest unlocked level unlocks the next (the learner moves up
  manually); progress is persisted per theme via `GameScoreStore.storiesGameId(theme)`.
- **Speak (answer out loud)**: `logic/SpeakGenerator.kt` builds 7 levels, all open (numbers, big
  numbers, ordinals, years, next/previous, mental maths, position puzzles); per-language text is
  in `data/Speak.kt` (`SpeakPack`). `ui/SpeakScreen.kt` owns the system `SpeechRecognizer`
  (RECORD_AUDIO + a `<queries>` entry for the recognition service) and passes its N-best guesses
  to `SpeechGrader`, which accepts figures ("92", "3e", "21st") or words compared after folding
  spaces/hyphens/accents/articles against `NumberWords` (cardinal, `spellYear`, `spellOrdinal`).
  Ordinals come from `LanguagePack.spellOrdinal` / `ordinalLabel`.
- **Money/percent/decimals**: locale decimal separator (`,` vs `.`) and currency symbol
  placement come from the `LanguagePack`; `normalizeNumberInput` strips symbols/whitespace and
  reconciles the separator before parsing user input.
