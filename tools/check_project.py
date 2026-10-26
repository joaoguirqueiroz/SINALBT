#!/usr/bin/env python3
"""Revisão verificável de arquivos essenciais, permissões e consistência do projeto."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
errors = []
def check(test, message):
    if not test:
        errors.append(message)

core = [
    'settings.gradle.kts', 'build.gradle.kts', 'app/build.gradle.kts',
    'app/src/main/AndroidManifest.xml',
    'app/src/main/java/br/com/vertice81/sinalbt/MainActivity.kt',
    'app/src/main/java/br/com/vertice81/sinalbt/bluetooth/BluetoothController.kt',
    'app/src/main/java/br/com/vertice81/sinalbt/bluetooth/EventStore.kt',
    'app/src/main/java/br/com/vertice81/sinalbt/ui/SinalApp.kt',
    'app/src/main/java/br/com/vertice81/sinalbt/ui/Design.kt',
    'app/src/main/java/br/com/vertice81/sinalbt/model/DeveloperInfo.kt',
    'app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml',
    'LICENSE', 'README.md', 'INSTALAR_APK.md', 'docs/REVISAO_1_2.md', 'docs/REVISAO_1_3.md', 'BUILD_APK_WINDOWS.bat', 'build-apk.sh',
]
for relative in core:
    check((root / relative).is_file(), f'Arquivo faltando: {relative}')

manifest = root / 'app/src/main/AndroidManifest.xml'
if manifest.exists():
    try:
        document = ET.parse(manifest)
        values = [n.attrib.get('{http://schemas.android.com/apk/res/android}name')
                  for n in document.iter('uses-permission')]
        for p in ['android.permission.RECORD_AUDIO',
                  'android.permission.BLUETOOTH_PRIVILEGED']:
            check(p not in values, f'Permissão indevida: {p}')
        for p in ['android.permission.BLUETOOTH_SCAN', 'android.permission.BLUETOOTH_CONNECT', 'android.permission.INTERNET', 'android.permission.REQUEST_INSTALL_PACKAGES']:
            check(p in values, f'Permissão exigida ausente: {p}')
    except ET.ParseError as exc:
        errors.append(f'Manifest inválido: {exc}')

for resource in (root / 'app/src/main/res').rglob('*.xml'):
    try:
        ET.parse(resource)
    except ET.ParseError as exc:
        errors.append(f'XML inválido {resource.relative_to(root)}: {exc}')

controller = (root / 'app/src/main/java/br/com/vertice81/sinalbt/bluetooth/BluetoothController.kt')
if controller.exists():
    content = controller.read_text()
    for required in ['connectGatt(', 'onConnectionStateChange(', 'onServicesDiscovered(',
                     'inspectClassicServices(', 'inspectLocalAudio(', 'clearConnectionTimeout(',
                     'closeAudioInspection(', 'scanGeneration']:
        check(required in content, f'Funcionalidade não encontrada: {required}')
    check('writeCharacteristic(' not in content, 'Escrita GATT inesperada no diagnóstico')
    for required in ['validRssi(', 'hasScanPermission(', 'hasConnectPermission(',
                     'refreshConnectedRssi(', 'BATTERY_LEVEL', 'ContextCompat.registerReceiver',
                     'resetDiscoveryCache(']:
        check(required in content, f'Correção v1.2 não encontrada: {required}')

ui = (root / 'app/src/main/java/br/com/vertice81/sinalbt/ui/SinalApp.kt')
if ui.exists():
    content = ui.read_text()
    for name in ['ResponsiveActionPair(', 'FastPairDiagnostics(', 'DeviceDetail(',
                 'ExplorerScreen(', 'HistoryScreen(', 'SessionsScreen(',
                 'OutlinedTextField(', 'Última detecção', 'Atualizar RSSI do link']:
        check(name in content, f'Tela/função não encontrada: {name}')

build = (root / 'app/build.gradle.kts').read_text()
check('SINAL_VERSION_NAME' in build and '1.5.0' in build, 'Versão automática não configurada')
check('GITHUB_RUN_NUMBER' in build and '15000' in build, 'VersionCode progressivo não configurado')

about_info = (root / 'app/src/main/java/br/com/vertice81/sinalbt/model/DeveloperInfo.kt').read_text()
check('https://www.instagram.com/rootscope.br/' in about_info, 'URL do Instagram incorreta')
check('@rootscope.br' in about_info, 'Identificador RootScope ausente')
check('DeveloperInfo.INSTAGRAM_URL' in ui.read_text(), 'Tela Sobre não usa o link do desenvolvedor')
check('Abrir Instagram' in ui.read_text(), 'Botão do Instagram ausente')
workflow = (root / '.github/workflows/android.yml').read_text()
for task in ['testDebugUnitTest', 'lintDebug', 'assembleDebug', 'assembleRelease', 'upload-artifact', 'release create', 'SINAL_SIGNING_KEYSTORE_B64', 'github.run_number']:
    check(task in workflow, f'Etapa faltando no GitHub Actions: {task}')

update_source = root / 'app/src/main/java/br/com/vertice81/sinalbt/updates/GithubUpdateManager.kt'
check(update_source.is_file(), 'Módulo de atualização ausente')
if update_source.is_file():
    up = update_source.read_text()
    for token in ['DownloadManager', 'releases/latest', 'compareVersions(', 'canRequestPackageInstalls(', 'sha256:', 'getUriForDownloadedFile(', 'installDownloaded(']:
        check(token in up, f'Atualização incompleta: {token}')
    check('USERNAME_TOKEN' not in up, 'Token de acesso não deve ser incluído no código')
ui_source = ui.read_text() if ui.exists() else ''
check('GithubUpdateManager.Phase.AVAILABLE' in ui_source, 'Tela de atualizações não está ligada ao fluxo')
if errors:
    for err in errors:
        print('FALHA:', err)
    sys.exit(1)
print(f'PASSOU — {len(core)} arquivos essenciais, permissões, XML e fluxos básicos consistentes.')
print('ATENÇÃO: esta verificação não substitui compilação Android e teste físico.')
