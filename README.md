# Yuyue · 语曰

<p align="center">
  <img src="docs/assets/yuyue-logo.jpg" alt="Yuyue logo" width="128" />
</p>

<p align="center">
  <strong>Speak a sentence. It lands on that day.</strong><br/>
  A private, local-first voice diary for time — not a cloud notes app.
</p>

<p align="center">
  <a href="./README.md">English</a> · <a href="./README.zh-CN.md">中文</a>
</p>

<p align="center">
  <img src="docs/assets/yuyue-home.jpg" alt="Home — hold to speak" width="180" />
  <img src="docs/assets/yuyue-time.jpg" alt="Time — calendar memory map" width="180" />
  <img src="docs/assets/yuyue-recall.jpg" alt="Recall — find by voice or text" width="180" />
  <img src="docs/assets/yuyue-vault.jpg" alt="Vault — lock and encrypted backup" width="180" />
</p>

---

## What it is

**Yuyue** (语曰) is an Android app for writing a **private time diary by speaking**.

| Loop | Meaning |
|------|---------|
| **Capture** | Hold to speak, type, share photos — raw words persist on-device first |
| **Time** | Month calendar → day capsules → entry detail (fuzzy times like “about evening”) |
| **Recall** | Ask in voice or text — local FTS / embeddings; hits are traceable to entry IDs |

AI is optional polish only. **Your on-device database and backups are the source of truth.** Model failure never invents or overwrites diary entries.

Brand palette: **Paper · Ink · Moss · Clay**.

## Screens

| Home | Time | Recall | Vault |
|------|------|--------|-------|
| Voice orb first; today + recent | Memory map calendar | Dated recall nodes | Lock, YUY2 backup, export |

Interactive HTML prototype (clickable): [`docs/rebuild/prototypes/index.html`](docs/rebuild/prototypes/index.html)

## Architecture (short)

```text
Voice / Text / Photo / Share
        ↓
   CaptureKernel (local L0 route + split)
        ↓
  Room yuyue.db  (+ optional Domain dual-write)
        ↓
  Time / Recall / Vault UI
        ↓
  Optional cloud polish (non-blocking)
```

- **Backup**: encrypted **YUY2** (salt + KDF in file header). Plain ZIP is labeled “unencrypted export”.
- **Package**: personal `com.yuyue.app` · QA `com.yuyue.app.qa`

## Quick Start

### Requirements

- Android Studio Ladybug+ (or equivalent) with JDK **17**
- Android SDK **35**, device/emulator **API 29+**
- Network once to download Vosk / BGE model assets on first build (cached under `android-app/src/main/assets/`, gitignored)

### 1. Clone

```bash
git clone https://github.com/XiaoYuTianTian/yuyue.git
cd yuyue
```

### 2. Configure secrets (required template)

```bash
cp config.local.properties.example config.local.properties
```

Edit `config.local.properties` — replace every `***`:

| Key | Required? | Purpose |
|-----|-----------|---------|
| `LLM_API_KEY` | Optional | MiniMax (or OpenAI-compatible) key for polish |
| `LLM_BASE_URL` | Optional | Default `https://api.minimaxi.com` |
| `LLM_MODEL` | Optional | Default `MiniMax-M2.7` |
| `SILICONFLOW_API_KEY` | Optional | Fast extract; leave `***` / empty to skip |
| `JWT_SECRET` | Only if running legacy `server/` | Dev secret |

> Keys are injected into `BuildConfig` at compile time and **must not** be committed.  
> The app **runs offline** for capture / calendar / local recall without any API key.

### 3. Open & run

1. Android Studio → **Open** the repo root (not only `android-app/`).
2. Select run configuration **`android-app`** (debug) or the **`qa`** build type for `com.yuyue.app.qa`.
3. Run on emulator or device. First launch creates default notebooks.

```bash
# CLI alternative (Windows)
.\gradlew.bat :android-app:installDebug
# or QA package
.\gradlew.bat :android-app:installQa
```

### 4. Smoke check

1. Hold the moss voice orb → say something → confirm preview → entry appears under **Time**.
2. Turn off network → capture again → raw text still saves.
3. **Vault** → set backup passphrase → encrypted YUY2 export.

## Configuration notes

- Never put real keys in README, commits, or screenshots.
- Release builds clear `DEFAULT_LLM_API_KEY` / `SILICONFLOW_API_KEY` in `build.gradle.kts`.
- `local.properties` (SDK path) is machine-local and gitignored.

## Docs

| Doc | Topic |
|-----|--------|
| [CHANGELOG.md](./CHANGELOG.md) | Version history — update on every release |
| [Product contract](./docs/rebuild/product-contract.md) | Trust / privacy red lines |
| [Legacy data contract](./docs/rebuild/legacy-data-contract.md) | Room v7 / backup formats |
| [Offline decrypt](./docs/offline-decrypt.md) | YUY2 / YUY1 offline recovery |
| [Rebuild complete](./docs/rebuild/REBUILD-COMPLETE.md) | Rebuild status & remaining device gates |
| [Chinese setup guide](./docs/本机运行调试指南.md) | Detailed local debug (ZH) |

## Security

Before publishing or forking:

1. Confirm `config.local.properties` is **not** in git (`git status`).
2. Rotate any key that ever appeared in chat, screenshots, or old forks.
3. Do not commit `docs/testing/evidence/` (may contain personal diary samples).

## License

No license file is attached yet — treat as **source-available / all rights reserved** until the author adds one. Ask before commercial use.

## Changelog policy

Every meaningful change should add an entry to [`CHANGELOG.md`](./CHANGELOG.md) (Keep a Changelog style). Tag releases as `vMAJOR.MINOR.PATCH`.
