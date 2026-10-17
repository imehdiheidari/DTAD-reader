"""Build content.html for a DTAD reader app from the session bodies, glossary and word audio.

Usage: APP_NAME="DTAD Theory" [DATA_DIR=...] python3 -I build_content.py OUT.html
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

def words_in(fragment):
    text = html.unescape(re.sub(r"<[^>]+>", " ", fragment))
    return {w.lower().replace("’", "'") for w in re.findall(r"[A-Za-zÀ-ÖØ-öø-ɏ][A-Za-zÀ-ÖØ-öø-ɏ'’-]*[A-Za-zÀ-ÖØ-öø-ɏ]|[A-Za-zÀ-ÖØ-öø-ɏ]", text)}

gloss = {}
for line in open(os.path.join(DATA, "glossary-fa.txt"), encoding="utf8"):
    line = line.rstrip("\n")
    if "|" in line:
        k, v = line.split("|", 1)
        gloss[k] = v

parts, words = [], set()
for fname, title in SESSIONS:
    frag = open(os.path.join(DATA, "sessions", fname), encoding="utf8").read()
    words |= words_in(frag)
    parts.append(f'<h2 class="session-title">{html.escape(title)}</h2>\n{frag}')

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

doc = ('<!doctype html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>' + html.escape(APP_NAME) + '</title>\n'
       '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">\n'
       '<style>[hidden]{display:none!important}body{margin:0}</style>\n</head>\n<body>\n'
       + theme + "\n\n".join(parts) + '\n</article>\n</div>\n'
       '<div id="gloss" hidden role="status" aria-live="polite"></div>\n'
       '<script id="dict" type="application/json">' + json.dumps(data, ensure_ascii=False, separators=(",", ":")) + '</script>\n'
       + script + '\n</body>\n</html>\n')
open(sys.argv[1], "w", encoding="utf8").write(doc)
print(f"{len(words)} words, {len(doc.encode()) // 1024} KB -> {sys.argv[1]}")
