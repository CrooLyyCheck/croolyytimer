# Weryfikacja CrooLyyTimer 1.2.1

Data: **9 listopada 2026**. Weryfikacja lokalna na Windows, JDK 21, Gradle Wrapper 9.4.1,
Android SDK 37.0 (compile), target 36, min 26. Zgodnie z decyzją właściciela repozytorium
**nie używa GitHub Actions**. Zakres wcześniejszego wydania uzupełniono o powiadomienie Androida 17.

## Automatyczne sprawdzenia

| Sprawdzenie | Wynik |
| --- | --- |
| Testy Kotlin / JVM / Robolectric | 56 testów, 0 błędów |
| Testy skryptów Python | 2 testy, 0 błędów |
| Android lint debug | 0 błędów, 26 ostrzeżeń o wersjach zależności i zaleceniach |
| APK debug | zbudowany; podpis debug |
| APK release | zbudowany z R8 i redukcją zasobów; bez klucza właściciela pozostaje unsigned |
| APK testów urządzeniowych | zbudowany |

Podpis debug został zweryfikowany narzędziem Android `apksigner verify --print-certs`.
Metadane APK i suma kontrolna 1.2.1 są publikowane w `updates.json` oraz w wydaniu GitHub.
APK: `CrooLyyTimer-1.2.1-debug.apk`, 24 497 663 B, `versionCode=4`.
SHA-256: `4fac51d24f56d1cb9e218044453e144075885f9b96ea26c81c9cdf441019d233`.
Sprawdzono zawartość APK: nie ma pliku weryfikatora ani wartości hasha administratora.
SHA-1 certyfikatu: `FD:21:F8:38:94:E1:08:8B:BC:22:18:86:FF:28:C1:B3:77:0A:F9:32`,
ten sam co 1.0.0. `aapt dump badging` potwierdza Android 8.0+ i target 36.

Polecenia:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug assembleDebug assembleRelease assembleDebugAndroidTest
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
- Statystyki: lokalne daty i zmiana czasu letniego, przekroczenie północy, ważone tempo,
  brakujące/cofnięte strony, serie dni, puste dane, miesiąc przestępny i szacunek czasu do końca.
- Interfejs wykresu 7/30 dni, wybór daty heatmapy, filtrowanie książek i otwieranie książki z sesji.
- Open Library: kodowanie polskich tytułów i parametrów, odrzucenie wyników bez okładki,
  usuwanie duplikatów, limit propozycji, buforowanie zapytań i wybranego obrazu.
- Interfejs wyszukania i wyboru dużej okładki z podstawionym transportem. Osobny test API 26
  potwierdza skopiowanie pobranego obrazu do prywatnego magazynu i przetrwanie usunięcia cache.
- Powiadomienia na API 35, 36 i 37: licznik w górę, nieruchomy czas na pauzie, prośba o promowanie,
  tożsamość działań dla konkretnej sesji i respektowanie usunięcia powiadomienia.
- Android 17: systemowy `MetricStyle` z czasem w treści i główną metryką dla pastylki,
  ukryty mały zegar nagłówka, zachowane tytuł i działania oraz spełnione
  `hasPromotableCharacteristics()`. Pauza i wznowienie nie doliczają przerwy; zmiana boot
  zamraża zapisany czas; sesja dłuższa niż uptime zachowuje cały pomiar.
- Render promowanego szablonu frameworka Android 17 w Robolectric z grafiką natywną:
  chronometr w treści pokazuje `01:10`, ma zmierzone wymiary i tekst co najmniej 24 sp.
  To test szablonu Androida, nie render interfejsu One UI.
- Działanie ze starego powiadomienia nie zmienia nowej sesji; zablokowanego zwykłego stopera
  nie można wznowić przyciskiem powiadomienia.
- Aktualizator: wybór kanału, wyższy numer wersji, zgodność z Androidem, ograniczenia rozmiaru
  i formatu katalogu, odrzucanie podmienionego pakietu, wersji, sumy pliku i certyfikatu.
- Interfejs pokazuje dostępną aktualizację i pozwala rozpocząć pobieranie; instalacja jest
  zablokowana podczas aktywnej sesji. Nowa karta ustawień została wyrenderowana i obejrzana.
- Zdalne hasło: format weryfikatora, brak liczenia awarii sieci jako błędnych haseł,
  pobranie nowego weryfikatora przy kolejnych próbach, zachowanie trwałego odblokowania
  oraz brak blokowania odświeżania zegara podczas oczekiwania na sieć.
- Skrypt przygotowania aktualizacji odczytuje i sprawdza faktyczne APK, a następnie generuje
  katalog z jego rozmiarem, SHA-256, wersją i certyfikatem. Nie publikuje nic automatycznie.
- Po publikacji 1.2.0 sprawdzono bez logowania rzeczywisty katalog z `main`, pobrano całe APK
  z GitHub Releases i potwierdzono jego rozmiar oraz SHA-256. Zdalny weryfikator administratora
  ma tę samą zawartość co opublikowany plik i poprawne parametry; hasło nie było wypisywane.
- 6 listopada 2026 wykonano rzeczywiste zapytanie Search API o „Mały Książę”: zwróciło kilka
  wydań z okładkami. Duży obraz z Covers API zwrócił HTTP 200 i 31 865 B. To próba HTTP na hoście,
  a nie test internetu i importu na fizycznym telefonie.

## Kontrola wizualna

Rendery natywnych komponentów Android wykonano przez Robolectric z grafiką natywną,
na wymiarach 393×850 dp. To rzeczywiste ekrany Compose z testową biblioteką, a nie makiety HTML.
Sprawdzono jasny ekran blokady, bibliotekę, szczegóły książki, aktywny stoper i ciemną bibliotekę,
a także komponenty kalendarza, wybranego dnia, statystyk i pełnej szerokości karty ustawień.
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
7. Użytkownik potwierdził działanie pastylki na S24 Ultra / One UI 9 / Androidzie 17 po włączeniu
   systemowej opcji dla wszystkich aplikacji, opisanej w [SYSTEM_TIMER.md](SYSTEM_TIMER.md).
   Nowy układ dużego czasu w 1.2.1 i działania z zablokowanego ekranu wymagają końcowej próby
   na tym telefonie. Testy JVM i render szablonu Androida nie zastępują kontroli interfejsu One UI.
8. Dekoder ImageDecoder z API 28+ używa natywnego kodu Androida, którego odczyt pliku nie działał
   w tym teście Windows. Test UI okładek potwierdza wybór i pobranie pliku, a osobny test API 26
   sprawdza import. Pełny import okładki na nowszym Androidzie pozostaje do sprawdzenia na urządzeniu.
9. Skrypt ma zweryfikowaną składnię i poprawne odczytanie polskich znaków w Windows PowerShell 5.1
   oraz PowerShell 7.6. Używa UTF-8 z BOM, co utrwala także `.editorconfig`.
   Rotacja hasła w Pythonie ma test automatyczny.
   Prywatne hasło właściciela nie zostało ustawione ani wygenerowane w logach tej sesji.
10. Pełny przebieg systemowej instalacji APK, powrót z ekranu zezwolenia i odblokowanie własnym
    hasłem na rzeczywistym telefonie nie zostały wykonane w tym środowisku. Testy potwierdzają
    reguły walidacji i interfejs; instalator jest systemową częścią Androida.

## Świadome ograniczenia

- Po restarcie telefonu zachowujemy czas do ostatniego checkpointu i wstrzymujemy sesję.
  Brakującą część można uzupełnić po zapisie w historii. Nie doliczamy czasu wyłączonego telefonu.
- Lokalna blokada funkcji nie jest zabezpieczeniem wrażliwych danych przed rootem lub zmienionym APK.
- Dawne APK 1.0/1.1 wymagają jednorazowej aktualizacji, aby korzystać ze zmiany hasła w repo.
- Od APK 1.2 źródłem weryfikatora administratora jest repo. Zmiana nie wymaga przebudowania APK,
  ale przyszłe trwałe odblokowanie wymaga internetu. Rotacja nie cofa już przyznanych uprawnień.
- Przerwany transfer Drive jest ponawiany jako nowy transfer, bez wznawiania po śmierci procesu.
- Nie ma automatycznych kopii ani usługi foreground. Chronometr powiadomienia obsługuje system;
  pomiar nie wymaga pracy procesora co sekundę w tle.
- Sesje w kalendarzu są przypisane do daty rozpoczęcia, bez dzielenia przez północ.
- Limity wielkości kopii, zasady prywatności i zachowanie nieużywanych okładek są opisane w README.
