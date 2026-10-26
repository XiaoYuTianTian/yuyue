# Changelog

All notable changes to **Yuyue (语曰)** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project aims to follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Public GitHub presence: bilingual README (EN / 中文), brand logo, product UI imagery.
- `config.local.properties.example` with `***` placeholders for all secrets.
- This changelog for ongoing release hygiene.
- Real-device README screenshots (Home / Calendar / Recall / Settings) from QA build on MIX 3.
- MIT `LICENSE` (aligned with Longriver) and license badge on README.
- README images under `docs/images/` with jsDelivr URLs for reliable GitHub rendering.

### Changed
- Hardened `.gitignore` (APKs, evidence, models, secrets).
- Legacy `API_BASE_URL` no longer hardcodes a private LAN IP; reads from local config with emulator default.
- README hero gallery now prefers on-device PNGs over rendered mockups.

### Security
- Documented that real API keys must live only in gitignored `config.local.properties`.

## [1.0.15] - 2026-09-21

### Added
- Full rebuild M0–M3 foundation: product/legacy contracts, paper·ink·moss·clay theme, interactive prototypes.
- `YuyueDomainDatabase` + `LegacyV7Importer` + Domain dual-write (`FEATURE_DOMAIN_WRITE`).
- Local-first `CaptureKernel` (record / recall / mutate with zero mis-write gates).
- Navigation Compose shell (`FEATURE_REBUILD_SHELL`): Home / Time / Recall / Vault + global Capture dock.
- Mature features: PIN app lock, YUY2 encrypted backup, permanent trash, undo, share image intake, widgets, Markdown/TXT/PDF export, On-This-Day notifications.
- Differentiators: person / place / topic timelines, week/month review, photo wall (local stubs for map/audio).
- Gold harness: `docs/testing/gold40.jsonl` × 3 deterministic L0 routing tests.

### Changed
- Removed product write path via `AgentLoop` / `LocalAgentTools` / `TokenStore` / `OfflineSync` / `MutateSheet`.
- Backup RestoreAsCopy remaps media (and revisions) to copied entry IDs; ZIP path-traversal rejected.

### Notes
- Device matrix, QA restore drill sign-off, and personal-package upgrade verification remain operator checklist items (see `docs/rebuild/REBUILD-COMPLETE.md`).

---

## How to update this file

1. Add bullets under `[Unreleased]` as you work (`Added` / `Changed` / `Fixed` / `Security` / `Removed`).
2. On release: move `[Unreleased]` items into a new `## [X.Y.Z] - YYYY-MM-DD` section.
3. Tag git: `git tag -a vX.Y.Z -m "vX.Y.Z"`.
4. Bump `versionName` / `versionCode` in `android-app/build.gradle.kts` when cutting a store/QA build.
