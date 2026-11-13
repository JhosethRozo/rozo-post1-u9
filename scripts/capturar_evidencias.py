"""Captura reportes reales y un visor HTML de logs; no genera resultados simulados."""
import argparse
import html
from pathlib import Path
import subprocess

from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
EVIDENCIAS = ROOT / 'docs/evidencias'


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT).decode('utf-8', errors='replace')


def leer(nombre, lineas=32):
    return '\n'.join((EVIDENCIAS / nombre).read_text(encoding='utf-8', errors='replace').splitlines()[-lineas:])


def codigo(prefijo):
    return '\n'.join(f'--- {p.relative_to(ROOT)} ---\n{p.read_text(encoding="utf-8")}'
                     for p in sorted((ROOT / prefijo).rglob('*.java')))


def capturar_visor(page, nombre, titulo, contenido):
    visor = ROOT / '.tmp' / f'{nombre}.html'
    visor.parent.mkdir(exist_ok=True)
    visor.write_text('<!doctype html><meta charset="utf-8"><style>'
                     'body{background:#101827;color:#e5edf8;margin:32px;font:15px Consolas,monospace}'
                     'h1{color:#78e0a2;font:24px Arial}p{color:#aabbd0;font:15px Arial}'
                     'pre{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.45}'
                     '</style><h1>' + html.escape(titulo) + '</h1>'
                     '<p>Unidad 9 · Visor de evidencia real: código, salida de comandos y reportes guardados.</p>'
                     '<pre>' + html.escape(contenido) + '</pre>', encoding='utf-8')
    page.goto(visor.as_uri())
    page.screenshot(path=str(EVIDENCIAS / f'{nombre}.png'), full_page=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--chrome', default=r'C:\Program Files\Google\Chrome\Application\chrome.exe')
    parser.add_argument('--workflow-url')
    args = parser.parse_args()
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path=args.chrome, headless=True)
        page = browser.new_page(viewport={'width': 1440, 'height': 1000}, device_scale_factor=1)
        capturar_visor(page, '01-compilacion', 'Checkpoint 1 — mvn compile', leer('01-compile.log', 18))
        capturar_visor(page, '02-dominio', 'Checkpoint 2 — Entidad, enum y JpaRepository',
                       codigo('src/main/java/com/clinica/recordatorios/domain') + '\n' +
                       codigo('src/main/java/com/clinica/recordatorios/repository'))
        capturar_visor(page, '03-strategy-factory', 'Checkpoint 3 — Dos estrategias y Factory por constructor',
                       codigo('src/main/java/com/clinica/recordatorios/channel'))
        capturar_visor(page, '04-arranque', 'Checkpoint 4 — Spring Boot y H2 vacía', leer('04-arranque.log', 22))
        capturar_visor(page, '05-parte1-pruebas', 'Checkpoint 5 — Tres pruebas unitarias de la Parte 1', leer('05-parte1-test.log', 21))
        capturar_visor(page, '06-parte1-git', 'Checkpoint 6 — Progreso y decisiones antes de las pruebas',
                       git('log', '--reverse', '--format=%h %s', 'c4c286a..a2bd1106') + '\n' +
                       git('show', '0a67ad01:README.md'))
        capturar_visor(page, '07-antes-dip', 'Paso 7 — Versión inicial y diagnóstico antes del refactor',
                       git('show', '8ceef3f5:src/main/java/com/clinica/recordatorios/service/CitaReminderServiceImpl.java') +
                       '\n' + leer('07-antes-refactor.log', 7))
        capturar_visor(page, '08-despues-dip', 'Checkpoint 8 — DIP y seis pruebas unitarias',
                       codigo('src/main/java/com/clinica/recordatorios/config') + '\n' + leer('08-refactor-test.log', 20))
        capturar_visor(page, '09-jpa', 'Checkpoint 9 — DataJpaTest: filtro y límites inclusivos',
                       (ROOT / 'target/surefire-reports/com.clinica.recordatorios.repository.CitaRepositoryTest.txt').read_text() +
                       '\n' + codigo('src/test/java/com/clinica/recordatorios/repository'))
        capturar_visor(page, '10-integracion', 'Checkpoint 10 — SpringBootTest y verificación Maven',
                       (ROOT / 'target/surefire-reports/com.clinica.recordatorios.CitaReminderServiceIntegrationTest.txt').read_text() +
                       '\n' + leer('10-verify.log', 28))
        page.goto((ROOT / 'target/site/jacoco/index.html').as_uri())
        page.screenshot(path=str(EVIDENCIAS / '13-cobertura.png'), full_page=True)
        page.goto((ROOT / 'target/site/jacoco/com.clinica.recordatorios.service/index.html').as_uri())
        page.screenshot(path=str(EVIDENCIAS / '14-cobertura-servicio.png'), full_page=True)
        capturar_visor(page, '12-entrega', 'Checkpoint 12 — Historial y estructura final',
                       git('log', '--oneline', '--reverse') + '\n' + git('ls-files'))
        if args.workflow_url:
            page.goto(args.workflow_url, wait_until='networkidle')
            page.screenshot(path=str(EVIDENCIAS / '11-github-actions.png'), full_page=True)
        browser.close()


if __name__ == '__main__':
    main()
