"""Device retest after quality fixes: QUICK_CAPTURE + empty voice."""
from __future__ import annotations

import json
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
                    }
                )
    (OUT / f"{stem}-nodes.json").write_text(
        json.dumps(nodes, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    labels = [n.get("text") or n.get("desc") for n in nodes if (n.get("text") or n.get("desc"))]
    print("nodes", stem, labels[:16])
    return nodes


def labels_of(nodes: list[dict]) -> str:
    return " | ".join((n.get("text") or n.get("desc") or "") for n in nodes)


def find_node(nodes: list[dict], needle: str) -> dict | None:
    for n in nodes:
        if needle in (n.get("text") or "") or needle in (n.get("desc") or ""):
            return n
    return None


def center(bounds: str) -> tuple[int, int]:
    a, b = bounds[1:-1].split("][")
    x1, y1 = map(int, a.split(","))
    x2, y2 = map(int, b.split(","))
    return (x1 + x2) // 2, (y1 + y2) // 2


def write_case(row: dict) -> None:
    with RESULTS.open("a", encoding="utf-8") as fh:
        fh.write(json.dumps(row, ensure_ascii=False) + "\n")
    print("RESULT", row["case_id"], row["status"])


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    adb("shell", "svc", "power", "stayon", "usb")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    time.sleep(0.4)

    adb("shell", "am", "force-stop", PKG)
    time.sleep(0.6)
    r = adb(
        "shell",
        "am",
        "start",
        "-a",
        "com.yuyue.app.action.QUICK_CAPTURE",
        "-n",
        ACT,
    )
    print("quick", (r.stdout or b"").decode("utf-8", "replace").strip()[:200])
    time.sleep(3.5)
    shot("fix-quick")
    nodes = dump_nodes("fix-quick")
    blob = labels_of(nodes)
    quick_ok = any(k in blob for k in ("要记的事", "发送", "改回语音"))
    write_case(
        {
            "case_id": "FI-ENTRY-001",
            "status": "PASS" if quick_ok else "FAIL",
            "command": "am start QUICK_CAPTURE",
            "evidence": "fix-quick.png;fix-quick-nodes.json",
            "notes": blob[:240],
        }
    )

    # Switch back to voice if composer is open, then hold empty.
    back = find_node(nodes, "改回语音")
    if back and back.get("bounds"):
        x, y = center(back["bounds"])
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(1.4)
    nodes = dump_nodes("fix-voice-home")
    shot("fix-voice-home")
    orb = (
        find_node(nodes, "按住说话")
        or find_node(nodes, "按住说一句")
        or find_node(nodes, "点击开始说话")
    )
    if orb and orb.get("bounds"):
        x, y = center(orb["bounds"])
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1800")
        time.sleep(1.2)
    shot("fix-voice-empty")
    after = dump_nodes("fix-voice-empty")
    after_blob = labels_of(after)
    voice_ok = any(k in after_blob for k in ("没听清", "没有听到声音", "几乎没录到声音"))
    write_case(
        {
            "case_id": "QC-UX-001",
            "status": "PASS" if voice_ok else "FAIL",
            "command": "hold orb 1.8s empty",
            "evidence": "fix-voice-empty.png;fix-voice-empty-nodes.json",
            "notes": after_blob[:240],
        }
    )


if __name__ == "__main__":
    main()
