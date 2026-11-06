# Aktualizacje bez sklepu

Od wersji 1.2 aplikacja sprawdza `updates.json` w tym samym publicznym repozytorium.
Przy uruchomieniu lub powrocie do aplikacji robi to najwyżej raz na 6 godzin.
W **Ustawienia → Aktualizacje** można sprawdzić dostępność ręcznie, wyłączyć sprawdzanie
automatyczne i wybrać uwzględnianie wydań testowych. Debug domyślnie uwzględnia wersje testowe.
Sprawdzanie przy uruchomieniu nie jest zadaniem okresowym działającym przy zamkniętej aplikacji.

Po wykryciu wersji pojawia się informacja z przejściem do ustawień. Wybierz **Pobierz aktualizację**,
a po pobraniu **Zainstaluj aktualizację**. Przy pierwszym użyciu Android poprosi o zezwolenie na
instalowanie z CrooLyyTimer. Następnie potwierdź aktualizację w instalatorze systemowym.
Pobieranie można anulować. Instalacja jest dostępna dopiero po zakończeniu aktywnej sesji,
również sesji wstrzymanej. Dane biblioteki i przyznane trwałe odblokowanie pozostają przy aktualizacji.

**Przejście z 1.1:** tę aktualizację trzeba zainstalować ręcznie z GitHub Releases. Dopiero 1.2+
zawiera aktualizator. Nie trzeba odinstalowywać aplikacji.

## Wydanie kolejnej wersji przez właściciela

1. Zwiększ `versionCode` i zmień `versionName` w `app/build.gradle.kts`.
2. Zbuduj APK tym samym kluczem co poprzednie wydanie. Dla obecnego kanału debug:
   `./gradlew assembleDebug` (`.\gradlew.bat assembleDebug` na Windows).
3. Z JDK 21 ustawionym w `JAVA_HOME` przygotuj pliki:

   ```powershell
   python tools/prepare_update.py --apk app/build/outputs/apk/debug/app-debug.apk --channel preview --notes "Opis zmian dla użytkownika."
   ```

   Skrypt sprawdza podpis APK, odczytuje jego wersję i minimum Androida, tworzy plik APK
   oraz `.apk.sha256` w `artifacts/`, a także aktualizuje lokalny `updates.json`.
4. Wyślij APK do GitHub Releases przez `gh release create` lub `gh release upload`.
   Tag musi być `v<versionName>`, a nazwa pliku zgodna z wygenerowaną. Kanał `preview`
   publikuj jako prerelease; kanał `stable` jako zwykłe wydanie.
5. Dopiero gdy APK jest dostępne do pobrania, zatwierdź i wyślij `updates.json` na `main`.
   To moment udostępnienia aktualizacji aplikacjom. Nie są używane GitHub Actions.

Właściciel przechowuje klucz podpisujący poza repozytorium. `prepare_update.py` nie podpisuje
APK i nie wysyła go automatycznie. Zmiana klucza wymaga osobnego planu migracji;
aktualizator odrzuca pliki podpisane innym certyfikatem.

## Kontrola i ograniczenia

Katalog ma limit 64 KiB, APK 128 MiB. Transfery korzystają wyłącznie z HTTPS i znanych hostów
GitHuba. Sprawdzane są: rozmiar, SHA-256, pakiet, wyższy numer wersji, minimum Androida
oraz certyfikat zgodny z instalacją i katalogiem. Kontrola jest powtarzana przed instalacją;
Android dodatkowo weryfikuje podpis przy instalowaniu. Nie ma tokenu GitHuba w aplikacji.

Pobrane APK jest w prywatnym cache. Po restarcie procesu może być użyte ponownie po kontroli.
Jeżeli Android usunie cache albo przerwie proces podczas transferu, pobieranie trzeba ponowić.
Sam instalator, zgody producenta i pełny przebieg aktualizacji wymagają próby na telefonie.
Ustawienia aktualizatora i jego cache są lokalne dla urządzenia i nie trafiają do kopii biblioteki ZIP.
