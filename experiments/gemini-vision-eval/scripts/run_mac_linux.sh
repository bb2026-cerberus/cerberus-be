#!/usr/bin/env bash
set -e
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

if [ ! -f .env ]; then cp .env.example .env; fi
echo "Edit .env and put GEMINI_API_KEY, then run: python run_all.py"
