#!/usr/bin/env python3
"""Recursos totalmente locais, produzidos antes da publicação do Pages e do build Android."""
from pathlib import Path
import json, math, subprocess, tempfile, shutil, random, sys
from PIL import Image,ImageDraw,ImageFont,ImageFilter

BASE=Path(__file__).resolve().parents[1]
WEB=BASE/"web"
AND=BASE/"android-java/app/src/main"
ROOT=AND/"assets"
DATA=json.loads((WEB/"content.json").read_text(encoding="utf-8"))
IMG=(WEB/"assets/misterios"); AUDIO=(WEB/"assets/audio")
IMG.mkdir(parents=True,exist_ok=True);AUDIO.mkdir(parents=True,exist_ok=True)
FONT="/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD="/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
def font(size,bold=False):
 try:return ImageFont.truetype(BOLD if bold else FONT,size)
 except:return ImageFont.load_default()

def picture(key,index,m):
    # Ilustração devocional estilizada desenhada localmente; nenhuma imagem remota.
    colors={
      "gozosos":((33,58,78),(216,172,110),(236,217,181)),
      "luminosos":((22,74,102),(112,167,186),(244,219,160)),
      "dolorosos":((64,37,46),(142,85,82),(241,174,115)),
      "gloriosos":((34,54,101),(148,152,184),(252,217,157))}
    dark,mid,gold=colors[key]
    w,h=720,450
    im=Image.new("RGB",(w,h));px=im.load()
    for y in range(h):
      t=y/(h-1)
      for x in range(w):
        # sky: warm radiance around the event center
        d=((x-(355+index*10))**2+(y-80)**2)**0.5
        glow=max(0,1-d/420)*0.5
        c=[]
        for j in range(3):
          v=(1-t)*dark[j]+t*mid[j]
          c.append(int(max(0,min(255,v*(1-glow)+gold[j]*glow))))
        px[x,y]=tuple(c)
    draw=ImageDraw.Draw(im,"RGBA")
    rnd=random.Random(index+17*list(colors).index(key))
    # distant mountain silhouettes
    for layer in range(4):
      yy=195+layer*70
      pts=[(0,h)]
      for x in range(-100,w+110,85):
        pts.append((x,yy+rnd.randint(-50,45)))
      pts.append((w,h))
      draw.polygon(pts,fill=(10+layer*10,30+layer*8,45+layer*5,110+layer*28))
    # luminous gold sky and rays
    sun=(w//2+(-1 if index%2 else 1)*60,138)
    for r in range(180,26,-12):
      draw.ellipse((sun[0]-r,sun[1]-r,sun[0]+r,sun[1]+r),outline=(255,220,145,20),width=3)
    for j in range(24):
      a=j*math.pi/12
      x0=sun[0]+34*math.cos(a);y0=sun[1]+34*math.sin(a)
      x1=sun[0]+235*math.cos(a);y1=sun[1]+235*math.sin(a)
      draw.line((x0,y0,x1,y1),fill=(255,225,160,38),width=2)
    # reverent symbolic figures, robe and halo
    for n in range(2 if key in ("gozosos","luminosos") else 1):
      xx=260+n*176;head_y=210+(n*15)
      robe=(215,205,181,242) if n==0 else (61,126,160,242)
      draw.polygon([(xx-30,head_y+33),(xx+30,head_y+33),(xx+66,440),(xx-66,440)],fill=robe)
      draw.ellipse((xx-20,head_y-28,xx+20,head_y+18),fill=(203,164,123,255))
      draw.arc((xx-32,head_y-36,xx+32,head_y+27),0,360,fill=(255,222,158,230),width=3)
      draw.line((xx-25,head_y+48,xx-70,head_y+128),fill=(205,176,145,230),width=15)
    # sacred cross silhouette and foreground texture
    draw.line((w-76,270,w-76,395),fill=(255,207,138,190),width=9)
    draw.line((w-103,301,w-49,301),fill=(255,207,138,190),width=8)
    draw.rectangle((0,364,w,h),fill=(8,22,32,160))
    title=m["title"]
    words=title.split();lines=[];cur=""
    for word in words:
      trial=(cur+" "+word).strip()
      if draw.textlength(trial,font=font(29,True))>w-70 and cur:
        lines.append(cur);cur=word
      else:cur=trial
    if cur:lines.append(cur)
    for j,line in enumerate(lines[-2:]):
      draw.text((34,373+j*35),line,font=font(29,True),fill=(252,238,203,255),stroke_width=1,stroke_fill=(15,31,40,255))
    # Preservar pinturas WebP personalizadas ja incorporadas no repositorio.
    destino=IMG/(m["id"]+".webp")
    if not destino.exists():
        im.save(destino,format="WEBP",quality=84,method=6)

for key,items in DATA["mysteries"].items():
 for i,m in enumerate(items):picture(key,i,m)

def narrator(name,txt):
    dest=AUDIO/(name+".ogg")
    with tempfile.NamedTemporaryFile(suffix=".wav",delete=True) as t:
      cmd=["espeak-ng","-v","pt-br","-s","128","-p","45","-w",t.name,txt]
      subprocess.run(cmd,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
      subprocess.run(["ffmpeg","-y","-hide_banner","-loglevel","error","-i",t.name,"-c:a","libopus","-b:a","13k","-ar","24000",str(dest)],check=True)
      if dest.stat().st_size<200:raise RuntimeError("Áudio vazio: "+name)

for key,txt in DATA["prayers"].items():narrator("oracao-"+key,txt)
for key,items in DATA["mysteries"].items():
 for m in items:narrator(m["id"],m["title"]+". "+m["scene"]+" "+m["reflection"]+" "+m["question"])

def icon(size):
    im=Image.new("RGBA",(size,size),(20,49,58,255));d=ImageDraw.Draw(im)
    c=size//2;r=int(size*.37)
    d.ellipse((c-r,c-r,c+r,c+r),outline=(236,198,138,255),width=max(3,size//31))
    for i in range(15):
      theta=2*math.pi*i/15
      x=c+int(r*math.cos(theta));y=c+int(r*math.sin(theta))
      d.ellipse((x-size//47,y-size//47,x+size//47,y+size//47),fill=(239,207,155,255))
    d.line((c,int(size*.26),c,int(size*.76)),fill=(255,220,172,255),width=size//20)
    d.line((int(size*.35),int(size*.43),int(size*.65),int(size*.43)),fill=(255,220,172,255),width=size//20)
    return im
icon(192).save(WEB/"icon-192.png");icon(512).save(WEB/"icon-512.png")
(AND/"res/drawable").mkdir(parents=True,exist_ok=True)
icon(192).save(AND/"res/drawable/app_icon.png")
(ROOT/"misterios").mkdir(parents=True,exist_ok=True);(ROOT/"audio").mkdir(parents=True,exist_ok=True)
shutil.copy2(WEB/"content.json",ROOT/"content.json")
for src in IMG.glob("*.webp"):shutil.copy2(src,ROOT/"misterios"/src.name)
for src in AUDIO.glob("*.ogg"):shutil.copy2(src,ROOT/"audio"/src.name)
print("Concluído:",len(list(IMG.glob("*.webp"))),"imagens WebP, ",len(list(AUDIO.glob("*.ogg"))),"áudios, ícones e dados Android")
