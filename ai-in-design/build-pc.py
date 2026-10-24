"""Make AI-in-Design-pc.html: content.html plus a small check that, when online, offers the newer PC file.

Usage: python3 -I build-pc.py   (run after content.html and version.json change, and commit the result)
The Android app updates itself; this is for opening the page in a browser on a PC.
"""
import json, os

HERE = os.path.dirname(os.path.abspath(__file__))
BASE = json.load(open(os.path.join(HERE, "app", "assets", "config.json")))["updateBase"]
version = json.load(open(os.path.join(HERE, "version.json"), encoding="utf8"))["version"]
NAME = "AI-in-Design-pc.html"

snippet = """<div id="pc-update" hidden style="position:fixed;top:0;left:0;right:0;z-index:50;background:#1f4fa3;color:#fff;padding:.7rem 1rem;font:16px/1.5 sans-serif;text-align:center;direction:rtl">
<span id="pc-msg"></span> <button id="pc-get" style="margin-inline-start:.6rem;font:inherit;padding:.2rem .8rem;border:0;border-radius:4px;cursor:pointer">دانلود نسخه‌ی جدید</button></div>
<script>
(function(){
  const LOCAL=__VERSION__, BASE=__BASE__, NAME=__NAME__;
  const bar=document.getElementById('pc-update'), btn=document.getElementById('pc-get');
  fetch(BASE+'version.json?t='+Date.now(),{cache:'no-store'}).then(r=>r.json()).then(v=>{
    if(!(v.version>LOCAL)) return;
    document.getElementById('pc-msg').textContent=(v.message||'محتوای جدید آمده است')+'.';
    bar.hidden=false;
    btn.onclick=()=>{
      btn.disabled=true; btn.textContent='در حال دانلود…';
      fetch(BASE+NAME+'?t='+Date.now(),{cache:'no-store'}).then(r=>r.blob()).then(b=>{
        const a=document.createElement('a'); a.href=URL.createObjectURL(new Blob([b],{type:'text/html'})); a.download=NAME;
        document.body.appendChild(a); a.click(); a.remove();
        btn.textContent='دانلود شد؛ فایل جدید را باز کن';
      }).catch(()=>{btn.disabled=false; btn.textContent='دوباره امتحان کن';});
    };
  }).catch(()=>{});
})();
</script>
"""
snippet = snippet.replace("__VERSION__", str(int(version))).replace("__BASE__", json.dumps(BASE)).replace("__NAME__", json.dumps(NAME))
page = open(os.path.join(HERE, "content.html"), encoding="utf8").read()
assert page.count("</body>") == 1
open(os.path.join(HERE, NAME), "w", encoding="utf8").write(page.replace("</body>", snippet + "</body>"))
print(f"{NAME}: version {version}")
