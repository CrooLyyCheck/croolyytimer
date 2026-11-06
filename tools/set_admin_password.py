#!/usr/bin/env python3
"""Rotate only the verifier; never accepts passwords via command-line arguments."""
import argparse
import base64
import getpass
import json
from pathlib import Path
import secrets
import sys
import shutil
import subprocess
from argon2.low_level import hash_secret_raw, Type

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "app/src/main/assets/admin_verifier.json"

def clipboard_command():
    for command in (["clip.exe"],) if sys.platform == "win32" else ((["pbcopy"],) if sys.platform == "darwin" else (["wl-copy"], ["xclip", "-selection", "clipboard"])):
        if shutil.which(command[0]):
            return command
    raise RuntimeError("Brak narzędzia schowka. Wygeneruj hasło w menedżerze i użyj trybu domyślnego.")

def main():
    parser = argparse.ArgumentParser(description="Ustaw lokalne hasło administratora CrooLyyTimer.")
    parser.add_argument("--generate", action="store_true", help="Wygeneruj losowe hasło; wymaga interaktywnego terminala.")
    parser.add_argument("--initialize", action="store_true", help="Pierwsza instalacja: losowy weryfikator bez ujawniania hasła.")
    args = parser.parse_args()
    if args.initialize:
        if TARGET.exists():
            parser.error("Weryfikator już istnieje. Użyj trybu interaktywnego do zmiany hasła.")
        password = secrets.token_urlsafe(32)
    elif args.generate:
        if not sys.stdout.isatty() or not sys.stdin.isatty():
            parser.error("Hasła nie wolno zapisywać w logach. Uruchom skrypt w prywatnym terminalu.")
        password = secrets.token_urlsafe(32)
        command = clipboard_command()
        subprocess.run(command, input=password, text=True, check=True, stdout=subprocess.DEVNULL)
        print("Losowe hasło skopiowano do schowka. Wklej je teraz do menedżera haseł.")
        try:
            if input("Wpisz ZAPISANE, aby zapisać nowy weryfikator i wyczyścić schowek: ") != "ZAPISANE":
                return 1
        finally:
            subprocess.run(command, input="", text=True, check=True, stdout=subprocess.DEVNULL)
    else:
        if not sys.stdin.isatty():
            parser.error("Wymagany terminal interaktywny; nie podawaj hasła przez potok.")
        password = getpass.getpass("Nowe hasło administratora (min. 20 znaków): ")
        if len(password) < 20:
            parser.error("Hasło musi mieć co najmniej 20 znaków.")
        if not secrets.compare_digest(password.encode(), getpass.getpass("Powtórz hasło: ").encode()):
            parser.error("Hasła są różne.")
    salt = secrets.token_bytes(16)
    digest = hash_secret_raw(password.encode("utf-8"), salt, 3, 65536, 1, 32, Type.ID, version=19)
    verifier = dict(salt=base64.b64encode(salt).decode(), hash=base64.b64encode(digest).decode(),
                    memoryKiB=65536, iterations=3, parallelism=1, version=19)
    TARGET.parent.mkdir(parents=True, exist_ok=True)
    TARGET.write_text(json.dumps(verifier, indent=2) + "\n", encoding="utf-8")
    print("Zapisano weryfikator Argon2id. Przebuduj aplikację i wydaj aktualizację.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
