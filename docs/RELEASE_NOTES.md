# CrooLyyTimer 1.2.0

- Aktualizator w Ustawieniach: automatyczne sprawdzanie przy uruchomieniu (najwyżej raz na 6 godzin), sprawdzanie ręczne, pobieranie z postępem i instalacja przez Androida.
- Kanał stabilny i opcjonalne wydania testowe, bez konta, sklepu, serwera właściciela i GitHub Actions.
- Sprawdzanie SHA-256, rozmiaru, pakietu, numeru wersji i certyfikatu podpisu przed instalacją. Instalacja nie jest uruchamiana podczas aktywnej sesji.
- Hasło administratora zarządzane w `config/admin-verifier.json` w repo. Aplikacja pobiera aktualny weryfikator przy odblokowaniu; hasło pozostaje w telefonie. Zmiana hasła nie wymaga nowego APK.
- Skrypt `.\tools\set-admin-password.ps1` publikuje nowy hash, bez kompilacji aplikacji. Przyznane wcześniej trwałe odblokowania pozostają aktywne.

**Zainstaluj 1.2.0 ręcznie jako aktualizację poprzedniego APK.** Starsze wersje nie mają aktualizatora
ani pobierania hasła z repo. Kolejne wydania będą wykrywane w aplikacji.

Trwałe odblokowanie nowym hasłem wymaga internetu. Biblioteka, stoper, publiczny kod `takchce`
i już przyznane trwałe odblokowanie nadal działają offline.

Poprzednie funkcje pozostają dostępne:

- Okładki z internetu bez własnego serwera i konta: wpisz tytuł, wyszukaj, wybierz wydanie z Open Library i zapisz książkę. Wybrana okładka zostaje w telefonie i kopii ZIP.
- Szczegóły książki: klikalny wykres 7/30 dni, tempo, serie dni, najdłuższa sesja i szacunek czasu do końca.
- Nowa zakładka Kalendarz: miesięczna heatmapa, filtr książki i lista sesji z wybranego dnia.
- Powiadomienie aktywnego stopera z licznikiem, pauzą i wznowieniem. Obsługa prośby o Live Updates na Androidzie 16+; wygląd pastylki / Now Bar zależy od telefonu i ustawień systemu.
- Krótsze komunikaty dla użytkownika, bez instrukcji OAuth i tekstu o stronach 0/1. Zera nadal można używać jako numeru strony.
- Karta informacji w ustawieniach ma pełną szerokość. Licencja i biblioteki otwierają konkretne pliki repozytorium.
- W interfejsie funkcja nazywa się Stoper. Marka CrooLyyTimer pozostaje.

APK: **CrooLyyTimer-1.2.0-debug.apk**, podpis zgodny z wydaniami 1.0.0 i 1.1.0, aktualizacja bez usuwania danych.
To instalowalne wydanie testowe; keystore właściciela do docelowej dystrybucji nie został dostarczony.
Repozytorium i pliki APK są publicznie dostępne, bez logowania do GitHuba.
Oficjalne APK są udostępnione do osobistego, niekomercyjnego użytku. Ponowne wykorzystanie kodu,
modyfikowanie i dalsze rozpowszechnianie wymagają pisemnej zgody właściciela, z wyjątkami określonymi
w [licencji](https://github.com/CrooLyyCheck/croolyytimer/blob/main/LICENSE).

Weryfikacja: 49 testów Kotlin/JVM/Robolectric i 2 testy skryptów Python, lint bez błędów,
kompilacja debug, release z R8 oraz APK testów urządzeniowych. Wszystko lokalnie, bez GitHub Actions.
Zakres i ograniczenia: [docs/VALIDATION.md](https://github.com/CrooLyyCheck/croolyytimer/blob/main/docs/VALIDATION.md).
Google Drive nadal wymaga konfiguracji OAuth właściciela. Instalator aktualizacji, widoczność Now Bar
i pełny przebieg z aparatem, selektorami oraz dekoderem obrazów wymagają końcowej próby na telefonie.
