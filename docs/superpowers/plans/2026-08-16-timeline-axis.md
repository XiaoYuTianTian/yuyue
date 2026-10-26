# 语曰 · 一球三层轴 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the current cream/sage UI. Ship month → day capsules → preview-before-write, with green/red `done` and expandable notebooks.

**Architecture:** Room stays source of truth. Ingest runs the agent with writes disabled, shows `IngestPreviewSheet`, then persists. Calendar dots and day capsules read `done`. Recall still uses the bottom orb (search icon removed).

**Tech Stack:** Kotlin, Jetpack Compose, Room 2.6, MiniMax agent tools.

## Global Constraints

- Visual: keep `Cream` `#F5F0E8` and `Sage` `#4A5D4E`. Add `Clay` `#C45C4A` only for 未做. No 国风 restyle.
- App name stays 语曰. No search icon on home. Settings gear stays.
- Past and future both record. `done=true` green; `done=false` red. Time passing does not flip `done`.
- Date: send device now to the model; if it cannot judge, default today.
- Data stays on-device Room. Destructive migration OK (`yuyue.db` v3).
- Do not commit unless the user asks.

## File map

- `android-app/src/main/java/com/yuyue/app/data/local/Entities.kt` — `done`, `utteranceId`
- `android-app/src/main/java/com/yuyue/app/data/local/YuyueDatabase.kt` — version 3
- `android-app/src/main/java/com/yuyue/app/util/MonthDots.kt` — month red/green aggregation
- `android-app/src/main/java/com/yuyue/app/agent/LocalAgentTools.kt` — persist flag, `done`, notebook upsert
- `android-app/src/main/java/com/yuyue/app/data/YuyueRepository.kt` — propose ingest, confirm, notebooks, setDone
- `android-app/src/main/java/com/yuyue/app/ui/*` — calendar dots, day capsules, preview, no search, add notebook

## Tasks

### Task 1: Data + month dots

`EntryEntity.done: Boolean`, `utteranceId: String?`. Room v3. `MonthDots.forDay(doneFlags): DayDot` — any false → Red, else Green. Calendar uses `Map<Int, DayDot>`.

### Task 2: Home chrome

Remove search icon. Notebook row gets `+`. Dialog creates a custom notebook (slug from name, random color from sage/work palette). Seeds cannot be deleted.

### Task 3: Day capsules

Replace day list cards with time axis + sage/clay capsules. Tap expands body; expanded row can toggle `done`. Tap title still opens edit. Edit screen has 已做 switch.

### Task 4: Preview ingest

`LocalAgentTools.persistWrites`. Ingest sets it false, collects hits as preview items (title, startAt, done, notebookId). Sheet: 先看一眼 / 记下 / 先不写. Confirm upserts with shared `utteranceId`. Prompt: already-happened → done true; will-do → false; include `done` on upsert.

### Task 5: Recall + tools (later)

Filter recall by done when the query is 做了什么 vs 没做. `set_entry_done`, `upsert_notebook`. Jump to day axis. Not in the first visible build if timeboxed — do after Task 4 compiles.

---

Execute inline in this session, Task 1 → 4 first.
