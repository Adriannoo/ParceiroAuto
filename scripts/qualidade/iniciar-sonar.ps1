param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$sonar = Join-Path $root '.tools/sonarqube-26.10.0.133078'
if (!$JavaHome -or !(Test-Path "$JavaHome/bin/java.exe")) { throw 'Informe -JavaHome com o caminho do JDK 21 ou 25.' }
if (!(Test-Path "$sonar/bin/windows-x86-64/StartSonar.bat")) { throw 'Instale o SonarQube em .tools conforme docs/testes-qualidade.md.' }
$env:SONAR_JAVA_PATH = "$JavaHome/bin/java.exe"
# A janela atual mostra os logs; Ctrl+C encerra o servidor.
& "$sonar/bin/windows-x86-64/StartSonar.bat"
