# CrooLyyTimer 1.0.0 — pierwsze wydanie testowe

Natywna aplikacja Android 8.0+ pod marką **CrooLyyTimer — Twój czas między stronami**.
Prywatna biblioteka książek, własne okładki, wiele timerów na książkę, historia i postępy,
niezależne pytania o strony, jasny i ciemny motyw, pełne kopie ZIP z opcjonalnym AES-256.

Zwykły timer jest domyślnie zablokowany. Hasło `takchce` odblokowuje go na 168 godzin.
Trwałe odblokowanie administratora korzysta z Argon2id; hasło właściciela ustawia się skryptem,
bez zapisywania go w repozytorium. Instrukcje są w README.

Do instalacji pobierz **CrooLyyTimer-1.0.0-debug.apk**. Jest podpisany kluczem debug;
wydanie do dystrybucji docelowej powinno być podpisane własnym keystore właściciela.
W przyszłych aktualizacjach zachowaj ten sam podpis albo wcześniej wykonaj kopię ZIP.

Weryfikacja lokalna: 24 testy Kotlin/Robolectric i 1 test Python, lint bez błędów,
kompilacja debug, release z R8 oraz APK testów urządzeniowych.
Projekt nie korzysta z GitHub Actions.

Google Drive wymaga konfiguracji OAuth i nie był jeszcze sprawdzony na koncie właściciela.
Końcowe testy aparatu, selektorów plików, wygaszenia ekranu i pełnego przebiegu na telefonie
pozostają do wykonania. Szczegółowy, uczciwy zakres w `docs/VALIDATION.md`.
