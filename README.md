<p align="center"><img src="docs/brand.svg" width="800" alt="CrooLyyTimer — Twój czas między stronami" /></p>

Natywna aplikacja Android do spokojnego mierzenia czasu czytania. Kotlin, Jetpack Compose,
Material 3, Room, DataStore, Coroutines/Flow i Navigation Compose. Cały interfejs po polsku.
Bez reklam, analityki i obowiązkowego konta. Android **8.0+ (API 26)**.

## Co działa

- Biblioteka: dodawanie, edycja, wyszukiwanie, ukończenie i usuwanie książek.
- Okładki z plików/galerii i aparatu, kopiowane do prywatnej pamięci; autorskie okładki zastępcze.
- Dowolna liczba nazwanych timerów na książkę, sumowana historia i statystyki.
- Jeden aktywny pomiar w całej aplikacji. Start, pauza, wznowienie, koniec i reset.
- Dwa niezależne wymagania strony przed/po sesji, domyślnie włączone. Strony **0 i 1 są poprawne**.
- Ostrzeżenie przy cofnięciu strony, późniejsza edycja stron i czasu sesji.
- Trwały stoper z zegarem monotonicznym, odtwarzanie po zamknięciu procesu i obsługa restartu telefonu.
- Osobny zwykły timer, domyślnie zablokowany, z własną historią.
- Pełne kopie ZIP, opcjonalnie AES-256, walidowane i przywracane transakcyjnie.
- Rzeczywista integracja Google Drive `appDataFolder`, **wymagająca konfiguracji OAuth właściciela**.
- Jasny/ciemny/systemowy motyw i opcjonalne niewygaszanie ekranu w aplikacji.

## Android Studio i uruchomienie

1. Sklonuj prywatne repo: `gh repo clone CrooLyyCheck/croolyytimer`.
2. Otwórz katalog projektu w Android Studio obsługującym AGP 9.2. Użyj JDK 21
   (wbudowany JDK Android Studio też może spełniać wymagania).
3. Zainstaluj Android SDK Platform 36 i zaakceptuj licencje SDK w SDK Managerze.
4. Pozwól Android Studio ustawić `local.properties` z lokalnym `sdk.dir`.
5. Poczekaj na Gradle Sync. Wybierz moduł `app` oraz telefon/emulator API 26+ i **Run**.

Projekt zawiera Gradle Wrapper 9.4.1. Pierwszy build potrzebuje internetu do pobrania bibliotek;
zainstalowana aplikacja działa bez internetu. Zależności są przypięte do kompatybilnego,
stabilnego zestawu, bez wersji alpha/beta i bez dynamicznych `+`.

### APK debug

```powershell
.\gradlew.bat assembleDebug
```

Linux/macOS: `./gradlew assembleDebug`. Wynik: `app/build/outputs/apk/debug/app-debug.apk`.
Instalacja: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
Artefakty z GitHub Actions są dostępne w zakładce Actions prywatnego repozytorium.

### Podpisany release

Utwórz własny klucz w **Android Studio → Build → Generate Signed App Bundle or APK**
albo `keytool -genkeypair` (RSA 3072+, SHA256withRSA). Zachowaj keystore i hasła poza repo,
w menedżerze haseł i bezpiecznej kopii. Ten sam klucz jest wymagany do kolejnych aktualizacji.

Konfiguracja Gradle odczytuje `CROOLYY_KEYSTORE`, `CROOLYY_STORE_PASSWORD`,
`CROOLYY_KEY_ALIAS` i `CROOLYY_KEY_PASSWORD` z lokalnego środowiska. Ustaw je bez
zapisywania sekretów w historii poleceń, np. przez menedżer sekretów albo skrypt z `Read-Host -AsSecureString`.
Następnie uruchom:

```powershell
.\gradlew.bat assembleRelease
```

Wynik: `app/build/outputs/apk/release/`. Bez zmiennych podpisu powstaje
`app-release-unsigned.apk`; nie jest to instalowalny podpisany release. Debug jest podpisany
automatycznie. CI sprawdza również kompilację release, ale nie ma dostępu do klucza właściciela.

## Odblokowanie zwykłego timera

Domyślnie pierwszy ekran jest zablokowany. Timery książek zawsze pozostają dostępne.
W Ustawieniach hasło **`takchce`** daje dokładnie **168 godzin od wpisania**. Ponowne wpisanie
ustawia nowe 168 godzin od tej chwili. Termin wygaśnięcia jest widoczny w interfejsie.

Stan jest zapisany w DataStore i przetrwa zamknięcie procesu oraz restart. Podczas jednego
uruchomienia telefonu czas dostępu uwzględnia monotoniczny zegar i trwały znacznik największego
zaobserwowanego czasu. Cofnięcie zegara nie wydłuża dostępu; znaczne cofnięcie przy restarcie
unieważnia tymczasowy dostęp. Przestawienie zegara w przyszłość może skrócić dostęp.
Po wygaśnięciu można zakończyć już aktywny zwykły pomiar, a wznowienie wymaga odblokowania.

### Hasło administratora i jego zmiana

W repo jest wyłącznie weryfikator **Argon2id v19, 64 MiB, 3 iteracje, 1 wątek, losowa sól 16 B**.
Bootstrap wygenerował losowe hasło i je odrzucił — nie istnieje wspólne hasło administratora,
które należałoby przekazywać w czacie. Przed korzystaniem z trwałego odblokowania ustaw własne:

```powershell
python -m pip install -r tools/requirements.txt
python tools/set_admin_password.py
```

Skrypt pyta bez echa o własne hasło (min. 20 znaków) i potwierdzenie. Zalecamy losowe hasło
z menedżera haseł. Opcja `python tools/set_admin_password.py --generate` generuje długie losowe
hasło i **kopiuje je do schowka**, po czym wymaga potwierdzenia zapisania go w menedżerze haseł
i czyści schowek. Ten tryb wymaga interaktywnego terminala i systemowego narzędzia schowka
(`clip.exe`, `pbcopy`, `wl-copy` lub `xclip`). Hasło nie jest wypisywane do terminala ani logów.
Skrypt nie przyjmuje hasła jako argumentu, nie zapisuje go do pliku i nie wypisuje w trybie własnego hasła.

Zmienia się tylko `app/src/main/assets/admin_verifier.json`. Przebuduj APK i wydaj aktualizację
podpisaną tym samym kluczem. Nowa instalacja zweryfikuje nowe hasło lokalnie. Dotychczasowe
trwałe odblokowanie pozostaje trwałe; rotacja hasła nie cofa już nadanych uprawnień.
Po 5 błędnych próbach działa trwały cooldown od 30 s do 1 godziny.

Blokada jest kontrolą funkcji. Całkowicie offline klient nie gwarantuje odporności na zmodyfikowane
APK, root, podmianę lokalnych danych czy reinstalację. Nie zabezpiecza danych wrażliwych.

## Timery i odzyskiwanie

Pomiar to suma zakończonych odcinków i różnicy `SystemClock.elapsedRealtime()` od ostatniego
wznowienia. Nie jest licznikiem dodającym sekundę w tle. Stan trafia do Room przy każdej zmianie,
co 10 s w widocznej aplikacji i przy przejściu w tło. Wygaszenie ekranu, blokada telefonu,
zmiana orientacji i zabicie procesu nie przerywają pomiaru w obrębie jednego startu urządzenia.

Po restarcie urządzenia sesja wraca **wstrzymana**, z czasem do ostatniego checkpointu.
Nie doliczamy arbitralnie czasu wyłączonego telefonu. Część od ostatniego zapisu do restartu
może wymagać ręcznej korekty w historii sesji; aplikacja mówi o tym wprost. Import aktywnej
sesji z ZIP-a również zawsze przywraca ją jako wstrzymaną. Nie tworzy drugiej sesji.

Nie ma usługi foreground ani stałego powiadomienia: do odtwarzania różnicy czasu nie jest
potrzebna praca procesora w tle. Aplikacja nie żąda uprawnień do kamery ani całej pamięci;
zdjęcie robi zewnętrzna aplikacja aparatu przez FileProvider, pliki wybiera systemowy selektor.
Jedynym deklarowanym uprawnieniem jest internet do opcjonalnego Drive.

## Lokalne kopie zapasowe

Ustawienia → **Utwórz kopię zapasową** otwiera systemowy wybór docelowego pliku.
Domyślnie kopia jest jawna. Każdy posiadacz pliku może ją przeczytać.
Włączenie szyfrowania wymaga dwukrotnego wpisania hasła (min. 8 znaków); Zip4j tworzy ZIP AES-256.
Hasło kopii nie jest zapisywane. Utraconego hasła nie można odzyskać.

```text
CrooLyyTimer-2026-11-06.zip
├── manifest.json    # format/version, długości i SHA-256 plików
├── data.json        # książki, timery, sesje, aktywny pomiar, ustawienia
└── covers/          # prywatne, zoptymalizowane okładki JPEG
```

Statystyki są wyliczane z sesji. Uprawnienia timera, verifier administratora i autoryzacja Google
nie są eksportowane ani odtwarzane z niezaufanej kopii. Informacja o ostatnim udanym eksporcie
jest lokalną metadaną urządzenia. Preferencje użytkownika są w transakcyjnej tabeli Room,
aby import ustawień i biblioteki był jedną operacją; DataStore przechowuje osobno dostęp i metadane.

Przywracanie wymaga potwierdzenia zastąpienia danych. Najpierw archiwum jest kopiowane do cache,
sprawdzane i strumieniowo rozpakowywane do osobnego katalogu. Walidowane są: wersja, nazwy,
rozmiary, sumy kontrolne, identyfikatory, relacje, wartości stron i obrazy. Okładki mają nazwy SHA-256
i są instalowane przed transakcją bazy bez nadpisywania istniejących plików. Uszkodzenie lub błąd
hasła nie zmienia obecnej biblioteki. Stare, nieużywane pliki okładek mogą zostać w prywatnej pamięci;
nie są dołączane do kolejnych kopii.

Limity ochronne: ZIP 256 MiB, po rozpakowaniu 512 MiB, dane JSON 32 MiB, pojedyncza okładka
10 MiB, do 10 002 wpisów; import zdjęcia źródłowego 30 MiB, optymalizacja do ok. 1400 px.
Nie wykonujemy `extractAll`; ścieżki są ściśle dopuszczone, sprawdzane kanonicznie, dane mają limity
również podczas czytania. Kopia wymaga wolnego miejsca na plik i tymczasowe rozpakowanie.

## Google Drive

Pełna instrukcja: [docs/GOOGLE_DRIVE.md](docs/GOOGLE_DRIVE.md).
Wymagane są projekt Google Cloud, Drive API, ekran zgody oraz klient Android dla pakietu
i SHA certyfikatu. Bez nich połączenie nie zadziała. Biblioteka i lokalne kopie pozostają niezależne.

## Testy i CI

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease
.\gradlew.bat connectedDebugAndroidTest
python -m unittest discover -s tools/tests -v
```

Testy jednostkowe i integracyjne obejmują zegary, pauzę, restart, pojedynczą aktywną sesję,
wszystkie kombinacje pytań o strony, rotację weryfikatora, limit 168 godzin, cooldown, migrację,
pełny roundtrip biblioteki z okładkami, AES-256, błędne hasło, uszkodzenie i Zip Slip.
Test Compose na emulatorze przechodzi od zablokowanego ekranu przez dodanie książki i timerów
do sesji od stron 0 i 1, odtworzenia Activity i niezależnych przełączników.

GitHub Actions buduje debug/release, uruchamia testy, lint i oddzielnie testy na emulatorze API 35.
APK oraz raporty są artefaktami workflow. Aktualny zakres wykonanej weryfikacji jest w
[docs/VALIDATION.md](docs/VALIDATION.md).

Na Windows uruchamianym w pakiecie MSIX Java może zgłosić `Unable to establish loopback connection`.
Pomaga utworzenie krótkiego katalogu tymczasowego i ustawienie lokalnie
`JAVA_TOOL_OPTIONS=-Djdk.net.unixdomain.tmpdir=C:\\krotka\\sciezka` przed Gradle.
Nie trzeba zmieniać projektu ani wyłączać testów.

## Struktura i rozwój

- `data/`: Room, modele UUID i pliki okładek.
- `domain/`: zegar monotoniczny i walidacja stron.
- `security/`: Argon2id, 168 godzin i ograniczanie prób.
- `backup/`: ZIP, walidacja, transakcyjny import, rzeczywisty klient Drive.
- `ui/`: Compose, ViewModel, nawigacja, motywy i dialogi.
- `tools/`: wymiana weryfikatora administratora oraz jej test.

Zależności wstrzykuje mały kontener `CrooLyyApp`; nie ma globalnego stanu biblioteki w pamięci.
Schematy Room są wersjonowane. Migracja 1→2 dodaje ukończenie książki bez usuwania historii;
brak destructive fallback. Nowe wersje bazy wymagają migracji i odpowiednich testów.

[Brand](docs/BRAND.md) · [Specyfikacja](docs/SPECIFICATION_PL.md) · [Prywatność](docs/PRIVACY.md)
· [Biblioteki i licencje](THIRD_PARTY_NOTICES.md). Repozytorium i kod są prywatne; prawa zastrzeżone.
