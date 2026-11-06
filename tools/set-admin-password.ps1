param([switch]$Generate, [switch]$NoPush)
$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$pythonCommand = Get-Command python -ErrorAction SilentlyContinue
$bundledPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
$pythonExecutable = if (Test-Path -LiteralPath $bundledPython) { $bundledPython } elseif ($pythonCommand) { $pythonCommand.Source } else { throw 'Zainstaluj Python 3, a następnie uruchom skrypt ponownie.' }
Push-Location $projectDirectory
try {
    & $pythonExecutable -m pip install -r tools/requirements.txt
    if ($LASTEXITCODE -ne 0) { throw 'Nie udało się przygotować biblioteki Argon2.' }
    if ($Generate) { & $pythonExecutable tools/set_admin_password.py --generate }
    else { & $pythonExecutable tools/set_admin_password.py }
    if ($LASTEXITCODE -ne 0) { throw 'Zmiana hasła została przerwana.' }
    if ($NoPush) { Write-Host 'Zapisano lokalnie. Opublikuj config/admin-verifier.json, aby udostępnić nowe hasło aplikacji.'; return }
    $branch = & git branch --show-current
    if ($LASTEXITCODE -ne 0 -or $branch -ne 'main') { throw 'Hash zapisano lokalnie. Publikacja wymaga gałęzi main.' }
    & git add -- config/admin-verifier.json
    if ($LASTEXITCODE -ne 0) { throw 'Nie udało się przygotować weryfikatora do publikacji.' }
    & git commit --only -m 'Rotate administrator password verifier' -- config/admin-verifier.json
    if ($LASTEXITCODE -ne 0) { throw 'Nie udało się zapisać zmiany w Git.' }
    & git push origin main
    if ($LASTEXITCODE -ne 0) { throw 'Weryfikator zapisano, ale publikacja nie powiodła się. Sprawdź dostęp do GitHuba i uruchom git push origin main.' }
    Write-Host 'Gotowe. Nowe hasło jest opublikowane w repo jako hash. APK 1.2+ pobierze go przy odblokowaniu.'
    Write-Host 'Nie trzeba budować ani instalować kolejnej wersji aplikacji.'
} finally { Pop-Location }
