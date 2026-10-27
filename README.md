# ⏱️ QuestionTimer

> **The Anti-Delusion Question Timer & Brutal Exam Analytics for JEE Main & JEE Advanced Aspirants.**  
> *Because staring at a textbook for 4 hours while daydreaming about IIT Bombay doesn't count as study time.* 📉

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_26--35)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Build & Tests](https://img.shields.io/badge/Tests-159_Passed_(100%25)-brightgreen?logo=checkmarx)](run_tests.sh)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Zero Bloat](https://img.shields.io/badge/Dependencies-0_Bloat_Libraries-blueviolet)](app/build.gradle)
[![Theme](https://img.shields.io/badge/UI-Pure_AMOLED_Black-000000)](app/src/main/res/values/colors.xml)

---

## 🧐 What Is This?

Most study timers are polite. They let you start a stopwatch, pause to check Instagram, stare at a single Irodov problem for 47 minutes, and then log it as "Productive Physics Study 💪".

**QuestionTimer** is built for aspirants who want to stop lying to themselves. It replaces polite timers with a relentless, two-zone touch surface, real-time pace tracking, and exam-accurate negative marking for **JEE Main (+4 / -1)** and **JEE Advanced (+4 / -2)**.

---

## 📲 Installation

> *"Wait, it's not on the Play Store? Are you telling me I have to enable 'Unknown Sources' like a 2012 Android hacker?!"*

Yes. Deal with it. 

### Method 1: The Direct APK Install (Fastest) 🚀
1. Grab the signed, pre-built binary directly from this repository: [`release/QuestionTimer.apk`](release/QuestionTimer.apk).
2. Transfer it to your phone (via USB, Telegram Saved Messages, Cloud Drive, or whatever channel hasn't been blocked by your parents yet).
3. Tap the `.apk` file to install.
4. If Android displays a popup claiming *"File might be harmful"*:  
   > ⚠️ **Calm down.** Play Protect flags every side-loaded indie APK that doesn't pay a developer license tax. The only thing in mortal danger here is your habit of postponing problem sets. Tap **"Install anyway"**.

### Method 2: The ADB Power-User Install 💻
If you have Android platform tools installed on your computer:
```bash
adb install -r release/QuestionTimer.apk
```

### Method 3: Build From Source (For Parrots Who Don't Trust Pre-builts) 🛠️
No Gradle daemon eating 8 GB of RAM required. We built a lightning-fast standalone direct compiler:
```bash
git clone https://github.com/YOUR_USERNAME/QuestionTimer.git
cd QuestionTimer
./run_tests.sh    # Executes all 159 unit tests in ~60ms
./build_apk.sh    # Direct compilation, D8 desugaring & v2/v3 signing in ~3s
```
Your shiny signed APK will land at `release/QuestionTimer.apk`.

---

## 🎮 How to Actually Use It (Without Cheating Yourself)

### 1. The Invisible Two-Zone Tap Surface 🌗
Forget fiddling with microscopic pause buttons while your brain is frying over organic reaction mechanisms. The entire screen is divided into two invisible, borderless zones:

```
┌──────────────────────────────────────────────┐
│                                              │
│               UPPER SCREEN HALF              │
│       🔴 TAP HERE = PAUSE & TAG WRONG        │
│                                              │
├ - - - - - - - - - - - - - - - - - - - - - - -┤
│                                              │
│               LOWER SCREEN HALF              │
│       🟢 TAP HERE = PAUSE & TAG CORRECT      │
│                                              │
└──────────────────────────────────────────────┘
```

- **Got the question WRONG?**  
  Tap anywhere on the **TOP HALF** of the display.  
  The timer instantly stops, flashes **Crimson Red**, gives a haptic kick, and records the attempt as **INCORRECT**.  
  *No, you don't get to say "I basically knew the formula, just made a calculation error, so I'll count it as right."* Tap the top half. Accept your `-1`. Move on.
- **Got the question RIGHT?**  
  Tap anywhere on the **BOTTOM HALF** of the display.  
  The timer stops, flashes **Emerald Green**, and tags it as **CORRECT**.
- **Ready for the next question?**  
  When paused, tap **ANYWHERE** on the screen. The clock resets and starts ticking for Question #N+1 immediately.
- **Accidental misclick panic?**  
  If your finger genuinely slipped, a quick **"Tap to Undo"** floating banner appears for 4 seconds at the bottom. Tap it to revert the tag before your misclick is etched into your permanent study statistics.

---

### 2. Customizing Feedback Transitions 🎨
Head into **Settings ⚙️ ➔ Display & Themes**:
- **Visual Styles:** Choose how your screen reacts when you pause:
  - `FADE_OUT`: Smooth, cinematic alpha dissipation.
  - `RIPPLE_SCALE`: Pulsing tactile scale wave.
  - `INSTANT_POP`: Snappy, punchy immediate flash.
- **Flash Duration:** Adjustable from **150 ms** (speed demons) up to **1200 ms** (so you can dwell in the agony of getting that rotational dynamics problem wrong).
- **Interactive Test Buttons:** Test buttons (`Test Wrong (Red)` and `Test Right (Green)`) let you preview feedback styles without messing up your active study session.

---

### 3. Exam Mode & Score Estimation 📊
Select your target exam in settings:
- **JEE Main Mode:** Evaluates your attempts using standard NTA rules:  
  $$\text{Score} = (\text{Correct} \times 4) - (\text{Incorrect} \times 1)$$
- **JEE Advanced Mode:** Simulates high-stakes negative marking:  
  $$\text{Score} = (\text{Correct} \times 4) - (\text{Incorrect} \times 2)$$

The bottom sheet history displays:
- **Estimated Marks:** Live calculated score with accuracy percentage.
- **Pace Analytics:** Average time per correct question vs. time wasted on incorrect attempts.
- **Mistake Categorization:** Tag mistakes by reason (`Calculation Error`, `Conceptual Gap`, `Time Pressure`, `Careless / Silly`, `Misread Question`).
- **Interactive Histograms & Heatmaps:** Daily consistency calendar and speed vs. accuracy scatter plots.

---

## ⚡ Key Features

| Feature | Description |
| :--- | :--- |
| **Two-Zone Touch Surface** | Borderless top/bottom screen dispatch for zero-look outcome recording. |
| **Instant Haptic & Visual Cues** | Crimson Red (`#A0FF453A`) & Emerald Green (`#A030D158`) feedback with custom timing. |
| **Pure AMOLED Theme** | Pure `#000000` dark mode to keep your display sipping battery during 12-hour study sprints. |
| **Offline Privacy** | Zero internet permissions requested. No trackers, no telemetry, no third-party SDKs. Your study data never leaves your device. |
| **Storage Access Framework (SAF)** | One-tap JSON and CSV backup/restore to Cloud Drive, SD Card, or local files. |
| **Orientation Aware** | Works seamlessly in both Portrait and Landscape tablet modes. |
| **Smart Study Coach** | Offline heuristic engine that highlights speed anomalies and recommends subject balance. |

---

## 🏗️ Architecture & Codebase

```
QuestionTimer/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Minimal permissions, no INTERNET required
│   │   ├── java/com/example/timerapp/
│   │   │   ├── data/
│   │   │   │   ├── analytics/           # JeeScoringCalculator, AnalyticsMath, StudyCoachEngine
│   │   │   │   ├── backup/              # SafBackupEngine, CsvManager
│   │   │   │   ├── engine/              # DualClockEngine, SessionStateMachine
│   │   │   │   ├── local/sqlite/        # TimerDatabaseManager, DatabaseContract
│   │   │   │   └── model/               # DisplaySettings, QuestionLog, Session, Subject
│   │   │   └── ui/                      # MainActivity, SettingsDialog, HistoryBottomSheet
│   │   └── res/                         # Clean vector drawables, layouts, and AMOLED styles
│   └── src/test/                        # 18 JUnit test suites covering 159 test cases
├── build_apk.sh                         # Direct command-line build pipeline (AAPT2 -> JAVAC -> D8 -> ZIPALIGN -> APKSIGNER)
├── run_tests.sh                         # OpenJDK 17 automated test execution runner
├── release/
│   └── QuestionTimer.apk                # Production signed release APK
├── LICENSE                              # MIT License
└── README.md
```

---

## 🧪 Testing

We believe critical study stats should never be lost to app crashes:
```bash
./run_tests.sh
```
Runs 159 unit tests covering:
- Two-zone hit detection coordinates across screen sizes and orientations
- Edge-case gesture rejection (swipe suppression, button protection)
- JEE Main and JEE Advanced scoring formulas and rounding rules
- Database migrations, SQLite CRUD transactions, and data integrity audits
- Storage Access Framework (SAF) export and import round-trips
- Monotonic clock math and background pause/resume reconciliation

---

## 🤝 Contributing

Contributions, bug reports, and suggestions are welcome!
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/cool-new-timer-feature`).
3. Ensure all tests pass (`./run_tests.sh`).
4. Commit your changes (`git commit -m 'feat: add cool feature'`).
5. Push to your branch (`git push origin feature/cool-new-timer-feature`).
6. Open a Pull Request.

---

## 📜 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.  
Built with determination for every aspirant grinding toward their dream college rank. 🎯
