# ZADANIE: Stwórz kompletną aplikację Android do mierzenia czasu czytania książek

Jesteś doświadczonym Android Developerem, UI/UX Designerem i architektem oprogramowania.

Twoim zadaniem jest **utworzenie od podstaw kompletnego, działającego projektu natywnej aplikacji Android**, gotowego do rozwijania w repozytorium GitHub, otwierania w Android Studio i instalowania na telefonie.

Nie ograniczaj się do napisania dokumentacji, planu, prototypu ani makiety. **Zaimplementuj rzeczywiście działającą aplikację.**

## 1. Inspiracja i założenia

Aplikacja referencyjna:

https://play.google.com/store/apps/details?id=com.hydroponicglass.reading_timer

Stwórz autorską aplikację o podobnej filozofii: wygodne mierzenie czasu czytania, katalog książek, historia sesji i monitorowanie postępów.

Nie kopiuj identycznie interfejsu, grafik ani kodu aplikacji referencyjnej.

Projekt powinien być:

- Nowoczesny, minimalistyczny i estetyczny.
- Przyjemny podczas codziennego korzystania.
- W całości dostępny po polsku.
- Dostosowany do telefonów Android.
- Funkcjonalny offline.
- Pozbawiony reklam i obowiązkowego konta.
- Wyposażony w jasny i ciemny motyw.
- Zoptymalizowany pod kątem płynności oraz zużycia baterii.

### Preferowany stos technologiczny

- Kotlin.
- Jetpack Compose.
- Material Design 3 / Material You.
- Room Database.
- DataStore Preferences.
- MVVM.
- Kotlin Coroutines i Flow.
- Navigation Compose.
- Dependency Injection, np. Hilt.
- Aktualne stabilne wersje bibliotek kompatybilne z projektem.
- Gradle Kotlin DSL.

Architektura powinna umożliwiać łatwe rozwijanie aplikacji.

## 2. Wygląd i interfejs

Zaprojektuj dopracowany interfejs przypominający nowoczesne aplikacje lifestyle/productivity.

Preferowany styl:

- Zaokrąglone karty i przyciski.
- Delikatne animacje.
- Czytelna typografia.
- Duże cyfry odmierzające czas.
- Estetyczne ikony.
- Przyjazne komunikaty.
- Eleganckie prezentowanie okładek książek.
- Subtelne efekty wizualne podczas uruchamiania i zatrzymywania timera.
- Przyjemne animowane paski postępu.
- Ekrany pustych stanów zamiast pustych list.

Możesz zastosować paletę ciepłych kremowych kolorów z akcentami zieleni, a w ciemnym motywie grafit i stonowane kolory akcentujące.

Interfejs powinien wyglądać jak gotowy produkt, a nie domyślna demonstracja komponentów Android.

### Nawigacja główna

Utwórz dolny pasek nawigacji z trzema głównymi zakładkami:

1. Timer
2. Książki
3. Ustawienia

Pierwsza zakładka musi być zakładką zwykłego timera.

---

## 3. Zakładka TIMER — domyślnie zablokowana

Pierwsza zakładka zawiera klasyczny stoper mierzący czas.

Jednak **po pierwszej instalacji zwykły timer musi być zablokowany**.

Użytkownik powinien widzieć atrakcyjny ekran blokady z ikoną kłódki i komunikatem:

**„Lepiej używaj timera z zakładki Książki! 📚

Jeśli jednak, szczurku 🐀, naprawdę chcesz korzystać ze zwykłego timera, przejdź do Ustawień i wpisz hasło odblokowujące.”**

Dodaj przycisk:

„Przejdź do ustawień”

### Odblokowanie na 7 dni

W ustawieniach umieść sekcję:

**„Odblokowanie zwykłego timera”**

Z polem do wpisania hasła.

Hasło użytkownika:

`takchce`

Po prawidłowym wpisaniu:

- Zwykły timer zostaje odblokowany na dokładnie 7 dni (168 godzin).
- Dostęp przetrwa zamknięcie aplikacji i ponowne uruchomienie telefonu.
- Użytkownik zobaczy datę i godzinę wygaśnięcia dostępu.
- Po upływie 7 dni blokada automatycznie wróci.
- Ponowne wpisanie hasła może przedłużyć dostęp o kolejne 7 dni od chwili wpisania.
- Czas dostępu nie może być liczony wyłącznie przez działający proces aplikacji.
- Zadbaj o możliwie rozsądną odporność na cofanie zegara systemowego.

### Hasło administratora — trwałe odblokowanie

Dodaj drugie, niezależne hasło administratora.

Wpisanie poprawnego hasła administratora ma:

- Trwale odblokować zwykły timer.
- Usunąć limit 7 dni.
- Zachować odblokowanie po ponownym uruchomieniu aplikacji i telefonu.
- Pokazać informację „Timer odblokowany na stałe”.

#### Bardzo ważne wymagania bezpieczeństwa

Nie umieszczaj hasła administratora jawnym tekstem w repozytorium ani w kodzie aplikacji.

Zamiast odwracalnego szyfrowania wykorzystaj bezpieczny weryfikator hasła oparty na silnej funkcji haszującej z losową solą, np. Argon2id.

Hasło administratora powinno być losowe, długie i trudne do odgadnięcia.

W repozytorium przechowuj wyłącznie wynik haszowania oraz potrzebne parametry weryfikacji.

Weryfikacja powinna odbywać się lokalnie, bez internetu.

Dodaj zabezpieczenie przed wielokrotnym automatycznym zgadywaniem haseł.

**Wymagam prostego mechanizmu zmiany hasła administratora przez właściciela repozytorium.**

W tym celu:

1. Utwórz skrypt `tools/set_admin_password.py` lub równoważny.
2. Skrypt powinien umożliwiać wygenerowanie nowego losowego hasła lub bezpieczne wprowadzenie własnego.
3. Skrypt ma wygenerować nowy weryfikator hasła.
4. Weryfikator ma zostać zapisany w odpowiednim pliku projektu.
5. W README podaj dokładną instrukcję uruchomienia skryptu.
6. Zmiana hasła musi wymagać jedynie uruchomienia skryptu, przebudowania aplikacji i wydania aktualizacji.
7. Nigdy nie zapisuj prawdziwego hasła w repozytorium ani logach.
8. Prawidłowe hasło właściciel powinien zachować we własnym menedżerze haseł.

W dokumentacji uczciwie wyjaśnij, że zabezpieczenie działające całkowicie offline po stronie klienta nie może gwarantować pełnej odporności na modyfikację APK lub manipulację danymi aplikacji.

Traktuj tę blokadę jako mechanizm kontroli funkcji, a nie zabezpieczenie dostępu do wrażliwych danych.

### Zwykły timer po odblokowaniu

Powinien udostępniać:

- Start.
- Pauzę.
- Wznowienie.
- Zakończenie.
- Reset.
- Wyświetlanie godzin, minut i sekund.
- Zapisywanie historii pomiarów.
- Opcję usuwania zapisanych pomiarów.

Zwykły timer jest niezależny od książek i nie zwiększa czasu ich czytania.

---

## 4. Zakładka KSIĄŻKI — główna funkcjonalność

Jest to najważniejsza część aplikacji.

Użytkownik powinien mieć możliwość dodawania dowolnej liczby książek.

### Dodawanie książki

Formularz powinien umożliwiać:

- Wpisanie tytułu książki — wymagane.
- Wpisanie autora — opcjonalne.
- Wpisanie całkowitej liczby stron — opcjonalne.
- Wybranie okładki z plików telefonu lub galerii.
- Zrobienie zdjęcia aparatem i wykorzystanie go jako okładki.
- Zapisanie książki.

Jeśli użytkownik nie wybierze okładki, aplikacja ma wygenerować estetyczną okładkę zastępczą.

Wybraną okładkę skopiuj do prywatnej przestrzeni aplikacji.

Nie polegaj wyłącznie na zewnętrznym URI zdjęcia, ponieważ mogłoby przestać działać po przywróceniu backupu.

### Biblioteka książek

Książki prezentuj na eleganckiej liście lub siatce.

Każda pozycja powinna prezentować:

- Okładkę.
- Tytuł.
- Autora.
- Aktualnie przeczytaną stronę.
- Łączny czas czytania.
- Procent postępu, jeżeli znana jest liczba stron.

Umożliwiaj wyszukiwanie książek, edycję oraz oznaczanie ich jako ukończonych.

### Szczegóły książki

Po kliknięciu książki otwórz dedykowany ekran zawierający:

- Dużą okładkę.
- Tytuł i autora.
- Aktualną stronę.
- Postęp czytania.
- Sumaryczny czas czytania.
- Listę timerów przypisanych do tej książki.
- Historię sesji czytania.
- Przycisk dodania kolejnego timera.

### Wiele timerów na książkę

**Jedna książka może posiadać dowolną liczbę niezależnych timerów.**

Każdy timer musi:

- Należeć do konkretnej książki.
- Mieć własny identyfikator.
- Opcjonalnie posiadać własną nazwę, np. „Wieczorne czytanie”.
- Zapamiętywać wszystkie powiązane sesje.
- Wyświetlać tytuł i okładkę przypisanej książki.
- Umożliwiać rozpoczęcie nowych sesji.
- Przechowywać łączny czas oraz historię sesji.

Wszystkie sesje z różnych timerów tej samej książki mają sumować się do jej całkowitego czasu czytania.

W jednej chwili aktywna może być maksymalnie jedna sesja odmierzająca czas, aby uniknąć przypadkowego podwójnego naliczania.

---

## 5. Obowiązkowe wpisywanie strony przed uruchomieniem timera

To jedna z kluczowych funkcji aplikacji.

### Przed rozpoczęciem czytania

Domyślnie, po kliknięciu „Rozpocznij czytanie”, wyświetl okno z pytaniem:

**„Na której stronie aktualnie jesteś?”**

Użytkownik wpisuje numer strony, od której rozpoczyna czytanie.

Ważne:

- Wartość 0 jest prawidłowa.
- Wartość 1 jest prawidłowa.
- Nie można odrzucać wartości 0 i 1.
- Nie dopuszczaj liczb ujemnych.
- Po zatwierdzeniu uruchom timer.
- Zapisz numer początkowej strony w sesji.

### Po zakończeniu czytania

Domyślnie, po kliknięciu „Zakończ”, wyświetl pytanie:

**„Na której stronie skończyłeś czytać?”**

Zapisz końcowy numer strony i powiąż go z sesją.

Na podstawie tych danych obliczaj:

- Liczbę przeczytanych stron w sesji.
- Czas czytania.
- Średnie tempo czytania, jeżeli dane są wystarczające.
- Aktualny postęp książki.

Jeżeli końcowa strona jest mniejsza niż początkowa, pokaż ostrzeżenie i umożliw świadome zatwierdzenie, np. przy ponownym czytaniu wcześniejszego fragmentu.

Nie generuj fałszywych statystyk, jeśli wymaganych danych brakuje.

### Dwa niezależne przełączniki w ustawieniach

Dodaj ustawienia:

**[WŁ.] Wymagaj numeru strony przed rozpoczęciem timera**

**[WŁ.] Wymagaj numeru strony po zakończeniu timera**

Oba ustawienia mają być domyślnie włączone.

Możliwe kombinacje:

- Oba włączone.
- Oba wyłączone.
- Tylko pytanie przed startem.
- Tylko pytanie po zakończeniu.

Jeżeli dany wymóg jest wyłączony, timer działa bez obowiązkowego pytania.

Użytkownik powinien móc później ręcznie uzupełnić lub poprawić numery stron zapisanej sesji.

---

## 6. Niezawodność timerów

Timer musi działać prawidłowo podczas:

- Wygaszenia ekranu.
- Przełączenia na inną aplikację.
- Zablokowania telefonu.
- Zmiany orientacji ekranu.
- Tymczasowego usunięcia procesu aplikacji przez system.
- Ponownego uruchomienia aplikacji.

Nie implementuj odmierzania czasu wyłącznie przez zwiększanie licznika co sekundę.

Stosuj mechanizm oparty na rzeczywistym czasie rozpoczęcia, czasie monotonicznym, zapisanym czasie pauz oraz trwałym stanie sesji.

Zaimplementuj poprawne odtwarzanie stanu po zamknięciu aplikacji.

Rozważ powiadomienie z informacją o aktywnej sesji i przyciskami pauzy/wznowienia, zgodnie z aktualnymi ograniczeniami usług działających w tle na Androidzie.

Nie nadużywaj foreground service. Zastosuj go tylko wtedy, gdy uzasadnia to rzeczywiście działająca funkcjonalność.

Po ponownym uruchomieniu urządzenia aplikacja powinna bezpiecznie obsłużyć niedokończoną sesję i umożliwić jej odzyskanie lub zakończenie bez podwójnego naliczania.

---

## 7. Historia i statystyki

Dodaj prosty, estetyczny system statystyk.

Na poziomie książki pokaż:

- Całkowity czas czytania.
- Liczbę sesji.
- Łączną liczbę przeczytanych stron, jeżeli możliwa do wyliczenia.
- Aktualny postęp procentowy.
- Średni czas sesji.
- Historię czytania z datami.

Na poziomie całej aplikacji możesz dodać:

- Łączny czas czytania.
- Czas czytania dzisiaj.
- Statystyki tygodniowe.
- Liczbę ukończonych książek.
- Serię kolejnych dni z czytaniem.

Niech statystyki mają przyjemną wizualną prezentację.

Funkcje te nie mogą opóźniać realizacji głównych wymagań: timerów, książek i backupów.

---

## 8. Kompletny lokalny backup ZIP

**Wymagam pełnego systemu tworzenia i przywracania kopii zapasowych.**

Użytkownik powinien móc wyeksportować wszystkie swoje dane do jednego pliku ZIP.

W ustawieniach utwórz sekcję „Kopie zapasowe”.

Dodaj przyciski:

- „Utwórz kopię zapasową”
- „Przywróć kopię zapasową”

### Zakres kopii

ZIP musi zawierać wszystkie dane użytkownika potrzebne do całkowitego odtworzenia aplikacji, w szczególności:

- Wszystkie książki.
- Ich tytuły i autorów.
- Okładki w oryginalnej lub zoptymalizowanej jakości.
- Numery stron.
- Wszystkie timery.
- Przypisania timerów do książek.
- Wszystkie sesje.
- Historię zwykłego timera.
- Statystyki lub dane umożliwiające ich ponowne obliczenie.
- Ustawienia użytkownika.
- Preferencje interfejsu.
- Pozostałe dane aplikacji.

Jedynymi wyjątkami mogą być dane uwierzytelniające, tokeny OAuth, hasło administratora i lokalne uprawnienia dostępu, których przywracanie z niezaufanego ZIP-a stwarzałoby ryzyko obejścia zabezpieczeń.

### Przykładowa struktura ZIP

```text
reading_backup.zip
├── manifest.json
├── books.json
├── timers.json
├── sessions.json
├── settings.json
└── covers/
    ├── cover_001.jpg
    ├── cover_002.png
    └── ...
```

To tylko propozycja. Możesz zastosować lepszą strukturę, jeśli będzie bezpieczna i wygodna do migracji.

Dodaj wersjonowanie formatu kopii.

### Opcjonalne szyfrowanie backupu

Podczas tworzenia backupu użytkownik może włączyć opcję:

**[WYŁ.] Zaszyfruj kopię zapasową hasłem**

Domyślnie szyfrowanie musi być wyłączone.

Jeżeli użytkownik włączy szyfrowanie:

1. Aplikacja poprosi o hasło.
2. Poprosi o ponowne wpisanie hasła.
3. Utworzy zabezpieczony plik ZIP z szyfrowaniem AES-256, wykorzystując sprawdzoną bibliotekę.
4. Przy przywracaniu poprosi o właściwe hasło.
5. Nie będzie przechowywać hasła backupu w kodzie ani w samym archiwum.

W przypadku niezaszyfrowanej kopii pokaż jasny komunikat, że każda osoba mająca plik ZIP może odczytać zawarte w nim dane.

### Przywracanie

Przywracanie powinno:

- Pozwalać wybrać plik z pamięci telefonu przez systemowy selektor.
- Rozpoznawać zwykłe i szyfrowane ZIP-y.
- Sprawdzać poprawność archiwum.
- Weryfikować wersję backupu.
- Odtwarzać książki, zdjęcia, timery i ich relacje.
- Pokazywać postęp operacji.
- Informować o sukcesie lub błędzie.

Przed przywróceniem danych pokaż pytanie:

**„Czy chcesz zastąpić obecne dane zawartością kopii zapasowej?”**

Nie usuwaj aktualnych danych bez świadomego potwierdzenia.

Zastosuj bezpieczne przywracanie przez dane tymczasowe, walidację i transakcję, żeby uszkodzony backup nie zniszczył istniejącej bazy.

Zabezpiecz rozpakowywanie przed Zip Slip, nadmiernymi rozmiarami i uszkodzonymi plikami.

### Wymagany test

Utwórz test, który:

1. Dodaje kilka książek.
2. Przypisuje do nich różne timery.
3. Dodaje sesje i okładki.
4. Tworzy backup.
5. Usuwa dane testowe.
6. Przywraca backup.
7. Sprawdza kompletność książek, timerów, sesji, ustawień i okładek.

---

## 9. Backup na koncie Google — preferowany Google Drive App Data

Chciałbym również możliwość tworzenia i przywracania kopii na koncie Google użytkownika.

Preferowane rozwiązanie:

**Google Drive API — `appDataFolder`**

Jest to ukryta przestrzeń danych aplikacji na Dysku Google.

### Wymagania

Dodaj do sekcji backupu opcjonalne funkcje:

- „Połącz konto Google”
- „Utwórz kopię w Google Drive”
- „Przywróć kopię z Google Drive”
- „Wyświetl dostępne kopie”
- „Usuń wybraną kopię”

Dane powinny być przechowywane w przestrzeni `appDataFolder`, jeżeli aktualne API i konfiguracja OAuth to umożliwiają.

Stosuj minimalny zakres uprawnień:

`https://www.googleapis.com/auth/drive.appdata`

Nie wymagaj dostępu do całej zawartości Dysku Google.

Użytkownik musi świadomie autoryzować połączenie.

Backup w Google Drive powinien wykorzystywać ten sam sprawdzony format danych co backup lokalny.

Jeżeli użytkownik włączy szyfrowanie, backup w chmurze również powinien zachować szyfrowanie.

Nie traktuj ukrytego folderu Google Drive jako zamiennika szyfrowania.

### Dodatkowe wymagania

- Obsługa braku internetu.
- Obsługa wygaśnięcia autoryzacji.
- Informacja o ostatniej wykonanej kopii.
- Czytelna lista kopii z datami.
- Możliwość rozłączenia konta.
- Brak automatycznego kasowania starszych backupów bez odpowiedniej polityki.

Możesz dodatkowo skonfigurować natywny Android Auto Backup, ale **nie może on zastąpić ręcznego eksportu ZIP ani świadomego przywracania backupu**.

### Jeżeli Google Drive wymaga konfiguracji zewnętrznej

Przygotuj implementację oraz dokumentację krok po kroku:

1. Utworzenie projektu Google Cloud.
2. Włączenie Google Drive API.
3. Konfiguracja OAuth.
4. Konfiguracja identyfikatora pakietu i SHA certyfikatu.
5. Uruchomienie i przetestowanie połączenia.

Nie dodawaj prywatnych kluczy ani tokenów do GitHuba.

Jeżeli bez zewnętrznej konfiguracji funkcja nie może działać, wyraźnie to zaznacz. Nie przedstawiaj makiety logowania ani fikcyjnego uploadu jako ukończonego backupu.

Priorytetem pozostaje w pełni działający backup lokalny.

---

## 10. Ustawienia aplikacji

Przygotuj elegancki ekran ustawień podzielony na sekcje.

### Timery

- Odblokowanie zwykłego timera.
- Pole do wpisania hasła.
- Informacja o terminie wygaśnięcia dostępu.
- Informacja o stałym odblokowaniu administratora.

### Czytanie

- Wymagaj strony przed rozpoczęciem.
- Wymagaj strony po zakończeniu.
- Opcjonalne ustawienia prezentacji aktywnego timera.

### Wygląd

- Motyw jasny.
- Motyw ciemny.
- Motyw zgodny z systemem.
- Opcjonalne pozostawianie ekranu włączonego podczas czytania.

### Kopie zapasowe

- Eksport ZIP.
- Import ZIP.
- Opcjonalne szyfrowanie.
- Kopie w Google Drive.
- Zarządzanie połączeniem konta.

### Informacje

- Wersja aplikacji.
- Informacja o licencji.
- Informacje o prywatności danych.

Wszystkie ustawienia muszą być zapisywane trwale.

---

## 11. Model danych i bezpieczeństwo

Zaprojektuj model danych pozwalający na łatwe dodawanie funkcjonalności.

Sugerowane encje:

- Book
- BookTimer
- ReadingSession
- GeneralTimerSession
- AppSettings

Każda książka i każdy timer powinny mieć stabilne UUID.

Koniecznie zachowaj relacje:

Book → wiele BookTimer → wiele ReadingSession.

Zaimplementuj migracje bazy danych, aby aktualizacja aplikacji nie powodowała utraty historii.

Nie trzymaj całej biblioteki wyłącznie w pamięci aplikacji.

Okładki i dane użytkownika przechowuj w prywatnej przestrzeni aplikacji.

Zadbaj o:

- Bezpieczne operacje na plikach.
- Walidację wejścia.
- Poprawne zarządzanie zasobami.
- Brak sekretów w repozytorium.
- Minimalne uprawnienia Android.
- Poprawną obsługę wyjątków.
- Stabilne działanie po aktualizacjach.
- Rozsądne ograniczenie rozmiaru zdjęć okładek.

---

## 12. Repozytorium GitHub i jakość projektu

Zorganizuj projekt w przejrzystej strukturze katalogów.

Wymagam:

- Kompletnego projektu Android Studio.
- Konfiguracji Gradle.
- Gradle Wrapper.
- Czytelnego README.md.
- Pliku `.gitignore`.
- Testów jednostkowych.
- Testów integracyjnych dla istotnych operacji.
- GitHub Actions do automatycznego budowania i testowania aplikacji.
- Instrukcji tworzenia APK.
- Instrukcji zmiany hasła administratora.
- Instrukcji konfiguracji Google Drive.
- Listy wykorzystanych bibliotek i ich licencji.

Preferuj architekturę prostą i łatwą do utrzymania. Nie komplikuj projektu bez rzeczywistej potrzeby.

### README musi wyjaśniać

1. Jak otworzyć projekt w Android Studio.
2. Jak uruchomić aplikację.
3. Jak wygenerować APK debug.
4. Jak przygotować podpisany APK release.
5. Jak zmienić hasło administratora.
6. Jak działa odblokowanie na 7 dni.
7. Jak działają lokalne backupy.
8. Jak skonfigurować Google Drive.
9. Jak uruchomić testy.

---

## 13. Testy akceptacyjne

Przed zakończeniem prac sprawdź następujące przypadki:

- Nowy użytkownik widzi zablokowany zwykły timer.
- Hasło `takchce` odblokowuje go na 7 dni.
- Po wygaśnięciu dostępu timer ponownie jest zablokowany.
- Hasło administratora odblokowuje go na stałe.
- Zmiana hasła administratora skryptem działa po przebudowaniu aplikacji.
- Można utworzyć wiele książek.
- Można przypisać wiele timerów do jednej książki.
- Timer zapisuje sesję w odpowiedniej książce.
- Użytkownik może rozpocząć czytanie od strony 0.
- Użytkownik może rozpocząć czytanie od strony 1.
- Wymaganie strony przed startem można wyłączyć niezależnie od wymagania strony na końcu.
- Timer działa po wygaszeniu ekranu.
- Pauza nie nalicza dodatkowego czasu.
- Zamknięcie i ponowne uruchomienie aplikacji nie gubi sesji.
- Można wybrać zdjęcie okładki z plików.
- Można zrobić zdjęcie okładki aparatem.
- Utworzony ZIP zawiera wszystkie dane użytkownika i okładki.
- Backup zaszyfrowany wymaga hasła do przywrócenia.
- Przywrócenie ZIP odtwarza poprawne powiązania.
- Błędne lub uszkodzone archiwum nie niszczy istniejących danych.
- Ustawienia są zapamiętywane.
- Jasny i ciemny motyw działają.
- Aplikacja uruchamia się bez połączenia z internetem.
- Główne funkcje działają bez konta Google.

## 14. Kolejność realizacji

Realizuj projekt etapami, ale nie kończ pracy po pierwszym etapie.

### Etap 1 — Fundament

- Projekt Android.
- Architektura.
- Model danych.
- Motywy.
- Nawigacja.
- Podstawowy interfejs.

### Etap 2 — Książki i timery

- Dodawanie książek.
- Okładki.
- Wiele timerów.
- Sesje czytania.
- Numery stron.
- Historia i postępy.
- Niezawodne działanie timerów.

### Etap 3 — Blokada zwykłego timera

- Ekran blokady.
- Hasło `takchce`.
- Odblokowanie na 7 dni.
- Trwałe odblokowanie administratora.
- Skrypt zmiany hasła.

### Etap 4 — Backup

- Kompletny ZIP.
- Opcjonalne szyfrowanie.
- Przywracanie.
- Walidacja.
- Testy poprawności.

### Etap 5 — Google Drive

- Integracja z Google.
- Ukryty folder danych aplikacji.
- Tworzenie i przywracanie backupów.
- Dokumentacja wymaganej konfiguracji.

### Etap 6 — Dopracowanie

- Animacje.
- Statystyki.
- Obsługa błędów.
- Testy.
- Dokumentacja.
- GitHub Actions.
- Finalna kompilacja.

## 15. Oczekiwany rezultat

Na końcu oczekuję:

1. Kompletnego kodu źródłowego w repozytorium.
2. Działającego projektu Android Studio.
3. Poprawnego przejścia kompilacji.
4. Możliwości wygenerowania instalowalnego APK.
5. Zaimplementowanych funkcji opisanych powyżej.
6. Przetestowanego mechanizmu backupu i przywracania danych.
7. Gotowej instrukcji zmiany hasła administratora.
8. Dokumentacji integracji Google Drive.
9. Podsumowania wykonanych prac i wyników testów.
10. Uczciwej listy ewentualnych ograniczeń.

Nie pozostawiaj pustych przycisków, fikcyjnych funkcji ani atrap działających mechanizmów.

Jeżeli napotkasz problem techniczny, szukaj rozwiązania zgodnego z aktualnymi wytycznymi Androida.

Przyjmuj rozsądne decyzje projektowe bez zadawania zbędnych pytań. Pytaj wyłącznie o rzeczy rzeczywiście wymagające mojej decyzji, dostępu lub konfiguracji zewnętrznej.

**Najważniejszy priorytet: aplikacja ma być naprawdę użyteczna, niezawodna, estetyczna i zachowywać wszystkie dane użytkownika. Zacznij tworzenie projektu.**