# Guia de desenvolvimento e publicações

## Melhorando o código

Edite as classes Kotlin em `app/src/main/java/br/com/vertice81/sinalbt/`. Mantenha o mesmo `applicationId`, a assinatura de distribuição e a configuração `UPDATES_REPO` após a primeira publicação. Não incorpore tokens ou senhas no APK.

## Publicação automatizada

Toda alteração com `git push origin main` dispara dois processos:

**Validação:** estrutura, SDK, testes unitários, Android lint e compilação debug; anexa APK em Artifacts.

**Release oficial:** se a chave estiver configurada em GitHub Actions Secrets, assina o APK, verifica o apksigner, calcula SHA-256 e publica GitHub Release com tag `v1.4.N`, em que `N` é o contador de execuções do workflow.

Em cada build o `versionCode` aumenta automaticamente; não altere manualmente o identificador `br.com.vertice81.sinalbt`.

### Publicação pelo Windows

Se estiver usando Git local, rode:

```powershell
powershell -ExecutionPolicy Bypass -File tools/PUBLICAR_ATUALIZACAO_WINDOWS.ps1 -Mensagem "Melhorias no scanner Bluetooth"
```

Sem Git instalado, use o editor online do GitHub, faça commit na branch `main` e aguarde a Actions.

### Solução de falhas

- **Nenhum workflow aparece:** confira se enviou a pasta oculta `.github` e se Actions está habilitado.
- **Falha no SDK:** confira se o runner tem Android SDK e ferramentas Android 35; veja logs da etapa SDK.
- **Falha no lint/testes:** abra o log, corrija Kotlin/manifest, faça push novamente.
- **Build verde mas sem Release:** configure os quatro Secrets de assinatura e faça um novo push.
- **Instalador acusa aplicativo não instalado:** certificado diferente, versãoCode regressivo, compatibilidade ou restrições do Android; exporte histórico antes de migrar de uma versão debug para release.
- **Atualizador diz repositório inexistente:** o app exige `joaoguirqueiroz/sinal-bt-android` **público**, com Release oficial (não apenas Artifacts).
- **Não encontrou dispositivo:** Bluetooth ligado, permissões próximas e, no Android 8–11, localização quando exigida; alguns aparelhos não anunciam enquanto conectados.

### Segurança

Sem extração de áudio, sem bypass de autenticação e sem conexão forçada com aparelhos de terceiros. Apenas testes em hardware próprio ou com autorização explícita.
