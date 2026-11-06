# Prywatność CrooLyyTimer

CrooLyyTimer nie zawiera reklam, analityki ani obowiązkowego konta. Książki, obrazy,
historia i ustawienia są w prywatnej przestrzeni Android. Aplikacja nie wysyła ich automatycznie.

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
