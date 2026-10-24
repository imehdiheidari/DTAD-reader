# AI in Design reader

Content for the **AI in Design** Android app: session transcripts with Persian word meanings and pronunciation, a Persian summary and a "What to do" tab per session. Built with `../toolkit`, same scheme as `../project` (DTAD Project).

- `content.html` is the page the app shows (works offline). `version.json` tells installed apps when new content is available: raise `version` after updating `content.html`.
- `build-data/sessions/NN.md` is each session's source (toolkit transcript format), `NN.html` its converted fragment, `NN-fa.html` the Persian summary, `NN-todo.html` the tasks for the next session, `index.json` the session list.
- `build-data/glossary-fa.txt` starts as a copy of the DTAD Project glossary; this course's new words are added to it. `build-data/audio.tar` (created with the first session) holds only the pronunciations this course adds; extract it together with `toolkit/build-data/audio.tar` and `project/build-data/audio.tar` into one folder.
- `AI-in-Design-pc.html` is the page for a PC browser; `build-pc.py` makes it from `content.html`.
- `app/` is the overlay on `toolkit/app` (name, icon, update URL); `build-app.sh` builds the APK under the package `ir.mehdi.aiindesign`, so it installs next to the DTAD apps. Same `KEYSTORE` / `KS_PASS` / SDK needs as `toolkit/app/build.sh`.

## Source recordings

The class recordings are in the Google Drive folder **ai in design**, named `ai in design N` for session N (`ai in design N-1`, `N-2` when a session has several parts).

## Adding a session

1. Download the recording(s) from Drive, transcribe them with timestamps, and write `build-data/sessions/NN.md` (a summary blockquote, then one `> **Part k** (recording N-k)` block per recording).
2. Write `NN-fa.html` (Persian summary) and `NN-todo.html` (what to do for the next session).
3. Build:

```sh
cd toolkit/build-data
python3 -I conv.py ../../ai-in-design/build-data/sessions/NN.md ../../ai-in-design/build-data/sessions/NN.html
# add ["NN.html", "Session N · ..."] to ai-in-design/build-data/sessions/index.json
mkdir -p /tmp/audio && tar xf audio.tar -C /tmp && tar xf ../../project/build-data/audio.tar -C /tmp
[ -f ../../ai-in-design/build-data/audio.tar ] && tar xf ../../ai-in-design/build-data/audio.tar -C /tmp
APP_NAME="AI in Design" COURSE_LINE="Bauhaus-Universität Weimar · WS 2026/27" DATA_DIR=../../ai-in-design/build-data AUDIO_DIR=/tmp/audio \
  VERSION=N UPDATE_BASE=https://raw.githubusercontent.com/imehdiheidari/DTAD-reader/main/ai-in-design/ \
  python3 -I build_content.py ../../ai-in-design/content.html
# it lists words missing from the glossary or audio: add glossary lines, run tts.py for the audio, re-run,
# add the new mp3s to ai-in-design/build-data/audio.tar, then python3 -I ../../ai-in-design/build-pc.py
```

4. Publish in the order given in `toolkit/README.md`: push `content.html`, wait for the CDN, then push `version.json` with the new number.
