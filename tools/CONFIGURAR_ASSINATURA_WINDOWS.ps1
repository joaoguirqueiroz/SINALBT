#requires -Version 5.1
<##
.SYNOPSIS
  Configura uma única vez a chave privada do SINAL BT como GitHub Actions Secrets.
  Execute APENAS no seu computador, nunca no GitHub Actions.
#>
param([string]$Repositorio = "joaoguirqueiroz/SINALBT")
$ErrorActionPreference = "Stop"
function Need($name) { if (-not (Get-Command $name -ErrorAction SilentlyContinue)) { throw "Ferramenta ausente: $name. Instale e execute novamente." } }
Need keytool
Need gh
& gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Faça login primeiro: gh auth login" }

$secure = Read-Host "Defina uma SENHA FORTE para a chave (guarde fora do GitHub)" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
if ($password.Length -lt 12) { throw "Use senha com pelo menos 12 caracteres." }

$localFolder = Join-Path (Get-Location).Path '.local_signing'
New-Item -ItemType Directory -Force -Path $localFolder | Out-Null
$keystore = Join-Path $localFolder 'sinal-bt-release.jks'
if (-not (Test-Path $keystore)) {
  & keytool -genkeypair -noprompt -keystore $keystore -storetype PKCS12 -alias sinalbt `
    -keyalg RSA -keysize 3072 -validity 10000 -storepass $password -keypass $password `
    -dname 'CN=SINAL BT RootScope, OU=Security Research, O=RootScope, C=BR'
  if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar a chave.' }
} else {
  Write-Host 'A chave existente será reutilizada. Nunca substitua esta chave após distribuir o aplicativo.'
  & keytool -list -keystore $keystore -storepass $password -alias sinalbt | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'Senha incorreta ou keystore incompatível.' }
}
# Evitar exibir dados sensíveis no terminal; gh secret lê o conteúdo pela entrada padrão.
$encoded = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystore))
$encoded | & gh secret set SINAL_SIGNING_KEYSTORE_B64 -R $Repositorio
if ($LASTEXITCODE -ne 0) { throw 'Falha ao enviar o segredo da chave.' }
$password | & gh secret set SINAL_SIGNING_STORE_PASSWORD -R $Repositorio
if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar senha de armazenamento.' }
'sinalbt' | & gh secret set SINAL_SIGNING_KEY_ALIAS -R $Repositorio
if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar alias.' }
$password | & gh secret set SINAL_SIGNING_KEY_PASSWORD -R $Repositorio
if ($LASTEXITCODE -ne 0) { throw 'Falha ao configurar senha da chave.' }
Write-Host ''
Write-Host 'SEGREDOS CONFIGURADOS. Guarde a chave privada em local offline e seguro:' -ForegroundColor Green
Write-Host $keystore
Write-Host 'IMPORTANTE: Faça backup da chave e senha. Não compartilhe com ninguém nem publique no GitHub.'
Write-Host 'O próximo push na branch main produzirá automaticamente uma Release assinada.'
