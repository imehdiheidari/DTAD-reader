# DTAD Reader

Content for the DTAD Reader Android app: lecture transcripts with Persian word meanings and pronunciation.

- `content.html` is the page the app shows (works offline).
- `version.json` tells the app when new content is available. Raise `version` after updating `content.html` and the app downloads it on its next launch with internet.

The DTAD Project app reads its content from `project/` (same scheme, its own `content.html` and `version.json`).

## Medication reminder

`medication-reminder/index.html` is a Bauhaus-style medication reminder (Farsi / English / German). It runs in any browser, but there reminders only work while the page is open.
`medication-reminder/app/` wraps the same page as an Android app (`ir.mehdi.doseclock`) that posts real notifications with Done / Later buttons even when the app is closed: `KEYSTORE=… KS_PASS=… medication-reminder/app/build.sh` (same SDK needs as `toolkit/app/build.sh`). `vendor/` holds the offline copies of React, htm, Tailwind and the fonts.
