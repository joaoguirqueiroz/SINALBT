# SINAL BT — estrutura corrigida no GitHub

Os arquivos do projeto Android estão na raiz deste repositório, com o workflow em `.github/workflows/android.yml`.

- Consulte [README.md](README.md) e [PRIMEIROS_PASSOS.md](PRIMEIROS_PASSOS.md).
- Abra a aba **Actions** para verificar os testes e baixar o APK de desenvolvimento gerado após cada push.
- Para publicar versões oficiais com atualização pelo aplicativo, configure uma única vez os secrets de assinatura conforme [ATUALIZACOES_GITHUB.md](ATUALIZACOES_GITHUB.md).
- O antigo APK beta de depuração está preservado em `downloads/`, mas pode exigir desinstalação ao migrar para a assinatura oficial.

**Repositório:** https://github.com/joaoguirqueiroz/SINALBT

**Segurança:** use apenas em dispositivos próprios ou autorizados. O aplicativo não prova nem explora automaticamente a CVE-2025-36911.
