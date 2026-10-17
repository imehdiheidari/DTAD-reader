# DTAD Project reader

Content for the **DTAD Project** Android app (course *Pandaemonium: Daemons for Weimar*): session transcripts merged with the studio brief, with Persian word meanings and pronunciation. Built with `../toolkit`.

- `content.html` is the page the app shows (works offline). `version.json` tells installed apps when new content is available: raise `version` after updating `content.html`, and the app downloads it on its next launch with internet.
- `build-data/sessions/NN.md` is each session's source (toolkit transcript format), `NN.html` its converted fragment, `index.json` the session list.
- `build-data/glossary-fa.txt` is the full glossary for this course (DTAD Theory's words plus this course's). `build-data/audio.tar` holds only the pronunciations this course added; extract it together with `toolkit/build-data/audio.tar` into one folder.
- `app/` is the overlay on `toolkit/app` (name, icon, update URL); `build-app.sh` builds the APK under the package `ir.mehdi.dtadproject`, so it installs next to DTAD Theory. Same `KEYSTORE` / `KS_PASS` / SDK needs as `toolkit/app/build.sh`.

## Adding a session

```sh
cd toolkit/build-data
python3 -I conv.py ../../project/build-data/sessions/02.md ../../project/build-data/sessions/02.html
# add ["02.html", "Session 2 · ..."] to project/build-data/sessions/index.json
mkdir -p /tmp/audio && tar xf audio.tar -C /tmp && tar xf ../../project/build-data/audio.tar -C /tmp
APP_NAME="DTAD Project" DATA_DIR=../../project/build-data AUDIO_DIR=/tmp/audio python3 -I build_content.py ../../project/content.html
# it lists words missing from the glossary or audio: add glossary lines, run tts.py for the audio, re-run,
# add the new mp3s to project/build-data/audio.tar, then raise "version" in project/version.json
```
