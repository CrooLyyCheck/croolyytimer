# Hasło administratora zarządzane w repo

Od APK 1.2 weryfikator nie jest wbudowany w aplikację. Jedynym aktywnym źródłem jest
[`config/admin-verifier.json`](../config/admin-verifier.json) na gałęzi `main` tego repozytorium.
Plik zawiera sól, hash i parametry Argon2id, bez hasła. Nie wpisuj tam hasła wprost.

Zmiana z Windows PowerShell 5.1 lub PowerShell 7:

```powershell
.\tools\set-admin-password.ps1
```

Skrypt prosi dwukrotnie o hasło (minimum 20 znaków), tworzy nową sól i hash, zatwierdza tylko
plik weryfikatora, a następnie wykonuje `git push origin main`. Nie buduje aplikacji.
Wymaga Pythona i uprawnień Git do repo. Inne pliki przygotowane w indeksie nie są dołączane
do commita weryfikatora. `-Generate` losuje hasło i przekazuje je do schowka, a `-NoPush`
pozostawia zmianę lokalnie. W przypadku błędu push plik i commit pozostają lokalnie do ponowienia publikacji.

Aplikacja przy próbie trwałego odblokowania pobiera aktualny plik przez HTTPS, z pominięciem
lokalnego cache. Sprawdza format i stałe, ograniczone koszty Argon2id, po czym porównuje hash
w telefonie. Ani wpisane hasło, ani wyliczony z niego hash nie są wysyłane do GitHuba.
Bez internetu trwałe odblokowanie nie jest przyznawane; nie używa się starego hasła offline.
Propagacja nowego pliku przez GitHuba może zająć krótką chwilę.

Przyznane wcześniej **trwałe odblokowanie pozostaje trwałe**, również po zmianie hasła.
Rotacja hasła zmienia przyszłe weryfikacje, a nie usuwa uprawnień już zapisanych w telefonach.
Kod `takchce` nadal daje 168 godzin lokalnego dostępu bez internetu.
Błąd sieci nie zwiększa licznika błędnych haseł; po 5 błędnych hasłach obowiązuje cooldown.

APK 1.0/1.1 mają dawny weryfikator wbudowany podczas kompilacji. Trzeba je raz zaktualizować
do 1.2+, żeby korzystały ze zmian hasła w repo. Lokalna blokada funkcji nadal może zostać
ominięta w zmodyfikowanej aplikacji; nie jest serwerowym systemem kont.
