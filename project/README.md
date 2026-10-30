# DTAD Project reader

Content for the **DTAD Project** Android app (course *Pandaemonium: Daemons for Weimar*): session transcripts merged with the studio brief, with Persian word meanings and pronunciation. Built with `../toolkit`.

- `content.html` is the page the app shows (works offline). `version.json` tells installed apps when new content is available: raise `version` after updating `content.html`, and the app downloads it on its next launch with internet.
- `build-data/sessions/NN.md` is each session's source (toolkit transcript format), `NN.html` its converted fragment, `index.json` the session list.
- `build-data/sessions/NN-fa.html` (Persian summary) and `NN-todo.html` ("What to do": tasks for the next session) are optional; when any exist, the page shows one tab per session with sub-tabs for the transcript, summary and to-do (`toolkit/build-data/tabs.html`).
- `build-data/glossary-fa.txt` is the full glossary for this course (DTAD Theory's words plus this course's). `build-data/audio.tar` holds only the pronunciations this course added; extract it together with `toolkit/build-data/audio.tar` into one folder.
- Add-ons (built by `toolkit/build-data/build_content.py`): `class-audio/` holds each class recording as 5-minute mp3 chunks (mono 32 kbps, `ffmpeg -f segment -segment_time 300`), listed in `build-data/sessions/class-audio.json` (served through jsDelivr), so tapping a timestamp plays that paragraph; `build-data/phrases.txt` + `phrases-audio.tar` (piper, `phrases/NN.mp3`) make the "class phrases" tab; the "my words" tab keeps looked-up words on the device for flashcard review.
- `DTAD-Project-pc.html` is the same page for a PC browser: when online it checks `version.json` and offers to download the newer PC file. `build-pc.py` makes it from `content.html`.
- `app/` is the overlay on `toolkit/app` (name, icon, update URL); `build-app.sh` builds the APK under the package `ir.mehdi.dtadproject`, so it installs next to DTAD Theory. Same `KEYSTORE` / `KS_PASS` / SDK needs as `toolkit/app/build.sh`.

## Adding a session

```sh
cd toolkit/build-data
python3 -I conv.py ../../project/build-data/sessions/02.md ../../project/build-data/sessions/02.html
# add the recording chunks to class-audio/ and class-audio.json
# add ["02.html", "Session 2 · ..."] to project/build-data/sessions/index.json
mkdir -p /tmp/audio && tar xf audio.tar -C /tmp && tar xf ../../project/build-data/audio.tar -C /tmp
APP_NAME="DTAD Project" DATA_DIR=../../project/build-data AUDIO_DIR=/tmp/audio python3 -I build_content.py ../../project/content.html
# it lists words missing from the glossary or audio: add glossary lines, run tts.py for the audio, re-run,
# add the new mp3s to project/build-data/audio.tar, raise "version" in project/version.json,
# then python3 -I ../../project/build-pc.py
```
