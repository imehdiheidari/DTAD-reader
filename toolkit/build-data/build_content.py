"""Build content.html for a DTAD reader app from the session bodies, glossary and word audio.

Usage: APP_NAME="DTAD Theory" [DATA_DIR=...] [VERSION=3 UPDATE_BASE=https://raw.githubusercontent.com/OWNER/REPO/main/] python3 -I build_content.py OUT.html
VERSION must match version.json in the repo; with UPDATE_BASE the page also updates itself when opened in a desktop browser.
Sessions are sessions/NN.html (body fragments made by conv.py), listed in sessions/index.json as [["01.html", "Session 1 · Kickoff"], ...].
Every word in a session must have a line in glossary-fa.txt (word|meaning) and a file audio/<word>.mp3
(apostrophes as _); the script prints any that are missing and stops.
"""
import base64, html, json, os, re, sys

HERE = os.path.dirname(os.path.abspath(__file__))
# a course keeps its own sessions/ and glossary-fa.txt in DATA_DIR (e.g. project/build-data); theme and script stay here
DATA = os.environ.get("DATA_DIR", HERE)
# audio/ is shipped as audio.tar: the shared folder mangled thousands of small mp3 files, so extract it locally
AUDIO_DIR = os.environ.get("AUDIO_DIR", os.path.join(HERE, "audio"))
SESSIONS = json.load(open(os.path.join(DATA, "sessions", "index.json"), encoding="utf8"))
APP_NAME = os.environ.get("APP_NAME", "DTAD Theory")
VERSION = int(os.environ.get("VERSION", "0"))
UPDATE_BASE = os.environ.get("UPDATE_BASE", "")

def words_in(fragment):
    text = html.unescape(re.sub(r"<[^>]+>", " ", fragment))
    return {w.lower().replace("’", "'") for w in re.findall(r"[A-Za-zÀ-ÖØ-öø-ɏ][A-Za-zÀ-ÖØ-öø-ɏ'’-]*[A-Za-zÀ-ÖØ-öø-ɏ]|[A-Za-zÀ-ÖØ-öø-ɏ]", text)}

gloss = {}
for line in open(os.path.join(DATA, "glossary-fa.txt"), encoding="utf8"):
    line = line.rstrip("\n")
    if "|" in line:
        k, v = line.split("|", 1)
        gloss[k] = v

# optional NN-fa.html (Persian summary) and NN-todo.html (tasks for the next session) beside NN.html turn on tabs:
# one tab per session, and inside each the transcript, summary and to-do as sub-tabs (tabs.html)
def extra(fname, kind):
    p = os.path.join(DATA, "sessions", fname.replace(".html", f"-{kind}.html"))
    return open(p, encoding="utf8").read() if os.path.exists(p) else None
TABS = any(extra(f, k) for f, _ in SESSIONS for k in ("fa", "todo"))
FA_DIGITS = str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹")

parts, words, nav = [], set(), []
for n, (fname, title) in enumerate(SESSIONS, 1):
    frag = open(os.path.join(DATA, "sessions", fname), encoding="utf8").read()
    words |= words_in(frag)
    head = f'<h2 class="session-title">{html.escape(title)}</h2>\n'
    if not TABS:
        parts.append(head + frag)
        continue
    sid = fname.split(".")[0]
    nav.append(f'<button type="button" role="tab" data-s="{sid}">جلسه {str(n).translate(FA_DIGITS)}</button>')
    panes = [("text", "متن کلاس", frag, "")]
    for kind, label in (("fa", "خلاصه فارسی"), ("todo", "What to do")):
        body = extra(fname, kind)
        if body:
            words |= words_in(body)
            panes.append((kind, label, body, ' dir="rtl"'))
    sub = "".join(f'<button type="button" role="tab" data-p="{k}">{l}</button>' for k, l, _, _ in panes)
    body = "".join(f'<div class="pane pane-{k}" data-p="{k}"{d} hidden>\n{b}\n</div>' for k, _, b, d in panes)
    parts.append(f'<section class="session" data-s="{sid}" hidden>\n{head}<nav class="subtabs" role="tablist">{sub}</nav>\n{body}\n</section>')
if TABS:
    parts.insert(0, '<nav class="sessiontabs" role="tablist">' + "".join(nav) + '</nav>')

audio = lambda w: os.path.join(AUDIO_DIR, w.replace("'", "_") + ".mp3")
missing_gloss = sorted(w for w in words if w not in gloss)
missing_audio = sorted(w for w in words if not os.path.exists(audio(w)))
if missing_gloss or missing_audio:
    print("missing glossary:", missing_gloss)
    print("missing audio:", missing_audio)
    sys.exit(1)

data = {w: [gloss[w], base64.b64encode(open(audio(w), "rb").read()).decode()] for w in sorted(words)}
theme = open(os.path.join(HERE, "theme.html"), encoding="utf8").read()
theme = theme.replace("/*__FONTS__*/", open(os.path.join(HERE, "fonts.css")).read())
theme = theme.replace("__APP_NAME__", html.escape(APP_NAME)).replace("__SUBTITLE__", f"{len(SESSIONS)} session" + ("s" if len(SESSIONS) > 1 else ""))
script = open(os.path.join(HERE, "reader-script.html"), encoding="utf8").read()
if TABS:
    script += "\n" + open(os.path.join(HERE, "tabs.html"), encoding="utf8").read()
script += "\n" + open(os.path.join(HERE, "updater.html"), encoding="utf8").read()

doc = ('<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>' + html.escape(APP_NAME) + '</title>\n'
       f'<meta name="dtad-version" content="{VERSION}">\n<meta name="dtad-update-base" content="{html.escape(UPDATE_BASE)}">\n'
       '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
       '<style>[hidden]{display:none!important}body{margin:0}</style>\n</head>\n<body>\n'
       + theme + "\n\n".join(parts) + '\n</article>\n</div>\n'
       '<div id="gloss" hidden role="status" aria-live="polite"></div>\n'
       '<script id="dict" type="application/json">' + json.dumps(data, ensure_ascii=False, separators=(",", ":")) + '</script>\n'
       + script + '\n</body>\n</html>\n')
open(sys.argv[1], "w", encoding="utf8").write(doc)
print(f"{len(words)} words, {len(doc.encode()) // 1024} KB -> {sys.argv[1]}")
