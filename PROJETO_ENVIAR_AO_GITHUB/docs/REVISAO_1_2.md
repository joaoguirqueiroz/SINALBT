# SINAL BT 1.2.0 — relatório de revisão

## Alterações técnicas

1. **Medição BLE e Classic:** rejeita RSSI fora de -127..20 dBm, incluindo o sentinela -32768 do Android; exibe diferença entre RSSI do anúncio e RSSI da conexão GATT.
2. **Cache de scanner:** elimina leituras antigas ao iniciar nova busca, preservando pareados e sessão GATT ativa; mantém endereço como identidade de deduplicação.
3. **Fast Pair:** reconhece serviço FE2C e aceita Model ID de três bytes; não deriva comprovação de CVE, marca/modelo ou autorização.
4. **Conexão BLE/GATT:** timeouts, resumo de serviços/quantidade de características, leitura opcional do Battery Service (0x180F) e atualização manual de RSSI do link.
5. **Bluetooth Classic:** leitura SDP com timeout, legendas de perfis A2DP/AVRCP/HFP e verificação local de perfis do celular, sem acessar áudio.
6. **Permissões:** separação de busca de conexão; registro compatível de broadcast receivers e orientação sobre serviço de localização no Android 8–11.
7. **Interface:** filtro por nome/MAC/Model ID, ordenação por sinal ou nome, dados de último avistamento, textos em contraste superior, botões com altura mínima e empilhamento em escala de fonte alta.
8. **Compilação:** versão 1.2.0, scripts para Windows/Linux, workflow automático do GitHub Actions e teste de invariantes do modelo em JVM.

## Validações realizadas neste ambiente

- Verificação estática de estrutura, manifesto, permissões e XML: **concluída**.
- Compilação Kotlin/JVM isolada do modelo e 13 verificações: **concluídas**.
- Checagem de consistência de caminhos/arquivos de CI: **concluída**.
- Build completo do Android/Gradle e instalação do APK: **não realizados** — Android SDK não disponível neste ambiente e download bloqueado.
- Testes em hardware real e escalas de fonte: **pendentes**.

## Interpretação responsável dos resultados

- Um link GATT confirmado não implica áudio Bluetooth Classic.
- Nenhuma leitura do Android identifica se um dispositivo está conectado ao celular de outra pessoa.
- Conexão multiponto pode ser comportamento esperado do fabricante.
- Presença de serviço Fast Pair e de uma característica GATT não confirma vulnerabilidade à CVE-2025-36911.
- Testes devem ser realizados apenas em aparelhos próprios ou com autorização.

## Antes de marcar estável

- Actions: executar `testDebugUnitTest`, `lintDebug` e `assembleDebug`.
- Testar Android 8–15, permissões concedidas/negadas, Bluetooth desativado, scan classic/BLE, SDP timeout, GATT erro 133, desconexão e reconexão.
- Verificar layout em 320, 360 e 412 dp com fontes 100%, 130% e 170%.
- Confirmar o funcionamento em caixa Bluetooth Classic sem BLE, BLE sem GATT e periférico multiponto.
