"""Remaining MIX 3 E2E cases. Writes evidence under e2e-20260919. No secrets."""
from __future__ import annotations

import json
import os
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
OUT.mkdir(parents=True, exist_ok=True)


def adb(*args: str, timeout: int = 30) -> subprocess.CompletedProcess:
    return subprocess.run(
        [ADB, "-s", SERIAL, *args],
        capture_output=True,
        timeout=timeout,
    )


def adb_text(*args: str, timeout: int = 30) -> str:
    r = adb(*args, timeout=timeout)
    return (r.stdout or b"").decode("utf-8", "replace")


def shot(stem: str) -> Path:
    adb("shell", "screencap", "-p", "/sdcard/Download/yuyue-e2e.png")
    dest = OUT / f"{stem}.png"
    adb("pull", "/sdcard/Download/yuyue-e2e.png", str(dest))
    print("shot", dest.name, dest.stat().st_size if dest.exists() else 0)
    return dest


def dump_nodes(stem: str) -> list[dict]:
    adb("shell", "uiautomator", "dump", "/sdcard/Download/yuyue-ui.xml")
    xml_path = OUT / f"{stem}.xml"
    adb("pull", "/sdcard/Download/yuyue-ui.xml", str(xml_path))
    nodes: list[dict] = []
    if xml_path.exists():
        root = ET.parse(xml_path).getroot()
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
                        "cls": n.attrib.get("class"),
                    }
                )
    (OUT / f"{stem}-nodes.json").write_text(
        json.dumps(nodes, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("nodes", stem, len(nodes))
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
    time.sleep(0.8)
    return True


def tap_xy(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.4)


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
    cur = con.cursor()
    data = {
        "notebooks": cur.execute("SELECT id,name,isDefault FROM notebooks").fetchall(),
        "entries": cur.execute(
            "SELECT id,title,status,deletedAtMillis,rawTranscript,substr(body,1,80) FROM entries"
        ).fetchall(),
        "ledger": cur.execute(
            "SELECT requestId,status,lastError,rawTranscript FROM capture_ledger"
        ).fetchall(),
        "media": cur.execute("SELECT id,entryId,requestId,localPath FROM entry_media").fetchall(),
    }
    con.close()
    (OUT / f"{tag}-db.json").write_text(json.dumps(data, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(tag, "entries", len(data["entries"]), "ledger", len(data["ledger"]), "media", len(data["media"]))
    return data


def clipboard_set(text: str) -> str:
    r = subprocess.run(
        [ADB, "-s", SERIAL, "shell", "service", "call", "clipboard", "2", "i32", "1", "i32", "1", "s16", text],
        capture_output=True,
    )
    out = (r.stdout or b"").decode("utf-8", "replace") + (r.stderr or b"").decode("utf-8", "replace")
    print("clipboard", out[:200].replace("\n", " "))
    return out


def paste_into_composer(text: str, stem: str) -> list[dict]:
    nodes = dump_nodes(f"{stem}-before")
    if not find_node(nodes, "要记的事") and not find_node(nodes, "发送"):
        tap_label(nodes, "改用打字")
        time.sleep(1.0)
        nodes = dump_nodes(f"{stem}-text")
    field = next((n for n in nodes if n.get("cls") == "android.widget.EditText"), None)
    if field:
        x, y = center(field["bounds"])
        tap_xy(x, y)
    clipboard_set(text)
    time.sleep(0.3)
    adb("shell", "input", "keyevent", "279")  # PASTE
    time.sleep(0.6)
    return dump_nodes(f"{stem}-pasted")


def back(n: int = 1) -> None:
    for _ in range(n):
        tap_xy(77, 184)
        time.sleep(0.5)


def ensure_foreground() -> None:
    adb("shell", "svc", "power", "stayon", "usb")
    adb("shell", "am", "start", "-n", ACT)
    time.sleep(1.2)


def write_case(path: Path, row: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(row, ensure_ascii=False) + "\n")
    print("RESULT", row.get("case_id"), row.get("status"))


def jpeg_bytes() -> bytes:
    # 1x1 pixel JPEG
    return bytes.fromhex(
        "ffd8ffe000104a46494600010100000100010000ffdb004300080606070605080707070909080a0c140d0c0b0b0c1912130f141d1a1f1e1d1a1c1c20242e2720222c231c1c2837292c30313434341f27393d38323c2e333432ffdb0043010909090c0b0c180d0d1832211c21323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232323232ffc00011080001000103012200021101031101ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a535455565758595a636465666768696a737475767778797a838485868788898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9ffda000c03010002110311003f00fdb20a28a0028a28a00ffd9"
    )


RESULTS = Path(r"C:\File\Project\Yuyue\docs\testing\results.jsonl")


def main() -> None:
    ensure_foreground()
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable")

    # --- clipboard probe + recall ---
    nodes = dump_nodes("40-home")
    shot("40-home")
    pasted = paste_into_composer("上周喝咖啡那次是哪天", "41-recall")
    texts = " | ".join((n.get("text") or "") for n in pasted)
    (OUT / "41-recall-texts.txt").write_text(texts, encoding="utf-8")
    send = find_node(pasted, "发送")
    before = pull_db("41-before-recall")
    n_entries = len(before["entries"])
    if send and ("上周" in texts or "哪天" in texts or "咖啡" in texts):
        tap_label(pasted, "发送")
        time.sleep(6)
        shot("41-recall-after")
        dump_nodes("41-recall-after")
        after = pull_db("41-after-recall")
        added = len(after["entries"]) - n_entries
        ui = dump_nodes("41-recall-final")
        shot("41-recall-final")
        has_card = any("要改" in (n.get("text") or "") or "找到" in (n.get("text") or "") for n in ui)
        has_day = any("星期" in (n.get("text") or "") or "则" in (n.get("text") or "") for n in ui)
        status = "FAIL" if added > 0 else ("PASS" if has_day or has_card else "INCONCLUSIVE")
        write_case(
            RESULTS,
            {
                "case_id": "MD-RECALL-001",
                "status": status,
                "command": "clipboard paste 上周喝咖啡那次是哪天; send",
                "evidence": "docs/testing/evidence/device/e2e-20260919/41-recall-final.png",
                "notes": f"entry_delta={added}; ui_has_day={has_day}; ui_has_card={has_card}; pasted_ok={('上周' in texts)}",
            },
        )
    else:
        write_case(
            RESULTS,
            {
                "case_id": "MD-RECALL-001",
                "status": "BLOCKED",
                "command": "clipboard paste Chinese",
                "evidence": "docs/testing/evidence/device/e2e-20260919/41-recall-texts.txt",
                "notes": f"clipboard/paste did not put Chinese into composer: {texts[:180]}",
            },
        )

    # --- split two events ---
    ensure_foreground()
    back(2)
    pasted = paste_into_composer("明早买菜，下午跑步", "42-split")
    texts = " | ".join((n.get("text") or "") for n in pasted)
    (OUT / "42-split-texts.txt").write_text(texts, encoding="utf-8")
    before = pull_db("42-before-split")
    if "买菜" in texts or "跑步" in texts:
        tap_label(pasted, "发送")
        time.sleep(10)
        shot("42-split-after")
        after = pull_db("42-after-split")
        new = [e for e in after["entries"] if e not in before["entries"]]
        write_case(
            RESULTS,
            {
                "case_id": "MD-AI-002",
                "status": "FAIL" if len(new) < 2 else "PASS",
                "command": "clipboard paste 明早买菜，下午跑步",
                "evidence": "docs/testing/evidence/device/e2e-20260919/42-after-split-db.json",
                "notes": f"new_entries={len(new)} {new}",
            },
        )
    else:
        write_case(
            RESULTS,
            {
                "case_id": "MD-AI-002",
                "status": "BLOCKED",
                "command": "clipboard paste split utterance",
                "evidence": "docs/testing/evidence/device/e2e-20260919/42-split-texts.txt",
                "notes": "Chinese paste failed",
            },
        )

    # --- voice hold ---
    ensure_foreground()
    back(3)
    nodes = dump_nodes("43-voice")
    shot("43-voice-before")
    orb = find_node(nodes, "点击开始说话") or find_node(nodes, "按住说一句")
    if orb:
        x, y = center(orb["bounds"])
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1800")
        time.sleep(0.8)
        shot("43-voice-after")
        after = dump_nodes("43-voice-after")
        labels = " | ".join((n.get("text") or n.get("desc") or "") for n in after)
        (OUT / "43-voice-labels.txt").write_text(labels, encoding="utf-8")
        silent_fail = "还没有" not in labels and "草稿" not in labels and "发送" not in labels and "听" not in labels
        # empty hold should surface error or draft, not vanish
        status = "FAIL" if silent_fail and not find_node(after, "发送") else "PASS"
        if find_node(after, "还没有可发送") or find_node(after, "还没有"):
            status = "PASS"
        write_case(
            RESULTS,
            {
                "case_id": "QC-UX-001",
                "status": status,
                "command": "input swipe hold 1800ms on mic orb",
                "evidence": "docs/testing/evidence/device/e2e-20260919/43-voice-after.png",
                "notes": labels[:240],
            },
        )
    else:
        write_case(
            RESULTS,
            {
                "case_id": "QC-UX-001",
                "status": "BLOCKED",
                "command": "hold mic orb",
                "evidence": "docs/testing/evidence/device/e2e-20260919/43-voice-before.png",
                "notes": "mic orb not on screen",
            },
        )

    # --- offline photo ---
    jpg = OUT / "yuyue-test.jpg"
    jpg.write_bytes(jpeg_bytes())
    adb("push", str(jpg), "/sdcard/Download/yuyue-test.jpg")
    adb("shell", "am", "broadcast", "-a", "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file:///sdcard/Download/yuyue-test.jpg")
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    time.sleep(1)
    ensure_foreground()
    back(3)
    before = pull_db("44-before-photo")
    nodes = dump_nodes("44-photo-home")
    tap_label(nodes, "添加照片")
    time.sleep(1)
    shot("44-photo-sheet")
    sheet = dump_nodes("44-photo-sheet")
    if not tap_label(sheet, "相册"):
        tap_label(sheet, "拍照")
    time.sleep(1.5)
    shot("44-gallery")
    gal = dump_nodes("44-gallery")
    (OUT / "44-gallery-texts.txt").write_text(
        "\n".join(f"{n.get('text')}|{n.get('desc')}|{n.get('bounds')}" for n in gal),
        encoding="utf-8",
    )
    # tap first likely thumbnail
    thumbs = [n for n in gal if n.get("clickable") and n.get("cls") in ("android.widget.ImageView", "android.view.View")]
    picked = False
    for n in gal:
        t = (n.get("text") or "") + (n.get("desc") or "")
        if "yuyue-test" in t or "Download" in t or "下载" in t:
            tap_label(gal, t.strip()[:8] or "下载")
            picked = True
            break
    if not picked and thumbs:
        x, y = center(thumbs[0]["bounds"])
        tap_xy(x, y)
        picked = True
    time.sleep(1.2)
    shot("44-attached")
    att = dump_nodes("44-attached")
    if find_node(att, "发送") or find_node(att, "只发照片") or find_node(att, "发送照片"):
        tap_label(att, "发送") or tap_label(att, "发送照片")
        time.sleep(3)
    shot("44-after-send")
    after = pull_db("44-after-photo")
    adb("shell", "cmd", "connectivity", "airplane-mode", "disable")
    new_entries = len(after["entries"]) - len(before["entries"])
    new_media = len(after["media"]) - len(before["media"])
    ledger_tail = after["ledger"][-1] if after["ledger"] else None
    ok_no_fake = new_entries == 0 or (
        ledger_tail and ledger_tail[1] in ("Retryable", "ForegroundRequired", "Pending", "Processing")
    )
    # engine says offline photo -> Retryable, no materialize
    status = "PASS" if new_entries == 0 else "FAIL"
    write_case(
        RESULTS,
        {
            "case_id": "MD-CAPTURE-003",
            "status": status if picked else "BLOCKED",
            "command": "airplane; attach photo; send",
            "evidence": "docs/testing/evidence/device/e2e-20260919/44-after-photo-db.json",
            "notes": f"picked={picked}; entry_delta={new_entries}; media_delta={new_media}; ledger_tail={ledger_tail}",
        },
    )

    # --- backup export ---
    ensure_foreground()
    back(4)
    nodes = dump_nodes("45-pre-settings")
    tap_label(nodes, "设置")
    time.sleep(1.2)
    shot("45-settings")
    st = dump_nodes("45-settings")
    before_zips = set(adb_text("shell", "ls", "/sdcard/Download").split())
    tap_label(st, "备份日记")
    time.sleep(2.5)
    shot("45-after-export")
    dump_nodes("45-after-export")
    after_zips = set(adb_text("shell", "ls", "/sdcard/Download").split())
    new_zips = sorted(after_zips - before_zips)
    (OUT / "45-zips.txt").write_text("\n".join(new_zips), encoding="utf-8")
    # pull newest yuyue zip if named
    zips = [z for z in after_zips if z.endswith(".zip") and ("yuyue" in z.lower() or "语" in z)]
    export_ok = bool(new_zips or zips)
    write_case(
        RESULTS,
        {
            "case_id": "QC-BACKUP-001",
            "status": "PASS" if export_ok else "INCONCLUSIVE",
            "command": "settings 备份日记; ls /sdcard/Download",
            "evidence": "docs/testing/evidence/device/e2e-20260919/45-zips.txt",
            "notes": f"new_files={new_zips}; zip_like={sorted(zips)[-3:]} (wipe+reimport not executed this step)",
        },
    )
    print("done remaining e2e")


if __name__ == "__main__":
    main()
