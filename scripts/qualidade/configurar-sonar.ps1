$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$local = Join-Path $root '.quality-local'
New-Item $local -ItemType Directory -Force | Out-Null
$url = 'http://127.0.0.1:9000'
$credentialPath = Join-Path $local 'sonar-admin.xml'
if (Test-Path $credentialPath) {
    $credential = Import-Clixml $credentialPath
} else {
    $credential = [pscredential]::new('admin', (ConvertTo-SecureString 'admin' -AsPlainText -Force))
}
function Invoke-Sonar($Path, $Body) {
    $pair = 'admin:' + $credential.GetNetworkCredential().Password
    $headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair)) }
    if ($null -eq $Body) { return Invoke-RestMethod "$url/api/$Path" -Headers $headers }
    Invoke-RestMethod "$url/api/$Path" -Method Post -Headers $headers -Body $Body
}
if (!(Test-Path $credentialPath)) {
    $password = [guid]::NewGuid().ToString('N') + '!aA1'
    Invoke-Sonar 'users/change_password' @{login='admin'; previousPassword='admin'; password=$password} | Out-Null
    $credential = [pscredential]::new('admin', (ConvertTo-SecureString $password -AsPlainText -Force))
    # O Windows protege a senha para este usuario e esta maquina.
    $credential | Export-Clixml $credentialPath
}
$project = 'parceiroauto-backend'
$projects = Invoke-Sonar "projects/search?projects=$project" $null
if ($projects.paging.total -eq 0) {
    Invoke-Sonar 'projects/create' @{project=$project; name='ParceiroAuto Backend'; visibility='private'} | Out-Null
}
$gate = 'ParceiroAuto - Entrega'
$gates = Invoke-Sonar 'qualitygates/list' $null
if (!($gates.qualitygates | Where-Object name -eq $gate)) {
    Invoke-Sonar 'qualitygates/create' @{name=$gate} | Out-Null
}
$existing = Invoke-Sonar ('qualitygates/show?name=' + [uri]::EscapeDataString($gate)) $null
# Todas as condicoes avaliam o codigo completo, nao apenas codigo novo.
$conditions = @(
    @{metric='coverage'; op='LT'; error='90'},
    @{metric='duplicated_lines_density'; op='GT'; error='2'},
    @{metric='bugs'; op='GT'; error='0'},
    @{metric='vulnerabilities'; op='GT'; error='0'},
    @{metric='code_smells'; op='GT'; error='0'},
    @{metric='sqale_rating'; op='GT'; error='1'}
)
foreach ($condition in $conditions) {
    if (!($existing.conditions | Where-Object metric -eq $condition.metric)) {
        $condition.gateName = $gate
        Invoke-Sonar 'qualitygates/create_condition' $condition | Out-Null
    }
}
Invoke-Sonar 'qualitygates/select' @{gateName=$gate; projectKey=$project} | Out-Null
$tokenPath = Join-Path $local 'sonar-token.xml'
if (!(Test-Path $tokenPath)) {
    $token = Invoke-Sonar 'user_tokens/generate' @{name="parceiroauto-local-$([guid]::NewGuid().ToString('N'))"; type='PROJECT_ANALYSIS_TOKEN'; projectKey=$project}
    ConvertTo-SecureString $token.token -AsPlainText -Force | Export-Clixml $tokenPath
}
Write-Host 'Projeto, token e Quality Gate configurados em http://127.0.0.1:9000'
