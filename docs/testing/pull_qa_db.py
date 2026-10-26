import json
import os
import sqlite3
import subprocess
import sys

ADB = r"C:\Android\Sdk\platform-tools\adb.exe"
SERIAL = "45c5974e"
PKG = "com.yuyue.app.qa"
OUT = r"C:\File\Project\Yuyue\docs\testing\evidence\device\e2e-20260919"


def adb(*args: str, capture: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        [ADB, "-s", SERIAL, *args],
        check=False,
        capture_output=capture,
        text=True if capture else False,
    )


def pull_db() -> sqlite3.Connection:
    os.makedirs(OUT, exist_ok=True)
    for name in ("yuyue.db", "yuyue.db-wal", "yuyue.db-shm"):
        dest = os.path.join(OUT, name)
        with open(dest, "wb") as fh:
            proc = subprocess.run(
                [ADB, "-s", SERIAL, "exec-out", "run-as", PKG, "cat", f"databases/{name}"],
                check=True,
                stdout=fh,
            )
        print(f"{name} {os.path.getsize(dest)}")
    return sqlite3.connect(os.path.join(OUT, "yuyue.db"))


def dump_ui(stem: str) -> None:
    adb("shell", "screencap", "-p", "/sdcard/Download/yuyue-e2e.png")
    adb("pull", "/sdcard/Download/yuyue-e2e.png", os.path.join(OUT, f"{stem}.png"))
    adb("shell", "uiautomator", "dump", "/sdcard/Download/yuyue-ui.xml")
    adb("pull", "/sdcard/Download/yuyue-ui.xml", os.path.join(OUT, f"{stem}.xml"))
    xml_path = os.path.join(OUT, f"{stem}.xml")
    if not os.path.exists(xml_path):
        print("no ui xml")
        return
    import xml.etree.ElementTree as ET

    root = ET.parse(xml_path).getroot()
    nodes = []
    for n in root.iter("node"):
        text = n.attrib.get("text") or ""
        desc = n.attrib.get("content-desc") or ""
        clickable = n.attrib.get("clickable") == "true"
        if text or desc or clickable:
            nodes.append(
                {
                    "text": text,
                    "desc": desc,
                    "clickable": clickable,
                    "bounds": n.attrib.get("bounds"),
                    "pkg": n.attrib.get("package"),
                    "cls": n.attrib.get("class"),
                    "rid": n.attrib.get("resource-id"),
                }
            )
    nodes_path = os.path.join(OUT, f"{stem}-nodes.json")
    with open(nodes_path, "w", encoding="utf-8") as fh:
        json.dump(nodes, fh, ensure_ascii=False, indent=2)
    print(f"nodes {len(nodes)} -> {nodes_path}")
    for n in nodes:
        label = n.get("text") or n.get("desc") or n.get("cls")
        print(f"{n.get('bounds')}\tclick={n.get('clickable')}\t{label.encode('unicode_escape').decode()}")


def summarize(con: sqlite3.Connection) -> None:
    cur = con.cursor()
    tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY 1")]
    print("tables", tables)
    for table in ("notebooks", "entries", "capture_ledger", "entry_media"):
        if table in tables:
            n = cur.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            print(f"count {table}={n}")
    if "notebooks" in tables:
        print("notebooks", cur.execute("SELECT id, name, isDefault FROM notebooks").fetchall())
    if "entries" in tables:
        print(
            "entries",
            cur.execute(
                "SELECT id, title, status, deletedAtMillis, substr(body,1,40), substr(rawTranscript,1,40) FROM entries"
            ).fetchall(),
        )
    if "capture_ledger" in tables:
        print(
            "ledger",
            cur.execute(
                "SELECT requestId, status, lastError, substr(rawTranscript,1,60) FROM capture_ledger"
            ).fetchall(),
        )


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "db"
    if cmd == "ui":
        dump_ui(sys.argv[2] if len(sys.argv) > 2 else "ui")
    elif cmd == "db":
        summarize(pull_db())
    else:
        raise SystemExit(f"unknown {cmd}")
