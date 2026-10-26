#requires -Version 5.1
# Publicação simplificada: cada novo commit na main inicia testes e uma release assinada.
param([string]$Mensagem = "Atualizacao SINAL BT")
$ErrorActionPreference = "Stop"
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { throw 'Instale o Git para Windows.' }
$branch = (& git branch --show-current).Trim()
if ($branch -ne 'main') { throw "Voce esta na branch '$branch'. Mude para main antes de publicar." }
& git add -A
& git diff --cached --quiet
if ($LASTEXITCODE -eq 0) { throw 'Nenhuma alteracao para publicar.' }
& git commit -m $Mensagem
if ($LASTEXITCODE -ne 0) { throw 'Falha ao criar commit.' }
& git push origin main
if ($LASTEXITCODE -ne 0) { throw 'Falha no push. Confira autenticacao e conexao Git.' }
Write-Host 'Push realizado. Acompanhe Actions; a Release sera gerada se a chave estiver configurada.'
