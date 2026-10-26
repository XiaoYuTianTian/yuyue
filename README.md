[English](README.md) · [中文](docs/README.zh-CN.md)

<p align="center">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/logo.jpg" width="128" alt="Yuyue">
</p>

<h1 align="center">Yuyue · 语曰</h1>

<p align="center">Speak a sentence. It lands on that day.<br>
A private, local-first voice diary for time — not a cloud notes app.</p>

<p align="center">
  <a href="LICENSE"><img alt="MIT License" src="https://img.shields.io/badge/license-MIT-1f4e5f"></a>
  <img alt="Android" src="https://img.shields.io/badge/Android-API%2029%2B-3D6B4F">
  <img alt="Local-first" src="https://img.shields.io/badge/data-on%20device-9A6B4F">
</p>

**Yuyue** (语曰) is an Android app for writing a **private time diary by speaking**. AI is optional polish only. Your on-device database and backups are the source of truth.

<p align="center">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/home.png" width="24%" alt="Home — hold to speak">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/time.png" width="24%" alt="Time — calendar memory map">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/recall.png" width="24%" alt="Recall — find by voice or text">
  <img src="https://cdn.jsdelivr.net/gh/XiaoYuTianTian/yuyue@7d37ec2189d451f7305a670d9174e8ca15334990/docs/images/vault.png" width="24%" alt="Settings — backup and workspaces">
</p>

<p align="center"><sub>Screenshots from a real device (QA build).</sub></p>

## What you can do

| Loop | Meaning |
|------|---------|
| **Capture** | Hold to speak, type, or share photos — raw words persist on-device first |
| **Time** | Month calendar → day capsules → entry detail (fuzzy times like “about evening”) |
| **Recall** | Ask in voice or text — local FTS / embeddings; hits trace back to entry IDs |
| **Vault** | App lock, YUY2 encrypted backup, trash, notebooks, export |

Brand palette: **Paper · Ink · Moss · Clay**. Interactive HTML prototype: [`docs/rebuild/prototypes/index.html`](docs/rebuild/prototypes/index.html).

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

## Quick start

### Requirements

- Android Studio (JDK **17**)
- Android SDK **35**, device/emulator **API 29+**
- Network once on first build (Vosk / BGE models download into gitignored `assets/`)

### 1. Clone

```bash
git clone https://github.com/XiaoYuTianTian/yuyue.git
cd yuyue
```

### 2. Configure secrets

```bash
cp config.local.properties.example config.local.properties
```

Edit `config.local.properties` and replace every `***`:

| Key | Required? | Purpose |
|-----|-----------|---------|
| `LLM_API_KEY` | Optional | MiniMax (or OpenAI-compatible) polish key |
| `LLM_BASE_URL` | Optional | Default `https://api.minimaxi.com` |
| `LLM_MODEL` | Optional | Default `MiniMax-M2.7` |
| `SILICONFLOW_API_KEY` | Optional | Fast extract; leave empty / `***` to skip |
| `JWT_SECRET` | Only for legacy `server/` | Dev secret |

Keys are injected into `BuildConfig` at compile time and **must not** be committed.  
The app **runs offline** for capture / calendar / local recall without any API key.

### 3. Open & run

1. Android Studio → **Open** the repo root.
2. Run configuration **`android-app`** (or build type **`qa`** → `com.yuyue.app.qa`).
3. First launch creates default notebooks.

```bash
.\gradlew.bat :android-app:installDebug
.\gradlew.bat :android-app:installQa
```

### 4. Smoke check

1. Hold the voice orb → speak → confirm → mark appears under **Time**.
2. Turn off network → capture again → raw text still saves.
3. **Settings** → set backup passphrase → export YUY2.

## Docs

| Doc | Topic |
|-----|--------|
| [CHANGELOG.md](./CHANGELOG.md) | Version history — update on every release |
| [Product contract](./docs/rebuild/product-contract.md) | Trust / privacy red lines |
| [Legacy data contract](./docs/rebuild/legacy-data-contract.md) | Room v7 / backup formats |
| [Offline decrypt](./docs/offline-decrypt.md) | YUY2 / YUY1 offline recovery |
| [Rebuild complete](./docs/rebuild/REBUILD-COMPLETE.md) | Rebuild status & device gates |
| [Chinese setup guide](./docs/本机运行调试指南.md) | Detailed local debug (ZH) |

## Security

1. Keep `config.local.properties` out of git.
2. Rotate any key that ever appeared in chat, screenshots, or old forks.
3. Do not commit `docs/testing/evidence/` (may contain personal diary samples).

## License

[MIT](LICENSE) © 2026 XiaoYuTianTian

## Changelog policy

Every meaningful change should add an entry to [`CHANGELOG.md`](./CHANGELOG.md) (Keep a Changelog). Tag releases as `vMAJOR.MINOR.PATCH`.
