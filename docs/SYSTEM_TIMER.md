# Stoper w powiadomieniu i pastylce systemowej

Aktywna sesja tworzy ciche powiadomienie z systemowym chronometrem liczącym w górę,
przyciskami Pauza/Wznów i Otwórz. Pauza pokazuje nieruchomy czas, koniec usuwa powiadomienie.
Na Androidzie 13+ zgoda pojawia się po uruchomieniu pierwszej sesji. Można ją zmienić w aplikacji:
**Ustawienia → Stoper poza aplikacją → Ustawienia powiadomień**.

Na Androidzie 16+ aplikacja prosi o promowanie trwającego pomiaru przez Live Updates,
deklaruje `POST_PROMOTED_NOTIFICATIONS` i używa standardowego stylu z chronometrem.
Warstwa AndroidX Core 1.17 zapewnia zgodność między API 36 i 36.1.
W ustawieniach aplikacji jest również przejście do systemowej widoczności na pasku.

## Samsung One UI 9: pastylka aplikacji zewnętrznej

**Potwierdzone przez użytkownika 9 listopada 2026:** na Galaxy S24 Ultra z One UI 9 / Androidem 17
CrooLyyTimer wyświetlał zwykłe powiadomienie, a w ustawieniach aplikacji nie było przełącznika
powiadomień na żywo. Pastylka zaczęła działać po włączeniu systemowej opcji:

**Opcje programisty → Więcej ustawień → Aplikacje → Powiadomienia na żywo dla wszystkich aplikacji**
(`Live notifications for all apps`).

Jeżeli Opcje programisty nie są widoczne, włącz je siedmioma stuknięciami w **Ustawienia →
Informacje o telefonie → Informacje o oprogramowaniu → Numer wersji**. Nazwy i położenie opcji
mogą zależeć od wydania One UI. To globalne ustawienie systemu; aplikacja nie może go samodzielnie
włączyć. Na sprawdzonym telefonie nie była potrzebna zmiana APK ani ponowna instalacja.

To potwierdzenie dotyczy wskazanego urządzenia i konfiguracji, a nie wszystkich wersji One UI.

Na zgodnym systemie może pojawić się pastylka statusu, a na Samsungu odpowiednia powierzchnia
Live Notifications / Now Bar. Aplikacja nie może wymusić dokładnie takiego wyglądu jak Zegar Samsunga.
Zależy to od modelu, wersji One UI/Androida, zgód, ustawień blokady i dodatkowych zasad producenta.
Sama nazwa One UI 8/9 nie wystarcza do zagwarantowania konkretnego położenia pastylki.
Obsługa jest zaimplementowana; rzeczywiste wyświetlanie na telefonie Samsung wymaga sprawdzenia.

Powiadomienie nie wymaga usługi foreground, pracy w pętli ani pobudek co sekundę.
Systemowy chronometr odświeża się sam; trwały pomiar nadal bazuje na zegarze monotonicznym
i Room. Przy usunięciu powiadomienia respektujemy wybór użytkownika i nie publikujemy go
ponownie dla tej samej sesji. Nie wykonujemy automatycznego startu po restarcie telefonu.
Odrzucenie zgody, systemowe ukrycie lub usunięcie powiadomienia nie przerywa zapisanego pomiaru.

Dokumentacja źródłowa:

- [Android Live Updates i wymagania powiadomienia](https://developer.android.com/develop/ui/views/notifications/live-update)
- [AndroidX NotificationCompat](https://developer.android.com/reference/androidx/core/app/NotificationCompat.Builder#setRequestPromotedOngoing(boolean))
- [Samsung: Now bar i Live Notifications](https://www.samsung.com/us/support/answer/ANS10004605/)
