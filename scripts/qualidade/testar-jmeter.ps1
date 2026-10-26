param([string]$JavaHome = $env:JAVA_HOME, [int]$Port = 8080, [ValidateRange(1,10)][int]$Users = 2, [ValidateRange(1,20)][int]$Loops = 2)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (!$JavaHome -or !(Test-Path "$JavaHome/bin/java.exe")) { throw 'Informe -JavaHome com o caminho do JDK.' }
$jar = "$root/.tools/apache-jmeter-5.6.3/bin/ApacheJMeter.jar"
if (!(Test-Path $jar)) { throw 'Instale o JMeter em .tools conforme docs/testes-qualidade.md.' }
$output = "$root/backend/parceiro_auto_back/target/jmeter/$(Get-Date -Format yyyyMMdd-HHmmss-fff)"
New-Item $output -ItemType Directory -Force | Out-Null
& "$JavaHome/bin/java.exe" -Xmx512m -jar $jar -n -t "$root/backend/parceiro_auto_back/src/test/jmeter/smoke-api.jmx" "-Jport=$Port" "-Jusers=$Users" "-Jloops=$Loops" -l "$output/results.csv" -j "$output/jmeter.log" -e -o "$output/html"
if ($LASTEXITCODE -ne 0) { throw 'JMeter nao conseguiu executar o plano.' }
$results = @(Import-Csv "$output/results.csv")
if (!$results.Count -or ($results | Where-Object success -ne 'true')) { throw "Ha requisicoes com falha. Consulte $output/results.csv" }
Write-Host "$($results.Count) requisicoes aprovadas. Relatorio: $output/html/index.html"
