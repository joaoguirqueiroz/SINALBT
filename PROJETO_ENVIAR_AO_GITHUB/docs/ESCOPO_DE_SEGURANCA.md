# Escopo do diagnóstico

A CVE-2025-36911 diz respeito a determinados dispositivos que implementam Google Fast Pair. A identificação de 0xFE2C não indica, por si só, que haja falha de autenticação.

**Implementado:** descobrir dispositivos, abrir conexão GATT normal, descobrir serviços e características, iniciar pareamento padrão Android com confirmação do usuário, registrar eventos, apresentar a documentação de risco.

**Não implementado:** payload de Key-Based Pairing não autenticado, bypass criptográfico, gravação/escuta de microfone, alteração de Account Keys, rastreamento Find Hub, repetição agressiva de conexões, API escondida `BLUETOOTH_PRIVILEGED` ou forçar que um periférico desconecte outra pessoa.

Qualquer relatório deve registrar o resultado como **não determinado** para a CVE: os mecanismos deste aplicativo não executam validação conclusiva dela. Nenhum resultado se chama VULNERÁVEL ou CORRIGIDO com base somente no anúncio ou na conexão.

Privacidade: a permissão BLUETOOTH_SCAN não é usada para localização, a flag `neverForLocation` está habilitada para Android 12+; isso pode filtrar anúncios de alguns dispositivos. Em Android 11 ou anterior, o sistema operacional exige localização para descobrir BLE, mas o aplicativo não usa GPS e não possui permissão de internet.
