"""Retest FI-ENTRY-002: ACTION_SEND cold + hot after SharedTextIntake."""
from __future__ import annotations

import json
import sqlite3
import subprocess
import time
from pathlib import Path

ADB = r"C:\Android\Sdk\platform-tools\adb.exe"
SERIAL = "45c5974e"
PKG = "com.yuyue.app.qa"
ACT = f"{PKG}/com.yuyue.app.MainActivity"
OUT = Path(r"C:\File\Project\Yuyue\docs\testing\evidence\device\e2e-20260919")
RESULTS = Path(r"C:\File\Project\Yuyue\docs\testing\results.jsonl")

COLD = "记下今天下午喝了咖啡-cold-task1"
HOT = "记下今天下午喝了咖啡-hot-task1"


def adb(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, timeout=40)


def pull_db(tag: str) -> dict:
    OUT.mkdir(parents=True, exist_ok=True)
    for name in ("yuyue.db", "yuyue.db-wal", "yuyue.db-shm"):
        dest = OUT / f"{tag}-{name}"
        with open(dest, "wb") as fh:
            subprocess.run(
                [ADB, "-s", SERIAL, "exec-out", "run-as", PKG, "cat", f"databases/{name}"],
                stdout=fh,
                check=True,
            )
    con = sqlite3.connect(OUT / f"{tag}-yuyue.db")
    data = {
        "notebooks": con.execute("SELECT id,name FROM notebooks").fetchall(),
        "entries": con.execute(
            "SELECT id,title,status,substr(rawTranscript,1,80) FROM entries"
        ).fetchall(),
        "ledger": con.execute(
            "SELECT requestId,status,lastError,rawTranscript FROM capture_ledger"
        ).fetchall(),
    }
    con.close()
    (OUT / f"{tag}-db.json").write_text(
        json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding="utf-8"
    )
    print(tag, "nb", len(data["notebooks"]), "e", len(data["entries"]), "l", len(data["ledger"]))
    for row in data["ledger"]:
        print("  ledger", row)
    return data


def send_text(text: str) -> str:
    r = adb(
        "shell",
        "am",
        "start",
        "-a",
        "android.intent.action.SEND",
        "-t",
        "text/plain",
        "--es",
        "android.intent.extra.TEXT",
        text,
        "-n",
        ACT,
    )
    out = (r.stdout or b"").decode("utf-8", "replace") + (r.stderr or b"").decode("utf-8", "replace")
    print("am start", out.strip()[:300], "rc", r.returncode)
    return out


def shot(stem: str) -> None:
    adb("shell", "screencap", "-p", "/sdcard/Download/yuyue-e2e.png")
    dest = OUT / f"{stem}.png"
    adb("pull", "/sdcard/Download/yuyue-e2e.png", str(dest))
    print("shot", dest.name, dest.stat().st_size if dest.exists() else 0)


def has_text(data: dict, needle: str) -> bool:
    blob = json.dumps(data, ensure_ascii=False)
    return needle in blob


def write_case(row: dict) -> None:
    with RESULTS.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(row, ensure_ascii=False) + "\n")
    print("RESULT", row["case_id"], row["status"])


def main() -> None:
    adb("shell", "svc", "power", "stayon", "usb")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    time.sleep(0.4)
    baseline = pull_db("share-baseline")
    adb("shell", "am", "force-stop", PKG)
    time.sleep(0.8)
    send_text(COLD)
    time.sleep(12)
    shot("share-cold")
    cold = pull_db("share-cold")
    cold_ok = has_text(cold, COLD) and len(cold["ledger"]) > len(baseline["ledger"])

    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1.0)
    send_text(HOT)
    time.sleep(12)
    shot("share-hot")
    hot = pull_db("share-hot")
    hot_ok = has_text(hot, HOT) and len(hot["ledger"]) > len(cold["ledger"])

    status = "PASS" if cold_ok and hot_ok else "FAIL"
    write_case(
        {
            "case_id": "FI-ENTRY-002",
            "status": status,
            "command": "cold+hot ACTION_SEND after SharedTextIntake",
            "evidence": "share-cold-db.json;share-hot-db.json;share-cold.png;share-hot.png",
            "notes": f"cold_ok={cold_ok} hot_ok={hot_ok} baseline_l={len(baseline['ledger'])} cold_l={len(cold['ledger'])} hot_l={len(hot['ledger'])}",
        }
    )


if __name__ == "__main__":
    main()
