# Konfiguracja Google Drive

Lokalne książki, pomiary i ZIP-y działają od razu, bez konta. Backup Drive korzysta
z prawdziwego Google Identity Services `AuthorizationClient` i REST API Drive v3.
Nie ma fikcyjnego logowania ani udawanego przesyłania.

## Konfiguracja właściciela

1. Otwórz [Google Cloud Console](https://console.cloud.google.com/), utwórz projekt **CrooLyyTimer**.
2. W **APIs & Services → Library** włącz **Google Drive API**.
3. W **Google Auth Platform** uzupełnij branding, kontakt i odbiorców. W trybie testowym
   dodaj adresy kont testowych. Dla kont konsumenckich wybierz External.
4. W Data Access dodaj tylko `https://www.googleapis.com/auth/drive.appdata`.
5. W Clients utwórz **OAuth client ID → Android**. Nazwa pakietu: `pl.croolyy.timer`.
6. Podaj SHA-1 certyfikatu, którym podpisano instalowany APK:
   `./gradlew signingReport` dla debug albo `keytool -list -v -keystore /prywatna/sciezka/owner.jks`
   dla release. Dla Google Play użyj SHA certyfikatu App Signing z Play Console.
   Każdy inny podpis wymaga osobnego klienta Android. APK z CI ma inny debug key niż lokalny.
7. Zapisz konfigurację i poczekaj na jej propagację. Klient Android jest identyfikowany
   przez **pakiet + SHA**, dlatego do projektu nie wkleja się client secret, klucza serwisowego
   ani tokenu. Nie jest potrzebny Firebase ani `google-services.json`.
8. Zainstaluj pasujący APK na telefonie z Google Play Services i internetem.
9. Ustawienia → Google Drive → **Połącz konto Google**. Wybierz konto i świadomie zaakceptuj zgodę.
10. Utwórz kopię, wyświetl listę, pobierz ją i potwierdź przywracanie. Sprawdź też wariant AES-256.
    Usuń testową kopię i rozłącz konto.

Bez tej zewnętrznej konfiguracji Google może zwrócić błąd konfiguracji/developer error.
Nie da się potwierdzić działania OAuth na koncie właściciela bez utworzenia klienta w jego projekcie.
Ta część wymaga sprawdzenia na skonfigurowanym koncie i nie jest przedstawiana jako zweryfikowana.

Certyfikat debug dostarczonego APK 1.0.0 ma SHA-1:
`FD:21:F8:38:94:E1:08:8B:BC:22:18:86:FF:28:C1:B3:77:0A:F9:32`.
Użyj go razem z pakietem `pl.croolyy.timer`, jeśli konfigurujesz dokładnie ten APK.
Po zmianie certyfikatu dodaj odpowiedni nowy klient Android.

## Zachowanie aplikacji

- Jedyny zakres to `drive.appdata`. Brak uprawnień do całego Dysku.
- Pliki mają rodzica `appDataFolder`; lista jest pobierana ze `spaces=appDataFolder`, ze stronicowaniem.
- Ukryte kopie nie są widoczne w zwykłym interfejsie Dysku.
- Upload wykorzystuje sesję resumable i przesyłanie strumieniowe. Przerwany upload można ponowić
  jako nową kopię; aplikacja nie wznawia transferu po śmierci procesu.
- Zapisuje się ten sam, zwalidowany format ZIP co lokalnie, z tym samym ustawieniem szyfrowania.
- Token dostępu pozostaje wyłącznie w pamięci procesu i nigdy nie wchodzi do ZIP-a.
  Po zamknięciu procesu lub wygaśnięciu autoryzacji użyj **Połącz konto Google** ponownie;
  wcześniejsza zgoda może pozwolić odnowić dostęp bez ponownego pytania.
- Brak internetu, błąd konfiguracji, odmowa zgody i wygasły token są komunikowane użytkownikowi.
- Rozłączenie wywołuje `revokeAccess`. Nie usuwa lokalnej biblioteki ani kopii.
- Usunięcie konkretnej kopii wymaga potwierdzenia. Nie ma automatycznego kasowania starszych kopii.
- Ukryty folder nie zapewnia poufności pliku ZIP; włącz AES-256, jeśli chcesz chronić zawartość.

## Dokumentacja źródłowa

- [Autoryzacja Android](https://developer.android.com/identity/authorization)
- [Drive appDataFolder](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Przesyłanie plików](https://developers.google.com/workspace/drive/api/guides/manage-uploads)
