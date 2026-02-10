# PowerShell helper (run from project root)
python -m venv .venv
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt

if (!(Test-Path .env)) { Copy-Item .env.example .env }
Write-Host "Edit .env and put GEMINI_API_KEY, then run: python run_all.py"
