# Revisão 1.1.0 — rastreabilidade de correções

## Correções implementadas e inspecionadas no código

| Nº | Problema anterior | Alteração na versão 1.1 |
|---|---|---|
| 01 | Um mesmo MAC aparecia em BLE e Classic como dois cartões | Identidade estável por endereço, agregando transportes quando coincidem |
| 02 | Atualização do pareamento podia fechar a tela de detalhes | Chave de navegação baseada no endereço, não no transporte |
| 03 | Busca anterior ainda podia adicionar anúncios após cancelamento | Geração de sessão invalida callbacks antigos |
| 04 | Descoberta GATT sem resposta mantinha estado indefinidamente | Timeout separado para descoberta de serviços |
| 05 | Parear e conectar sem permissão inicial não abriam diálogo Android | Fluxo de permissão compartilhado pelas operações Bluetooth |
| 06 | Dispositivo pareado antigo continuava listado após remoção | Reconciliação de pareamentos no refresh |
| 07 | Interface com botões de largura fixa cortava textos | Botões com altura mínima, duas linhas e agrupamento adaptativo |
| 08 | Cartão do dispositivo tinha chips e RSSI em coluna estreita | Cartão em duas linhas, dados prioritários primeiro |
| 09 | Marca/app icon não acompanhava todos os recursos Android | Ícone adaptativo e paleta unificada |
| 10 | Caixa Bluetooth Classic só exibia UUIDs BLE | Consulta de perfis por SDP, se suportada |
| 11 | Não havia conferência local de perfis de áudio | Consulta A2DP/HFP do próprio aparelho, sem áudio/captura |
| 12 | Estado de permissão negada não aparecia na tela de detalhes | Mensagem de sistema visível no detalhe |

## O que NÃO está validado neste ambiente

Não foram feitos testes instrumentados em Android real, testes de responsividade de renderização
em emulador e compilação Gradle com Android SDK completo. A workflow CI do GitHub executará
análise estática, testes unitários, lint e `assembleDebug`; acompanhe o resultado em Actions.

## Critérios obrigatórios antes de marcar uma versão estável

- Gradle CI e lintDebug verdes;
- scanner BLE + Classic em pelo menos dois celulares reais;
- confirmação/negação de permissões Android 8/11/12/14/15;
- GATT conecta, descobre serviços, trata erro e desconecta;
- SDP pode falhar sem travar;
- A2DP/HFP reportam SOMENTE perfis locais;
- design com fontes de sistema 100%, 130% e 170%, telas 320 dp, 360 dp, 412 dp;
- encerrar tela durante busca/conexão sem vazamento;
- validar registros JSON e permissões negativas;
- verificar compatibilidade de caixa/fone no laboratório autorizado.

A conexão a dispositivos ocupados por outro telefone não é garantida. Um dispositivo
que permite BLE + Classic simultâneos não é necessariamente vulnerável à CVE-2025-36911.
