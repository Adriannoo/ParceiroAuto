param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$backend = Join-Path $root 'backend/parceiro_auto_back'
if (!$JavaHome -or !(Test-Path "$JavaHome/bin/java.exe")) { throw 'Informe -JavaHome com o caminho do JDK.' }
if (!(Test-Path "$backend/target/site/jacoco/jacoco.xml")) { throw 'Execute mvnw.cmd clean verify antes da analise.' }
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaHome/bin;$env:PATH"
$secret = Import-Clixml "$root/.quality-local/sonar-token.xml"
$previousToken = $env:SONAR_TOKEN
try {
    $env:SONAR_TOKEN = [pscredential]::new('token', $secret).GetNetworkCredential().Password
    Push-Location $backend
    try {
        & ./mvnw.cmd -B sonar:sonar '-Dsonar.host.url=http://127.0.0.1:9000' '-Dsonar.qualitygate.wait=true' '-Dsonar.scanner.skipJreProvisioning=true'
        if ($LASTEXITCODE -ne 0) { throw 'Analise falhou ou Quality Gate reprovou. Consulte a saida e o painel.' }
    } finally { Pop-Location }
} finally { $env:SONAR_TOKEN = $previousToken }
