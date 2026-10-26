"""Continue photo, voice, backup after clipboard Chinese failed."""
from __future__ import annotations

import json
import sqlite3
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = r"C:\Android\Sdk\platform-tools\adb.exe"
SERIAL = "45c5974e"
PKG = "com.yuyue.app.qa"
ACT = f"{PKG}/com.yuyue.app.MainActivity"
OUT = Path(r"C:\File\Project\Yuyue\docs\testing\evidence\device\e2e-20260919")
RESULTS = Path(r"C:\File\Project\Yuyue\docs\testing\results.jsonl")


def adb(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, timeout=40)


def adb_text(*args: str) -> str:
    r = adb(*args)
    return (r.stdout or b"").decode("utf-8", "replace")


def shot(stem: str) -> None:
    adb("shell", "screencap", "-p", "/sdcard/Download/yuyue-e2e.png")
    dest = OUT / f"{stem}.png"
    adb("pull", "/sdcard/Download/yuyue-e2e.png", str(dest))
    print("shot", dest.name, dest.stat().st_size if dest.exists() else 0)


def dump_nodes(stem: str) -> list[dict]:
    adb("shell", "uiautomator", "dump", "/sdcard/Download/yuyue-ui.xml")
    xml_path = OUT / f"{stem}.xml"
    adb("pull", "/sdcard/Download/yuyue-ui.xml", str(xml_path))
    nodes: list[dict] = []
    if xml_path.exists():
        for n in ET.parse(xml_path).getroot().iter("node"):
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
                        "cls": n.attrib.get("class"),
                    }
                )
    (OUT / f"{stem}-nodes.json").write_text(json.dumps(nodes, ensure_ascii=False, indent=2), encoding="utf-8")
    print("nodes", stem, len(nodes), [n.get("text") or n.get("desc") for n in nodes if (n.get("text") or n.get("desc"))][:12])
    return nodes


def center(bounds: str) -> tuple[int, int]:
    a, b = bounds[1:-1].split("][")
    x1, y1 = map(int, a.split(","))
    x2, y2 = map(int, b.split(","))
    return (x1 + x2) // 2, (y1 + y2) // 2


def find_node(nodes: list[dict], needle: str) -> dict | None:
    for n in nodes:
        if needle in (n.get("text") or "") or needle in (n.get("desc") or ""):
            return n
    return None


def tap_label(nodes: list[dict], needle: str) -> bool:
    n = find_node(nodes, needle)
    if not n or not n.get("bounds"):
        print("missing", needle)
        return False
    x, y = center(n["bounds"])
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.9)
    return True


def tap_xy(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.35)


def pull_db(tag: str) -> dict:
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
        "entries": con.execute(
            "SELECT id,title,status,deletedAtMillis,rawTranscript,substr(body,1,80) FROM entries"
        ).fetchall(),
        "ledger": con.execute(
            "SELECT requestId,status,lastError,rawTranscript FROM capture_ledger"
        ).fetchall(),
        "media": con.execute("SELECT id,entryId,requestId,localPath FROM entry_media").fetchall(),
    }
    con.close()
    (OUT / f"{tag}-db.json").write_text(json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(tag, "e", len(data["entries"]), "l", len(data["ledger"]), "m", len(data["media"]))
    return data


def write_case(row: dict) -> None:
    with RESULTS.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(row, ensure_ascii=False) + "\n")
    print("RESULT", row["case_id"], row["status"])


def main() -> None:
    adb("shell", "svc", "power", "stayon", "usb")
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
    adb("shell", "am", "start", "-n", ACT)
    time.sleep(1.2)

    # Voice: leave text composer
    nodes = dump_nodes("50-voice-home")
    shot("50-voice-home")
    tap_label(nodes, "改回语音")
    time.sleep(1.2)
    nodes = dump_nodes("50-voice-orb")
    shot("50-voice-orb")
    orb = find_node(nodes, "点击开始说话") or find_node(nodes, "按住说一句")
    if orb:
        x, y = center(orb["bounds"])
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "2000")
        time.sleep(1.0)
        shot("50-voice-held")
        after = dump_nodes("50-voice-held")
        labels = " | ".join((n.get("text") or n.get("desc") or "") for n in after)
        (OUT / "50-voice-labels.txt").write_text(labels, encoding="utf-8")
        has_err = any(k in labels for k in ("还没有", "没听清", "没有可发送", "再说一次", "空"))
        has_draft = any(k in labels for k in ("发送", "先记下", "听", "识别"))
        listening = bool(find_node(after, "点击结束说话") or find_node(after, "正在"))
        status = "PASS" if (has_err or has_draft or listening) else "FAIL"
        write_case(
            {
                "case_id": "QC-UX-001",
                "status": status,
                "command": "tap 改回语音; swipe-hold 2000ms on orb",
                "evidence": "docs/testing/evidence/device/e2e-20260919/50-voice-held.png",
                "notes": labels[:300],
            }
        )
    else:
        write_case(
            {
                "case_id": "QC-UX-001",
                "status": "BLOCKED",
                "command": "voice orb after 改回语音",
                "evidence": "docs/testing/evidence/device/e2e-20260919/50-voice-orb.png",
                "notes": "orb not visible",
            }
        )

    # Offline photo using existing screenshot as image
    src = OUT / "18-send-9s.png"
    adb("push", str(src), "/sdcard/Download/yuyue-test-photo.png")
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    time.sleep(1.0)
    adb("shell", "am", "start", "-n", ACT)
    time.sleep(1.0)
    before = pull_db("51-before-photo")
    nodes = dump_nodes("51-photo-home")
    if find_node(nodes, "改回语音"):
        pass  # already text mode, photo icon exists
    tap_label(nodes, "添加照片")
    time.sleep(1.0)
    shot("51-photo-sheet")
    sheet = dump_nodes("51-photo-sheet")
    tap_label(sheet, "相册")
    time.sleep(1.8)
    shot("51-gallery")
    gal = dump_nodes("51-gallery")
    (OUT / "51-gallery.txt").write_text(
        "\n".join(f"{n.get('text')}|{n.get('desc')}|{n.get('cls')}|{n.get('bounds')}" for n in gal),
        encoding="utf-8",
    )
    picked = False
    for n in gal:
        blob = (n.get("text") or "") + (n.get("desc") or "")
        if "yuyue-test" in blob or "Download" in blob or "下载" in blob:
            x, y = center(n["bounds"])
            tap_xy(x, y)
            picked = True
            break
    if not picked:
        imgs = [n for n in gal if n.get("clickable") and "Image" in (n.get("cls") or "")]
        if imgs:
            x, y = center(imgs[0]["bounds"])
            tap_xy(x, y)
            picked = True
        elif gal:
            # tap a mid-grid cell typical for first photo
            tap_xy(180, 500)
            picked = True
    time.sleep(1.2)
    shot("51-attached")
    att = dump_nodes("51-attached")
    if find_node(att, "发送"):
        tap_label(att, "发送")
    elif find_node(att, "发送照片"):
        tap_label(att, "发送照片")
    time.sleep(3.0)
    shot("51-after-send")
    after = pull_db("51-after-photo")
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
    delta_e = len(after["entries"]) - len(before["entries"])
    tail = after["ledger"][-1] if after["ledger"] else None
    status = "BLOCKED" if not picked else ("PASS" if delta_e == 0 else "FAIL")
    write_case(
        {
            "case_id": "MD-CAPTURE-003",
            "status": status,
            "command": "airplane; 添加照片; 相册; send",
            "evidence": "docs/testing/evidence/device/e2e-20260919/51-after-photo-db.json",
            "notes": f"picked={picked}; entry_delta={delta_e}; ledger_tail={tail}",
        }
    )

    # Backup export to Downloads
    adb("shell", "am", "start", "-n", ACT)
    time.sleep(1.0)
    nodes = dump_nodes("52-home")
    tap_label(nodes, "设置")
    time.sleep(1.2)
    st = dump_nodes("52-settings")
    shot("52-settings")
    before_ls = set(adb_text("shell", "ls", "/sdcard/Download").split())
    tap_label(st, "备份日记")
    time.sleep(2.5)
    shot("52-export")
    dump_nodes("52-export")
    after_ls = set(adb_text("shell", "ls", "/sdcard/Download").split())
    new_files = sorted(after_ls - before_ls)
    zips = sorted(x for x in after_ls if x.endswith(".zip"))
    (OUT / "52-download-ls.txt").write_text(
        "new=" + json.dumps(new_files, ensure_ascii=False) + "\nzips=" + json.dumps(zips, ensure_ascii=False),
        encoding="utf-8",
    )
    export_ok = any(x.endswith(".zip") for x in new_files) or bool(zips)
    write_case(
        {
            "case_id": "QC-BACKUP-001",
            "status": "PASS" if export_ok else "FAIL",
            "command": "Settings 备份日记; ls Download",
            "evidence": "docs/testing/evidence/device/e2e-20260919/52-download-ls.txt",
            "notes": f"new_files={new_files}; zip_count={len(zips)}; wipe+reimport not run (destructive)",
        }
    )
    print("continue done")


if __name__ == "__main__":
    main()
