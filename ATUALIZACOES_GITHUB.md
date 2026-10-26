# Atualizações via GitHub — fluxo automático

As atualizações do SINAL BT 1.4.x são publicadas automaticamente a cada push na branch main, desde que a chave permanente esteja cadastrada nos GitHub Actions Secrets. Não é preciso criar tags manualmente ou aumentar versionCode a cada release: o workflow usa o contador do GitHub.

**Primeira configuração:** siga [PRIMEIROS_PASSOS.md](PRIMEIROS_PASSOS.md) e execute `tools/CONFIGURAR_ASSINATURA_WINDOWS.ps1` após fazer login no GitHub CLI. Preserve um backup offline criptografado da chave privada e a senha. **Não inclua `.jks` no GitHub.**

**Novas versões:** altere o código, faça commit e push em main. Quando o build for aprovado, o GitHub cria uma Release assinada v1.4.N contendo APK e `SHA256SUMS.txt`.

**No celular:** em **Sobre → Atualizações oficiais pelo GitHub**, o usuário pode verificar novas versões, baixar dentro do app e abrir a instalação do Android. Para automação mais ampla, o Obtainium também acompanha releases públicas.

O Android ainda distribui pacotes APK internamente e pode solicitar autorização. O primeiro download deve acontecer por um canal de distribuição confiável. A atualização exige que o APK novo preserve a mesma assinatura e identificador de pacote; a versão de desenvolvimento v1.3.0 tem assinatura de debug e poderá exigir uma desinstalação inicial.

**Repositório configurado no código:** `joaoguirqueiroz/SINALBT`. Ele deve ser público para que o app consulte `releases/latest` sem token. Mantenha o site RootScope privado.
