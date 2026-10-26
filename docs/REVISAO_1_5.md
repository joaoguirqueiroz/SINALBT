# SINAL BT — revisão v1.5: interface e mídia

## Design
- Paleta light RootScope (branco/off-white, ciano e grafite), sem neon e sem degradês.
- Símbolo vetorial próprio desenhado no Compose e ícones contextuais para categorias detectáveis.
- Cinco abas: Scanner, Dispositivos, Mídia, Registros e Sobre.
- Painel de mídia em grafite escuro como contraste funcional.
- Ações e informações organizadas em cartões com suporte a fontes ampliadas.

## Scanner e identificação
- Busca BLE + Classic já existente mantida com captura real de RSSI e UUIDs.
- Pesquisa por nome, MAC, categoria sugerida, UUID, fabricante anunciado e Model ID.
- Filtros por Fast Pair, dispositivos de áudio, pareados e conectados a este celular.
- Classificação heurística de caixas, fones, relógios, celulares, computadores e sensores.
- Categoria sugerida **não** valida marca, fabricante ou vulnerabilidade.

## Conexões e áudio
- BLE/GATT e solicitação de pareamento seguem as APIs oficiais do Android.
- Consulta A2DP/HFP do próprio Android para separar conexão de áudio e BLE.
- Nova tela Mídia: lista saídas Bluetooth disponíveis; ajuste do volume de mídia.
- Botões para enviar comandos play/pause e faixa anterior/próxima a sessões de player locais.
- Som sintetizado e curto usando AudioTrack / USAGE_MEDIA, direcionado à saída atual do Android.
- Não grava, intercepta, retransmite nem acessa microfones.
- Não força conexões, não altera perfis A2DP ocultamente e não garante controle remoto de aparelhos já ocupados.
- A lista de saídas disponíveis não comprova qual saída está atualmente roteada; um som de teste pode revelar a rota informada pelo AudioTrack.

## Segurança e testes
- CVE-2025-36911 (WhisperPair) descrita para pesquisa, sem payload de exploração.
- Preserve permissões Android, logs locais e exportação.
- Verificações unitárias para classificação e build no GitHub Actions.
- Teste físico e confirmação de roteamento ainda necessários com os modelos reais.

**Desenvolvedor:** João Guilherme · RootScope · @rootscope.br.
