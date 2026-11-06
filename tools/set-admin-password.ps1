param([switch]$Generate)
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
    $bundledJava = Join-Path $env:USERPROFILE '.cache\croolyycar-toolchain\jdk-21\jdk-21.0.11+10'
    if (-not $env:JAVA_HOME -and (Test-Path -LiteralPath $bundledJava)) { $env:JAVA_HOME = $bundledJava }
    $temporaryDirectory = Join-Path $projectDirectory '.tmp'
    New-Item -ItemType Directory -Path $temporaryDirectory -Force | Out-Null
    $previousJavaOptions = $env:JAVA_TOOL_OPTIONS
    try {
        $env:JAVA_TOOL_OPTIONS = "$previousJavaOptions -Djdk.net.unixdomain.tmpdir=$temporaryDirectory"
        & .\gradlew.bat assembleDebug --console=plain
        if ($LASTEXITCODE -ne 0) { throw 'Hasło ustawiono, ale kompilacja nie powiodła się. Popraw błąd i uruchom .\gradlew.bat assembleDebug.' }
    } finally { $env:JAVA_TOOL_OPTIONS = $previousJavaOptions }
    Write-Host 'Gotowe: app\build\outputs\apk\debug\app-debug.apk'
    Write-Host 'Zainstaluj jako aktualizację, otwórz Ustawienia aplikacji i wpisz swoje hasło. Stoper odblokuje się na stałe.'
} finally { Pop-Location }
