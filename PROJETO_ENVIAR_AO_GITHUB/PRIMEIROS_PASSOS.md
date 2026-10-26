# Primeiros passos — GitHub Actions e aplicativo instalável

### 1. Instale o APK beta para conhecer o app

Baixe do pacote de entrega `SINAL_BT_v1.3.0_beta_instalavel.apk`. Ele foi compilado em GitHub Actions, mas **não foi testado em hardware real**. Permita a instalação pelo navegador/gerenciador de arquivos quando o Android solicitar.

### 2. Crie um repositório de distribuição público

Abra `https://github.com/new`, preencha **Repository name: `sinal-bt-android`**, Visibility **Public** e crie sem README adicional. **Não torne público o repositório privado do site RootScope**.

### 3. Suba o código

Extraia o pacote no PC; envie **somente os arquivos e pastas de `PROJETO_ENVIAR_AO_GITHUB/`** à raiz do repositório público. O diretório oculto `.github` precisa ser incluído. Use o botão **Add file → Upload files**, ou Git:

```bash
git init
git branch -M main
git add .
git commit -m "feat: SINAL BT com atualizacoes e GitHub Actions"
git remote add origin https://github.com/joaoguirqueiroz/sinal-bt-android.git
git push -u origin main
```

### 4. Baixe o primeiro APK gerado pelo seu GitHub

Abra `https://github.com/joaoguirqueiroz/sinal-bt-android/actions` → última execução verde → **Artifacts → SINAL-BT-v1.4.X-debug**. Esse é um APK de testes, criado automaticamente a cada push na branch main.

### 5. Uma única vez, configure a chave de assinatura

No Windows: instale o JDK (para `keytool`) e [GitHub CLI](https://cli.github.com/), faça `gh auth login` e execute na pasta do repositório:

```powershell
powershell -ExecutionPolicy Bypass -File tools/CONFIGURAR_ASSINATURA_WINDOWS.ps1
```

**Crítico:** guarde cópia offline da pasta `.local_signing/` e da senha. Não envie a chave no GitHub, não mostre em fotos ou conversas. A perda da chave impede instalar atualizações sobre versões assinadas com ela.

### 6. Gere uma Release oficial automaticamente

Depois da assinatura, basta criar um commit na branch main (por exemplo, corrigir uma linha no README) e fazer `git push origin main`. O pipeline compila e cria `Releases → v1.4.X`, disponibilizando o instalador oficial para todos.

Daí em diante, quando houver código novo, faça push. O aplicativo instalado consultará a versão mais recente pela aba Sobre e abrirá o instalador do Android após baixar; sem procurar manualmente APKs.

**Antes de migrar a beta 1.3.0 para a versão oficial**, exporte relatórios e desinstale a beta se houver incompatibilidade de assinatura. Instale a primeira release oficial uma vez e, depois disso, atualize normalmente.
