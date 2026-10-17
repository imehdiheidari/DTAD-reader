# DTAD reader toolkit

Everything needed to rebuild a DTAD reader app (lecture transcript + Persian word meanings + offline pronunciation, Android WebView APK with auto-update from a GitHub repo).

- `build-data/conv.py transcript.md sessions/NN.html` converts a transcript (format below) to a session fragment.
- `build-data/tts.py VOICE.onnx AUDIO_DIR words...` makes per-word MP3 pronunciations (piper-tts, en_US-lessac-medium).
- `build-data/build_content.py OUT.html` (env `APP_NAME`, `AUDIO_DIR`, and `DATA_DIR` for a course that keeps its own `sessions/` and glossary, like `project/build-data`) builds the self-contained page from `sessions/index.json`, `glossary-fa.txt` (word|Persian meaning) and the audio. `audio.tar` holds MP3s for the 2,230 words of DTAD Theory lecture 1.
- `app/` is the Android app (plain Java WebView, no Gradle). `app/build.sh` needs `KEYSTORE`, `KS_PASS`, and the Android SDK (platform 34 + build-tools 34) at `/tmp/claude-0/sdk`.

Transcript format: `**[h:mm:ss]** text` paragraphs; slide text as `>` blockquote blocks (lines starting with `- ` become list items); a `# ` title line and a `Source:` line are skipped.

Words may contain accented Latin letters (daímōn, Fürstengruft); the page and the build script treat them as one word.

Other courses built with this toolkit: `project/` (DTAD Project).
