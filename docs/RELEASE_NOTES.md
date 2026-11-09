# CrooLyyTimer 1.2.1 — czytelny stoper w powiadomieniu

- Android 17+: duży, samoczynnie odświeżany czas w treści karty dzięki systemowemu `MetricStyle`.
- Ukryty mały licznik w nagłówku. Tytuł książki i nazwa stopera pozostają widoczne w nagłówku.
- Pastylka systemowa korzysta z tego samego pomiaru. Przyciski Pauza/Wznów i Otwórz pozostają dostępne.
- Pauza pokazuje nieruchomy czas; wznowienie zachowuje wcześniejszy wynik i nie dolicza przerwy.
- Na Androidzie 8–16 pozostaje dotychczasowy systemowy chronometr.

Aktualizacja jest dostępna w **Ustawienia → Aktualizacje** (kanał wydań testowych) lub przez pobranie
`CrooLyyTimer-1.2.1-debug.apk` z tego wydania. Zainstaluj jako aktualizację, bez odinstalowywania aplikacji.
APK ma ten sam certyfikat co wcześniejsze wydania; biblioteka i odblokowania pozostają zapisane.

Na Galaxy S24 Ultra / One UI 9 użytkownik potwierdził działanie pastylki po włączeniu
**Opcje programisty → Więcej ustawień → Aplikacje → Powiadomienia na żywo dla wszystkich aplikacji**.
Nowy układ dużego czasu wymaga jeszcze końcowego sprawdzenia na tym telefonie; wyglądem zarządza One UI.
Szczegóły: [stoper systemowy](https://github.com/CrooLyyCheck/croolyytimer/blob/main/docs/SYSTEM_TIMER.md).

Wydanie testowe podpisane kluczem debug. Testy i kompilacja lokalne, bez GitHub Actions.
Zakres weryfikacji: [docs/VALIDATION.md](https://github.com/CrooLyyCheck/croolyytimer/blob/main/docs/VALIDATION.md).
Warunki używania i ponownego wykorzystania kodu: [licencja](https://github.com/CrooLyyCheck/croolyytimer/blob/main/LICENSE).
