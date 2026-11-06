# Weryfikacja CrooLyyTimer 1.0.0

Data: **6 listopada 2026**. Weryfikacja lokalna na Windows, JDK 21, Gradle Wrapper 9.4.1,
Android SDK 36. Zgodnie z decyzją właściciela repozytorium **nie używa GitHub Actions**.

## Automatyczne sprawdzenia

| Sprawdzenie | Wynik |
| --- | --- |
| Testy Kotlin / JVM / Robolectric | 24 testy, 0 błędów |
| Test skryptu zmiany hasła | 1 test, 0 błędów |
| Android lint debug | 0 błędów; pozostają ostrzeżenia o nowszych wersjach zależności i drobne zalecenia |
| APK debug | zbudowany; podpis debug |
| APK release | zbudowany z R8 i redukcją zasobów; bez klucza właściciela pozostaje unsigned |
| APK testów urządzeniowych | zbudowany |

Podpis debug został zweryfikowany narzędziem Android `apksigner verify --print-certs`.
APK: `CrooLyyTimer-1.0.0-debug.apk` (24 946 607 B).
SHA-256: `adc8ec2756b1adf2ad39928d9a882e8f571046ca9b68e6e0a8142d4152fab68c`.

Polecenia:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest
python -m unittest discover -s tools/tests -v
```

## Co potwierdzają testy

- Start od strony 0 i 1; brak akceptacji ujemnych stron.
- Rzeczywiste dialogi Compose akceptują 0 i 1, a cofnięcie strony wymaga osobnego potwierdzenia.
- Wszystkie cztery kombinacje niezależnych wymagań strony przed/po sesji.
- Jedna aktywna sesja; brak doliczania pauzy; czas monotoniczny mimo zmian zegara ściennego.
- Odtworzenie aktywnej sesji po ponownym otwarciu plikowej bazy i bezpieczna pauza po restarcie.
- Domyślna blokada, dokładny limit 168 godzin, wygaśnięcie, cofnięcie zegara, trwałe odblokowanie i cooldown.
- Zgodność weryfikatora Argon2id generowanego Pythonem z implementacją Android/JVM.
- Rotacja hasła skryptem zmienia sól i hash; poprzednie hasło przestaje pasować; hasło nie trafia do pliku ani logu.
- Migracja bazy 1→2 zachowuje książki.
- Kopiowanie zdjęcia do prywatnego magazynu i zachowanie go po usunięciu pliku źródłowego.
- Pełny roundtrip ZIP: dwie książki, trzy timery, różne sesje, historia zwykłego timera, ustawienia i okładki.
- Po eksporcie test usuwa bibliotekę i okładkę, następnie import odtwarza wszystkie dane i bajty obrazu.
- ZIP AES-256 odmawia odczytu bez poprawnego hasła.
- Uszkodzone archiwum, zmienione dane niezgodne z sumą kontrolną, Zip Slip i błędne relacje są odrzucane.
- Błąd importu nie zmienia istniejącej biblioteki.
- Natywny interfejs w Robolectric: dodanie książki, timery bez pytań o strony, pauza/wznowienie,
  dwie zapisane sesje, niezależne przełączniki i zmiana na ciemny motyw.

## Kontrola wizualna

Rendery natywnych komponentów Android wykonano przez Robolectric z grafiką natywną,
na wymiarach 393×850 dp. To rzeczywiste ekrany Compose z testową biblioteką, a nie makiety HTML.
Sprawdzono jasny ekran blokady, bibliotekę, szczegóły książki, aktywny timer i ciemną bibliotekę.
Obrazy są w [screenshots/](screenshots/). Dane testowe nie są dołączone do instalowanej aplikacji.

## Co wymaga telefonu lub konfiguracji zewnętrznej

1. **Pełny test urządzeniowy nie został zaliczony w tym środowisku.** Emulator uruchomiony
   wewnątrz maszyny wirtualnej Windows nie ukończył startu. Testy urządzeniowe są przygotowane
   w `app/src/androidTest` i skompilowane. Uruchom je na działającym emulatorze lub telefonie:
   `./gradlew connectedDebugAndroidTest`.
2. W złożonym scenariuszu nawigacji Robolectric pozostawiał nowe okno dialogu bez przyłączenia
   i pomiaru, co kończyło się brakiem bezczynności renderera. Dlatego test powłoki aplikacji
   obejmuje wariant z wyłączonymi pytaniami; dialogi stron mają osobne przechodzące testy.
   Nie przedstawiamy pełnego przebiegu na urządzeniu z domyślnymi pytaniami jako potwierdzonego.
3. Fizyczny aparat, systemowy wybór zdjęcia i wybór docelowego ZIP-a wymagają ręcznego sprawdzenia
   na urządzeniu. Sam magazyn okładek i format kopii mają testy integracyjne.
4. Wygaszenie ekranu i rzeczywiste zabicie procesu przez Android wymagają końcowej próby na telefonie.
   Testy potwierdzają trwałe dane i algorytm odtwarzania; nie zastępują testów konkretnego producenta.
5. Google Drive ma rzeczywistą implementację API, ale **nie został autoryzowany ani przetestowany
   na koncie właściciela**. Wymaga konfiguracji opisanej w [GOOGLE_DRIVE.md](GOOGLE_DRIVE.md).
6. Podpisany kluczem właściciela release wymaga jego keystore. Dostarczony debug APK jest instalowalny.

## Świadome ograniczenia

- Po restarcie telefonu zachowujemy czas do ostatniego checkpointu i wstrzymujemy sesję.
  Brakującą część można uzupełnić po zapisie w historii. Nie doliczamy czasu wyłączonego telefonu.
- Lokalna blokada funkcji nie jest zabezpieczeniem wrażliwych danych przed rootem lub zmienionym APK.
- Pierwszy losowy sekret administratora został odrzucony; właściciel ustawia własny skryptem i przebudowuje APK.
- Przerwany transfer Drive jest ponawiany jako nowy transfer, bez wznawiania po śmierci procesu.
- Nie ma automatycznych kopii ani powiadomienia z usługą foreground. Stan timera nie wymaga pracy w tle.
- Limity wielkości kopii, zasady prywatności i zachowanie nieużywanych okładek są opisane w README.
