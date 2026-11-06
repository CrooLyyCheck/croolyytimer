#!/usr/bin/env python3
"""Prepare a bounded update catalog from a verified APK. Does not publish anything."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
MAX_APK = 128 * 1024 * 1024

def descriptor(version_code, version_name, variant, size, digest, signers, min_sdk, notes):
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:-[A-Za-z0-9.-]+)?", version_name):
        raise ValueError("Nieprawidłowy numer wersji.")
    if version_code < 1 or not 0 < size <= MAX_APK or min_sdk < 26:
        raise ValueError("Nieprawidłowe parametry APK.")
    if not re.fullmatch(r"[a-f0-9]{64}", digest) or not signers or any(not re.fullmatch(r"[a-f0-9]{64}", v) for v in signers):
        raise ValueError("Nieprawidłowe sumy kontrolne.")
    return dict(versionCode=version_code, versionName=version_name, tag=f"v{version_name}",
                apkName=f"CrooLyyTimer-{version_name}-{'debug' if variant == 'debug' else 'release'}.apk",
                sha256=digest, size=size, minSdk=min_sdk, signerSha256=sorted(signers), notes=notes[:4000])

def main():
    parser = argparse.ArgumentParser(description="Przygotuj APK, sumę kontrolną i updates.json do publikacji.")
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--channel", choices=("preview", "stable"), default="preview")
    parser.add_argument("--notes", default="Nowa wersja CrooLyyTimer.")
    args = parser.parse_args()
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        for line in (ROOT / "local.properties").read_text().splitlines():
            if line.startswith("sdk.dir="): sdk = line.split("=", 1)[1].replace("\\:", ":").replace("\\\\", "\\")
    tools = sorted((Path(sdk) / "build-tools").iterdir(), key=lambda p: [int(n) for n in re.findall(r"\d+", p.name)])[-1]
    java = Path(os.environ["JAVA_HOME"]) / "bin" / ("java.exe" if os.name == "nt" else "java")
    def run(command): return subprocess.check_output([str(x) for x in command], text=True, encoding="utf-8", errors="replace")
    badging = run([tools / ("aapt.exe" if os.name == "nt" else "aapt"), "dump", "badging", args.apk])
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not package or package[1] != "pl.croolyy.timer": raise ValueError("To nie jest APK CrooLyyTimer.")
    signing = run([java, "-jar", tools / "lib/apksigner.jar", "verify", "--print-certs", args.apk])
    signers = {v.lower() for v in re.findall(r"Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]{64})", signing)}
    size = args.apk.stat().st_size
    if size > MAX_APK: raise ValueError("APK przekracza limit 128 MiB.")
    with args.apk.open("rb") as source: digest = hashlib.file_digest(source, "sha256").hexdigest()
    variant = "debug" if "CN=Android Debug" in signing else "release"
    release = descriptor(int(package[2]), package[3], variant, size, digest, signers,
                         int(re.search(r"sdkVersion:'(\d+)'", badging)[1]), args.notes)
    catalog_path = ROOT / "updates.json"
    catalog = json.loads(catalog_path.read_text()) if catalog_path.exists() else dict(schemaVersion=1, stable=None, preview=None)
    existing = catalog.get(args.channel)
    if existing and existing["versionCode"] > release["versionCode"]: raise ValueError("Nie można opublikować starszego numeru wersji.")
    artifact = ROOT / "artifacts" / release["apkName"]
    artifact.parent.mkdir(exist_ok=True)
    if artifact.resolve() != args.apk.resolve(): shutil.copyfile(args.apk, artifact)
    artifact.with_suffix(".apk.sha256").write_text(f"{digest}  {artifact.name}\n", encoding="utf-8")
    catalog[args.channel] = release
    catalog_path.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Przygotowano {artifact.name} i updates.json. Najpierw wyślij APK do wydania {release['tag']}, potem opublikuj updates.json.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
