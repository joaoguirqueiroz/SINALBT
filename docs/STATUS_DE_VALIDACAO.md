# Estado de validação — 26/10/2026

- **Versão beta 1.3.0:** compilada pelo GitHub Actions em execução `40378008994`, com artefato `12675412351`. Arquivo APK extraído, SHA256 `12d62e0c95a8062c094cae8a05e43283f193c55525dcc5f17e5414388b6637ef` e formato ZIP/APK íntegro.
- **Versão do projeto 1.4.0:** arquivos conferidos e modelo Kotlin executado em testes locais. A compilação do projeto atualizado ainda precisa ser concluída no GitHub após publicação.
- **App instalado em telefone:** não testado neste ambiente. Não alegar que todos os modelos Bluetooth conectam, nem que foi executado um teste completo de CVE.
- **Releases assinadas:** dependem da configuração de quatro Secrets e de um novo push. A chave secreta não pode ser incluída no projeto público.
- **Distribuição automática:** o aplicativo consulta GitHub Releases quando aberto e pede consentimento para instalar atualizações pelo Android.
