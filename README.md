# SINAL BT — scanner Bluetooth e controle local de mídia

Projeto Android nativo Kotlin / Jetpack Compose mantido por **João Guilherme — RootScope**.

Instagram: https://www.instagram.com/rootscope.br/

## Recursos

- Scanner BLE/Classic, leitura RSSI, serviços, UUIDs e sinais Fast Pair.
- Pesquisa e filtros por nome, MAC, tipo sugerido, UUID, pareamentos e conexões locais.
- Conexão BLE/GATT real e pareamento pelo Android, com estados de erro e tempo limite.
- Consulta de perfis de áudio deste celular (A2DP/HFP).
- Painel de mídia com controles do player local, volume e um som curto de teste enviado ao dispositivo de saída selecionado no Android.
- Histórico e exportação JSON.
- Análise não invasiva relacionada à CVE-2025-36911 (WhisperPair).

**Limite:** o aplicativo não força pareamento de terceiros, não recebe nem grava microfones, não garante que dispositivos em uso por outro celular aceitem outra conexão e não comprova uma CVE apenas pela presença de Fast Pair.

## Como usar

1. Instale a versão beta no Android 8.0 ou superior.
2. Ative o Bluetooth e conceda as permissões solicitadas.
3. Em **Scanner**, selecione BLE, Classic ou Ambos e execute uma busca.
4. Selecione um dispositivo para conferir nome anunciado, MAC, categoria sugerida, RSSI, dados de fabricante e serviços.
5. Quando houver BLE, use **Conectar por BLE/GATT**. Para Classic/A2DP, conclua o pareamento e selecione a saída nas **Configurações Bluetooth do Android**.
6. Em **Mídia**, confira saídas disponíveis, ajuste volume, teste o som e envie comandos à sessão de mídia do próprio celular.
7. Exporte o histórico na aba **Registros**.

O botão **Reproduzir som de teste** gera um sinal de 660 Hz por cerca de 0,75 segundos. Sem saída Bluetooth selecionada, o áudio poderá sair pelo alto-falante do celular. Use volume moderado.

## Desenvolvimento

- JDK 17, Android SDK 35 e Gradle 8.9.
- Compilar: `gradle :app:assembleDebug`.
- Testes: `gradle :app:testDebugUnitTest :app:lintDebug`.
- CI: `.github/workflows/android.yml`.
- Distribuição beta: [Releases](https://github.com/joaoguirqueiroz/SINALBT/releases).
- Releases oficiais para atualizações in-place exigem a configuração única e segura dos segredos de assinatura Android descritos em `PRIMEIROS_PASSOS.md`.

## Crédito e segurança

Pesquisa e desenvolvimento RootScope — João Guilherme. Projeto inspirado em informações públicas sobre Bluetooth, Google Fast Pair e CVE-2025-36911; não integra a exploração ativa do ZWhisper.
