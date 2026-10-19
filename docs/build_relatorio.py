#!/usr/bin/env python3
"""Gera docs/relatorio-etapa2.pdf = relatorio-etapa2.html (com evidencias resolvidas) + relatorio-etapa1.pdf (Anexo I).
Uso (dentro de docs/):  python3 build_relatorio.py
Requisitos: wkhtmltopdf e pdfunite (poppler) ou qpdf.
Para cada <div class="ev" data-src="X.png">: se docs/evidencias/X.png existir, a imagem entra no PDF;
senao entra o aviso 'Pendente de registro'. No checklist, 'Testado' so aparece se o print existir."""
import os, re, shutil, subprocess, sys
AQUI = os.path.dirname(os.path.abspath(__file__)); os.chdir(AQUI)
html = open("relatorio-etapa2.html", encoding="utf-8").read()
existe = lambda f: os.path.isfile(os.path.join("evidencias", f))

def ev(m):
    f, leg = m.group(1), m.group(2)
    if existe(f):
        return f'<div class="ev"><img src="evidencias/{f}"><div class="leg">Evidência: {leg}</div></div>'
    return (f'<div class="ev"><div class="pendente"><strong>Pendente de registro:</strong> {leg}<br>'
            f'(salvar o print em <code>docs/evidencias/{f}</code> e regenerar o PDF)</div></div>')
html = re.sub(r'<div class="ev" data-src="([^"]+)" data-leg="([^"]*)"></div>', ev, html)

def linha(m):
    f = m.group(1); corpo = m.group(0)
    cel = (f'<td><span class="ok">Testado</span> — {f}</td>' if existe(f)
           else f'<td><span class="pend">Pendente de registro</span> ({f})</td>')
    corpo = corpo.replace('<td class="t"></td>', cel)
    return corpo.replace(f' data-ev="{f}"', "")
html = re.sub(r'<tr data-ev="([^"]+)">.*?</tr>', linha, html, flags=re.S)
html = re.sub(r"<script>.*?</script>", "", html, flags=re.S)   # sem JS no PDF

resolvido = os.path.join(AQUI, "_resolvido.html")
open(resolvido, "w", encoding="utf-8").write(html)
corpo = "/tmp/_etapa2_corpo.pdf" if os.name != "nt" else os.path.join(AQUI, "_corpo.pdf")
subprocess.check_call(["wkhtmltopdf", "--quiet", "--enable-local-file-access", "--page-size", "A4",
    "--margin-top", "16mm", "--margin-bottom", "16mm", "--margin-left", "14mm", "--margin-right", "14mm",
    "--footer-center", "Grupo 4 - Oficina Mecanica - Etapa 2 - pag. [page]", "--footer-font-size", "7",
    resolvido, corpo])
os.remove(resolvido)
saida = "relatorio-etapa2.pdf"
if shutil.which("pdfunite"):
    subprocess.check_call(["pdfunite", corpo, "relatorio-etapa1.pdf", saida])
else:
    subprocess.check_call(["qpdf", "--empty", "--pages", corpo, "relatorio-etapa1.pdf", "--", saida])
pend = len(re.findall("Pendente de registro", html))
print(f"Gerado {saida}. Evidencias pendentes (itens+checklist): {pend}")
