"""Comprueba una ejecución real de Actions y captura su página pública."""
import json
from pathlib import Path
import sys
import urllib.request

from playwright.sync_api import sync_playwright

root = Path(__file__).resolve().parents[1]
run_id = sys.argv[1]
api = f'https://api.github.com/repos/JhosethRozo/rozo-post1-u9/actions/runs/{run_id}'
with urllib.request.urlopen(api) as response:
    run = json.load(response)
assert run['status'] == 'completed' and run['conclusion'] == 'success', run['conclusion']
evidence = root / 'docs/evidencias'
(evidence / '11-github-actions.json').write_text(json.dumps({
    key: run[key] for key in ('id', 'head_sha', 'html_url', 'status', 'conclusion', 'created_at', 'updated_at')
}, indent=2) + '\n', encoding='utf-8')
with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe')
    page = browser.new_page(viewport={'width': 1440, 'height': 1100})
    page.goto(run['html_url'], wait_until='networkidle', timeout=60000)
    page.screenshot(path=str(evidence / '11-github-actions.png'), full_page=True)
    browser.close()
print(run['html_url'], run['conclusion'])
