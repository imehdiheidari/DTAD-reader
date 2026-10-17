"""Make audio/<word>.mp3 for every word that has none yet.

Usage: python3 -I tts.py VOICE.onnx AUDIO_DIR word1 word2 ...   (or words on stdin, one per line)
Voice: en_US-lessac-medium from https://huggingface.co/rhasspy/piper-voices (pip install piper-tts).
"""
import os, subprocess, sys, tempfile, wave
from piper import PiperVoice

voice, out_dir, words = sys.argv[1], sys.argv[2], sys.argv[3:] or [l.strip() for l in sys.stdin if l.strip()]
v = PiperVoice.load(voice)
os.makedirs(out_dir, exist_ok=True)
tmp = os.path.join(tempfile.mkdtemp(), "t.wav")
trim = "silenceremove=start_periods=1:start_threshold=-45dB,areverse,silenceremove=start_periods=1:start_threshold=-45dB,areverse"
made = 0
for w in words:
    out = os.path.join(out_dir, w.replace("'", "_") + ".mp3")
    if os.path.exists(out):
        continue
    with wave.open(tmp, "wb") as f:
        v.synthesize_wav(w, f)
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", tmp, "-af", trim, "-ac", "1", "-ar", "16000",
                    "-c:a", "libmp3lame", "-b:a", "24k", out], check=True)
    made += 1
print(f"made {made} new mp3 files in {out_dir}")
