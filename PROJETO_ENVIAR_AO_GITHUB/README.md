# SINAL BT — RootScope

**Scanner e diagnóstico Bluetooth para Android**, desenvolvido em Kotlin/Jetpack Compose, com suporte BLE, Bluetooth Classic, conexões GATT autorizadas, inspeção de serviços, identificação de anúncios Google Fast Pair e histórico exportável.

**Desenvolvedor:** João Guilherme · RootScope · [@rootscope.br](https://www.instagram.com/rootscope.br/)

> O aplicativo não explora a CVE-2025-36911, não captura áudio e não força o pareamento de terceiros. Conexões GATT não equivalem ao áudio Bluetooth Classic. O aplicativo verifica condições técnicas, não comprova vulnerabilidades sem teste especializado.

## Primeira instalação — já disponível para testes

O pacote de entrega fornecido separadamente contém `SINAL_BT_v1.3.0_beta_instalavel.apk`, compilado por GitHub Actions e validado por SHA-256. Esta versão de depuração **não tem a assinatura permanente de produção**. É adequada a testes iniciais, mas exige reinstalação ao migrar para a versão oficial assinada.

## GitHub: instalar e receber versões sem gerenciar APK manualmente

1. Crie um repositório **público** `joaoguirqueiroz/sinal-bt-android` (o repositório do site `rootscope` deve continuar privado).
2. Envie o **conteúdo desta pasta** para a raiz do repositório, preservando `.github/workflows/android.yml`.
3. A cada push na branch `main`, o GitHub Actions compila, testa e publica um **APK de teste** em `Actions → Artifacts`.
4. Uma única vez, no seu computador com JDK e GitHub CLI, execute `powershell -ExecutionPolicy Bypass -File tools/CONFIGURAR_ASSINATURA_WINDOWS.ps1`. A ferramenta gera/guarda localmente a chave e envia ao repositório **apenas os GitHub Actions Secrets** necessários. Nunca publique a chave no repositório.
5. Depois da configuração, cada push na branch `main` publica também uma **Release oficial assinada**, com versão numerada automaticamente (`v1.4.1`, `v1.4.2`, etc.).
6. No SINAL BT, abra **Sobre → Atualizações oficiais pelo GitHub**; toque **Procurar atualizações** e **Baixar** quando necessário. O Android pedirá autorização antes de instalar; não há instalação silenciosa fora dos mecanismos autorizados.

O **download inicial** da versão oficial será encontrado em [`Releases`](https://github.com/joaoguirqueiroz/sinal-bt-android/releases) quando o repositório e a primeira publicação existirem. Também é possível usar o [Obtainium](https://obtainium.imranr.dev/) para acompanhar futuras versões.

## Guias completos

- [PRIMEIROS_PASSOS.md](PRIMEIROS_PASSOS.md) — passo a passo da primeira publicação.
- [INSTALAR_APK.md](INSTALAR_APK.md) — instalação inicial e permissões Android.
- [ATUALIZACOES_GITHUB.md](ATUALIZACOES_GITHUB.md) — regras de versão, assinatura e atualização.
- [GUIA_DO_DESENVOLVEDOR.md](GUIA_DO_DESENVOLVEDOR.md) — fluxo de trabalho e correções.
- [docs/ESCOPO_DE_SEGURANCA.md](docs/ESCOPO_DE_SEGURANCA.md) — limitações éticas e técnicas.

## Arquitetura

`app/src/main/java/br/com/vertice81/sinalbt/` contém `MainActivity`, `bluetooth/BluetoothController`, `model`, `ui` e `updates/GithubUpdateManager`.

- **Android mínimo:** 8.0 (API 26)
- **Android SDK:** 35
- **Java/JDK:** 17
- **Gradle:** 8.9
- **Identificador:** `br.com.vertice81.sinalbt`
- **Repositório de atualizações:** `joaoguirqueiroz/sinal-bt-android`

## Verificações

`python3 tools/check_project.py` valida arquivos, recursos XML, permissões e fluxos esperados. O workflow executa os testes unitários, Android Lint e a compilação. Passar nos testes automatizados não garante funcionamento em todo aparelho Bluetooth; é necessária validação física em diferentes modelos e versões do Android.

## Código original e créditos

Este é um projeto autoral RootScope de diagnóstico, com pesquisa inspirada em estudos públicos sobre Fast Pair e WhisperPair. Não é uma implementação da exploração ativa do projeto ZWhisper. A licença Apache 2.0 e os avisos de atribuição estão em `LICENSE` e `NOTICE.md`.
