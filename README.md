# DTAD Reader

Content for the DTAD Reader Android app: lecture transcripts with Persian word meanings and pronunciation.

- `content.html` is the page the app shows (works offline).
- `class-audio/NN/` holds the DTAD Theory class recordings as 5-minute mp3 chunks (served through jsDelivr, listed in `toolkit/build-data/sessions/class-audio.json`): tapping a timestamp plays that paragraph from the real class.
- `version.json` tells the app when new content is available. Raise `version` after updating `content.html` and the app downloads it on its next launch with internet.

The DTAD Project app reads its content from `project/` and the AI in Design app from `ai-in-design/` (same scheme, each with its own `content.html` and `version.json`).
