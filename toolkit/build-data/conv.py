import re,html,sys
# usage: python3 -I conv.py transcript.md sessions/NN.html
src=open(sys.argv[1],encoding='utf8').read().split('\n')
def inl(s):
    s=html.escape(s,quote=False)
    s=re.sub(r'\*\*\[(\d+:\d\d:\d\d)\]\*\*',r'<span class="ts">\1</span>',s)
    s=re.sub(r'\*\*(.+?)\*\*',r'<strong>\1</strong>',s)
    s=re.sub(r'(?<!\w)\*(.+?)\*(?!\w)',r'<em>\1</em>',s)
    return s
out=[];i=0
while i<len(src):
    l=src[i]
    if l.startswith('# ') or l.startswith('Source:'): i+=1; continue
    if l.startswith('>'):
        blk=[]
        while i<len(src) and src[i].startswith('>'):
            blk.append(src[i][1:].strip()); i+=1
        parts=[];ul=False
        for b in blk:
            if b.startswith('📊'): continue
            if b.startswith('- '):
                if not ul: parts.append('<ul>'); ul=True
                parts.append('<li>'+inl(b[2:])+'</li>'); continue
            if ul: parts.append('</ul>'); ul=False
            if b: parts.append('<p>'+inl(b)+'</p>')
        if ul: parts.append('</ul>')
        out.append('<aside class="slide"><div class="slide-tag">Slide</div>'+''.join(parts)+'</aside>')
        continue
    if l.strip():
        out.append('<p>'+inl(l)+'</p>')
    i+=1
open(sys.argv[2],'w',encoding='utf8').write('\n'.join(out))
print(len(out))
