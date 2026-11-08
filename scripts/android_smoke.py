#!/usr/bin/env python3
"""Smoke funcional real no emulador Android (sem rede) para a beta nativa."""
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PKG = "br.com.tercocontemple.beta"
ACT = f"{PKG}/br.com.tercocontemple.MainActivity"


def adb(*args, check=True):
    cmd = ["adb", *args]
    r = subprocess.run(cmd, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    if check and r.returncode != 0:
        raise RuntimeError(f"{' '.join(cmd)} => {r.returncode}\n{r.stdout}")
    return r.stdout


def dump():
    err = None
    for _ in range(6):
        try:
            adb("shell", "uiautomator", "dump", "/sdcard/window.xml")
            raw = adb("shell", "cat", "/sdcard/window.xml")
            if "<?xml" in raw:
                return ET.fromstring(raw[raw.index("<?xml"):])
        except Exception as exc:
            err = exc
        time.sleep(2)
    raise AssertionError(f"Não foi possível acessar a interface de acessibilidade: {err}")


def find(part, root=None):
    root = root if root is not None else dump()
    return next((n for n in root.iter() if part.lower() in
                 (n.attrib.get("text", "") + " " + n.attrib.get("content-desc", "")).lower()), None)


def seek(part, max_scrolls=10):
    """Localizar o item inclusive depois de rolar a tela, como um usuário."""
    size = adb("shell", "wm", "size")
    match = re.search(r"(\d+)x(\d+)", size)
    width, height = (int(v) for v in match.groups()) if match else (1080, 1920)
    for attempt in range(max_scrolls + 1):
        root = dump()
        node = find(part, root)
        if node is not None:
            return node
        if attempt < max_scrolls:
            adb("shell", "input", "swipe", str(width//2), str(int(height*.73)),
                str(width//2), str(int(height*.30)), "480")
            time.sleep(0.8)
    visible = [n.attrib.get("text") for n in root.iter() if n.attrib.get("text")]
    raise AssertionError(f"'{part}' não localizado depois da rolagem; visíveis: {visible[:45]}")


def expect(part):
    seek(part)
    print("PASSOU:", part, flush=True)


def tap(part):
    node = seek(part)
    bounds = node.attrib.get("bounds", "")
    m = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if m is None:
        raise AssertionError(f"Sem coordenadas para '{part}': {bounds}")
    x1, y1, x2, y2 = (int(x) for x in m.groups())
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(2)


def screenshot(name):
    os.makedirs("qa-android", exist_ok=True)
    p = f"qa-android/{name}.png"
    with open(p,"wb") as f:
        r = subprocess.run(["adb","exec-out","screencap","-p"],stdout=f,check=True)
    print("Captura:",p,flush=True)


def run():
    apk="android-java/app/build/outputs/apk/debug/app-debug.apk"
    adb("install","-r",apk)
    adb("shell","svc","wifi","disable")
    adb("shell","svc","data","disable")
    adb("logcat","-c")
    adb("shell","am","start","-W","-n",ACT)
    time.sleep(6)
    expect("Um encontro diário")
    expect("MISTÉRIOS DE HOJE")
    screenshot("inicio-offline")
    tap("Rezar")
    expect("Meu Santo Terço")
    expect("Em nome do Pai")
    screenshot("primeira-oracao")
    tap("Ouvir em português")
    time.sleep(4)
    tap("Terminei esta oração")
    expect("Creio em Deus")
    adb("shell","am","force-stop",PKG)
    adb("shell","am","start","-W","-n",ACT)
    time.sleep(4)
    tap("Rezar")
    expect("Creio em Deus")
    screenshot("progresso-recuperado")
    adb("shell","am","force-stop",PKG)
    errors = adb("logcat","-d","-t","3000","-v","brief")
    fatal = [line for line in errors.splitlines()
             if "FATAL EXCEPTION" in line or "Unable to start activity" in line]
    if fatal: raise AssertionError("Falha AndroidRuntime: " + " | ".join(fatal[:5]))
    print("APROVADO: abertura, telas, áudio acionado, avanço e retomada no modo offline",flush=True)


if __name__ == "__main__":
    run()
