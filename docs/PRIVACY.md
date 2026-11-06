# Prywatność CrooLyyTimer

CrooLyyTimer nie zawiera reklam, analityki ani obowiązkowego konta. Książki, obrazy,
historia i ustawienia są w prywatnej przestrzeni Android. Aplikacja nie wysyła ich automatycznie.

Od wersji 1.2 aplikacja może sprawdzać nowe wersje na GitHubie przy uruchomieniu, najwyżej raz
na 6 godzin. Można to wyłączyć w ustawieniach. Katalog wersji i APK są publiczne; konto GitHub
nie jest potrzebne. Pobierany jest także publiczny weryfikator przy próbie trwałego odblokowania
stopera. Wpisane hasło i jego wyliczony hash nie są wysyłane do dostawcy. GitHub widzi zwykłe
dane połączenia, np. IP i identyfikator aplikacji, bez biblioteki i historii czytania.
Weryfikator nie jest przechowywany w APK ani używany offline. Przyznane trwałe odblokowanie
pozostaje w telefonie; zmiana hasła w repo go nie cofa. Pliki aktualizatora i jego preferencje
są lokalne dla urządzenia i nie są eksportowane do kopii biblioteki ZIP.

Wyszukiwanie okładek jest dobrowolne: po naciśnięciu przycisku wpisany tytuł trafia przez HTTPS
do Open Library. Dostawca widzi także zwykłe dane połączenia (np. adres IP i identyfikator aplikacji).
Nie przesyłamy historii sesji, całej biblioteki ani danych konta Google. Wybrana okładka zostaje
skopiowana lokalnie; podglądy mogą być buforowane w cache Androida. Obejmuje je polityka
dostawcy [Internet Archive / Open Library](https://archive.org/about/terms.php).

Za zgodą użytkownika aktywna sesja ma powiadomienie systemowe z czasem i sterowaniem.
Na ekranie blokady stosujemy prywatną widoczność treści; końcowy wygląd zależy od ustawień telefonu.
Usunięcie powiadomienia nie zatrzymuje pomiaru. Aplikacja nie publikuje go ponownie dla tej samej sesji.

Eksport lokalny zapisuje wybrany przez użytkownika plik ZIP. Bez hasła jest czytelny
dla każdego posiadacza. Opcjonalne AES-256 szyfruje zawartość, ale nazwy wpisów ZIP
mogą pozostać widoczne. Nigdy nie zapisujemy hasła kopii.

Google Drive jest dobrowolny, wymaga osobnej zgody i używa tylko `drive.appdata`.
Przesyła się wyłącznie kopię wybraną przez użytkownika. Token jest w pamięci procesu,
nie jest częścią lokalnej bazy, ZIP-a ani logów. Rozłączenie cofa zgodę przez Google.
Usuwanie kopii wymaga świadomego wyboru; nie ma automatycznej retencji.

Usunięcie książki usuwa jej timery i historię; nieużywane pliki okładek mogą zostać
w prywatnej pamięci do usunięcia danych aplikacji. Usunięcie aplikacji usuwa jej lokalną
bibliotekę. Zewnętrzne ZIP-y i kopie Drive użytkownik usuwa osobno. Android Auto Backup
jest wyłączony, aby nie odtwarzać lokalnych uprawnień dostępu poza kontrolą użytkownika.
