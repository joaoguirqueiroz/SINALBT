# SINAL BT — pacote de entrega

## O que você recebeu

1. **`SINAL_BT_v1.3.0_beta_instalavel.apk`** — APK Android real compilado no GitHub Actions, para testes de instalação. Não é uma versão oficial assinada para atualização contínua.
2. **`PROJETO_ENVIAR_AO_GITHUB/`** — código-fonte 1.4.0, GitHub Actions, publicação automática de versões, telas, logotipo e documentação.

## O que enviar ao GitHub

Envie **somente o CONTEÚDO da pasta `PROJETO_ENVIAR_AO_GITHUB/`** para um repositório **público** chamado `joaoguirqueiroz/sinal-bt-android`. **Não envie o arquivo beta APK nem este README de pacote**.

A cada push em `main` será gerado um APK de teste em Actions. Para que o Actions **publique também a versão oficial em Releases** e o celular possa atualizar sem buscar arquivos manualmente, configure uma vez os Secrets usando o guia `PRIMEIROS_PASSOS.md` e o script `tools/CONFIGURAR_ASSINATURA_WINDOWS.ps1`.

**Importante:** Android sempre requer um pacote APK para distribuição direta; o fluxo automatiza download e disponibilização. A instalação de atualizações pode exigir confirmação. O APK 1.3.0 debug não atualiza diretamente para produção se a assinatura for diferente. O projeto 1.4.0 ainda precisa passar pela compilação do próprio repositório e pelos testes de hardware.
