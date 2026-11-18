"""Exercise the installed debug APK on an Android emulator across three window sizes."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

out = Path(os.environ.get("FOLD_SCREENSHOTS", "fold-screens"))
out.mkdir(exist_ok=True)
package = "com.gomelev.app.debug"


def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=45)


def shell(*args):
    return adb("shell", *args).decode().strip()


def nodes():
    shell("uiautomator", "dump", "/sdcard/fold-ui.xml")
    return list(ET.fromstring(adb("exec-out", "cat", "/sdcard/fold-ui.xml")).iter("node"))


def find(label, attempts=5, scroll=False):
    for _ in range(attempts):
        for node in nodes():
            if label in (node.get("text"), node.get("content-desc")):
                return node
        if scroll:
            shell("input", "swipe", "180", "690", "180", "280", "350")
        time.sleep(.5)
    raise AssertionError(f"Missing {label}")


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    shell("input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def capture(name):
    data = adb("exec-out", "screencap", "-p")
    assert data.startswith(b"\x89PNG")
    (out / (name + ".png")).write_bytes(data)


try:
    shell("svc", "wifi", "disable")
    shell("svc", "data", "disable")
    shell("wm", "density", "160")
    shell("wm", "size", "360x820")
    shell("am", "start", "-W", "-n", package + "/com.gomelev.app.MainActivity")
    find("Электромобили для доставки")
    capture("01-cover")
    tap(find("FAW Bestune Xiaoma 222", scroll=True))
    find("Назад к каталогу")
    capture("02-selected")
    # Fold8 open in its wider natural orientation; 1/3 of 2448 x 1848.
    shell("wm", "size", "816x616")
    time.sleep(1)
    shell("input", "swipe", "180", "230", "180", "550", "250")
    find("Электромобили для доставки")
    matches = [n for n in nodes() if n.get("text") == "FAW Bestune Xiaoma 222"]
    assert any(int(re.findall(r"\d+", n.get("bounds"))[0]) > 390 for n in matches), "detail pane missing"
    capture("03-unfolded")
    # Fold8 cover has the 1248:1972 aspect ratio.
    shell("wm", "size", "416x657")
    find("Назад к каталогу")
    find("FAW Bestune Xiaoma 222")
    capture("04-refolded")
    shell("wm", "size", "400x840")
    find("Назад к каталогу")
    shell("input", "keyevent", "4")
    time.sleep(.7)
    shell("input", "swipe", "180", "230", "180", "650", "250")
    find("Электромобили для доставки")
    capture("05-phone")
except Exception:
    capture("failure")
    print([(n.get("text"), n.get("content-desc"), n.get("bounds")) for n in nodes()
           if n.get("text") or n.get("content-desc")], flush=True)
    raise
finally:
    shell("wm", "size", "reset")
    shell("wm", "density", "reset")
    shell("svc", "wifi", "enable")
    shell("svc", "data", "enable")
