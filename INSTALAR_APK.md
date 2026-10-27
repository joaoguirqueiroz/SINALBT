# SINAL BT — instalação pelo GitHub Releases

**Desenvolvedor:** João Guilherme / RootScope · Instagram: @rootscope.br.

## Baixe sempre a versão mais nova

Página oficial: https://github.com/joaoguirqueiroz/SINALBT/releases

1. Abra o link no **Google Chrome**, em vez de usar o navegador interno do aplicativo GitHub.
2. Entre na versão beta mais recente e expanda **Assets**.
3. Toque em `SINAL-BT-vX.Y.Z-beta.apk` (não escolha `Source code` nem `SHA256SUMS.txt`).
4. Aguarde o download, abra **Arquivos → Downloads** e toque no arquivo terminado em `.apk`.
5. Se o Android solicitar autorização, abra as configurações de **Instalar apps desconhecidos**, habilite o Chrome ou o gerenciador de arquivos usado e confirme a instalação.

## Se aparecer “App não instalado”

O Android exige que atualizações mantenham **a mesma chave de assinatura**.
As betas anteriores foram geradas com assinatura de depuração do GitHub Actions, que pode mudar entre execuções.

- Se o SINAL BT já estiver instalado, entre primeiro na aba **Registros** e use **Exportar JSON** para salvar seu histórico.
- Depois, em **Configurações → Apps → SINAL BT → Desinstalar**, retire a beta antiga.
- Abra o novo APK em **Downloads**, autorize a origem e instale.
- Atenção: desinstalar pode apagar dados locais que não tenham sido exportados.

## Se o arquivo não baixar

- Abra https://github.com/joaoguirqueiroz/SINALBT/releases no Chrome.
- Toque no nome do arquivo APK em **Assets**.
- Confira se há espaço livre (recomenda-se pelo menos 100 MB).
- Não renomeie o arquivo para .zip nem tente instalar o ZIP de GitHub Actions como um aplicativo.

## Se instalar mas não abrir

- Verifique se o Android é versão 8.0 ou superior.
- Em **Configurações → Apps → SINAL BT → Permissões**, permita **Dispositivos próximos/Bluetooth** quando solicitado.
- Reinicie o Bluetooth e abra novamente o aplicativo.
- Se o fechamento continuar, envie o modelo do aparelho, versão Android e a mensagem exibida. O resultado de uma compilação bem-sucedida não substitui testes em celulares reais.

## Atualizações pelo próprio aplicativo

O SINAL BT foi preparado para consultar tanto **GitHub Releases beta quanto oficiais**, mas uma beta pode exigir reinstalação quando a assinatura tiver mudado. Para atualizações confiáveis sem desinstalar, o projeto deve usar **uma chave de assinatura permanente** nos segredos do GitHub Actions.

Permissões de microfone ou gravação não são solicitadas. O aplicativo não explora ativamente a CVE-2025-36911, nem força pareamentos Bluetooth.
