# Instalação do SINAL BT no Android

## Beta de testes — pronta agora

O pacote de entrega possui `SINAL_BT_v1.3.0_beta_instalavel.apk`, compilado pelo GitHub Actions e validado por SHA-256. Transfira ao telefone, abra e permita a instalação pelo aplicativo de arquivos quando o Android solicitar. Essa compilação **não foi testada fisicamente** e foi assinada com chave debug.

## Versão oficial atualizável

Crie o repositório público, envie o projeto e configure a assinatura conforme [PRIMEIROS_PASSOS.md](PRIMEIROS_PASSOS.md). A primeira Release oficial será gerada automaticamente após um push na branch main e ficará disponível em GitHub Releases. Instale a Release oficial uma única vez; depois use **Sobre → Atualizações oficiais pelo GitHub** para atualizar pelo próprio app.

**Nota:** se a assinatura da versão beta for diferente, exporte relatórios e desinstale a beta antes de instalar a oficial. Android não aceita atualizar uma instalação assinada com outra chave.

**Permissões:** Bluetooth/Dispositivos próximos para buscar e conectar; localização em Android 8–11 quando exigida; rede somente para consultar GitHub Releases; autorização de instalação de apps desconhecidos somente se iniciar uma atualização.
