# SINAL BT 1.3.0 — Primeira distribuição RootScope

## Alterações
- Identificação do desenvolvedor na aba Sobre: **João Guilherme / RootScope**.
- Botão **Abrir Instagram** apontando para https://www.instagram.com/rootscope.br/.
- Botão **Copiar @rootscope.br** com a área de transferência do Android.
- Tratamento de falha quando nenhum aplicativo consegue abrir o link.
- Atualização de versionName 1.3.0 e versionCode 4.
- Workflow prepara artefato instalável `app-debug.apk` ao executar GitHub Actions.

## Restrições e verificação
- O link externo é aberto por ACTION_VIEW; o aplicativo não tem permissão INTERNET.
- Não há captura de áudio, pareamento não autorizado ou exploração ativa da CVE.
- Projeto revisado localmente; o APK só pode ser afirmado como compilado após o Gradle completar no SDK Android.
- Validar em aparelho físico navegação, Instagram, pedidos de permissão e conectividade.
