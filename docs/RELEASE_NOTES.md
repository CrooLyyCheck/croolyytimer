# CrooLyyTimer 1.1.0

- Okładki z internetu bez własnego serwera i konta: wpisz tytuł, wyszukaj, wybierz wydanie z Open Library i zapisz książkę. Wybrana okładka zostaje w telefonie i kopii ZIP.
- Szczegóły książki: klikalny wykres 7/30 dni, tempo, serie dni, najdłuższa sesja i szacunek czasu do końca.
- Nowa zakładka Kalendarz: miesięczna heatmapa, filtr książki i lista sesji z wybranego dnia.
- Powiadomienie aktywnego stopera z licznikiem, pauzą i wznowieniem. Obsługa prośby o Live Updates na Androidzie 16+; wygląd pastylki / Now Bar zależy od telefonu i ustawień systemu.
- Krótsze komunikaty dla użytkownika, bez instrukcji OAuth i tekstu o stronach 0/1. Zera nadal można używać jako numeru strony.
- Karta informacji w ustawieniach ma pełną szerokość. Licencja i biblioteki otwierają konkretne pliki repozytorium.
- W interfejsie funkcja nazywa się Stoper. Marka CrooLyyTimer pozostaje.
- Prosta konfiguracja własnego hasła administratora na Windows: `.\tools\set-admin-password.ps1` pyta o hasło i buduje osobiste APK. Po instalacji wpisz to hasło w ustawieniach, aby odblokować stoper na stałe.

APK: **CrooLyyTimer-1.1.0-debug.apk**, podpis zgodny z wydaniem 1.0.0, aktualizacja bez usuwania danych.
To instalowalne wydanie testowe; keystore właściciela do docelowej dystrybucji nie został dostarczony.
Repozytorium i pliki APK są publicznie dostępne, bez logowania do GitHuba.
Oficjalne APK są udostępnione do osobistego, niekomercyjnego użytku. Ponowne wykorzystanie kodu,
modyfikowanie i dalsze rozpowszechnianie wymagają pisemnej zgody właściciela, z wyjątkami określonymi
w [licencji](https://github.com/CrooLyyCheck/croolyytimer/blob/main/LICENSE).

Weryfikacja: 41 testów Kotlin/JVM/Robolectric i 1 test Python, lint bez błędów,
kompilacja debug, release z R8 oraz APK testów urządzeniowych. Wszystko lokalnie, bez GitHub Actions.
Zakres i ograniczenia: [docs/VALIDATION.md](https://github.com/CrooLyyCheck/croolyytimer/blob/main/docs/VALIDATION.md).
Google Drive nadal wymaga konfiguracji OAuth właściciela. Widoczność Now Bar i pełny przebieg
z aparatem, selektorami oraz dekoderem obrazów wymagają końcowej próby na rzeczywistym telefonie.
