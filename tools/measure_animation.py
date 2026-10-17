import subprocess,time,re,sys,xml.etree.ElementTree as E,json,os,shutil
from pathlib import Path
ADB = os.environ.get('ADB') or shutil.which('adb')
if not ADB:
 raise SystemExit('Set ADB to adb.exe or add Android SDK platform-tools to PATH')
PKG='com.llsit.sipduddy.benchmark'
if len(sys.argv)<2: raise SystemExit('Usage: python tools/measure_animation.py LABEL')
label=sys.argv[1]
out=Path(os.environ.get('SIPBUDDY_BENCH_OUTPUT','app/build/reports/frame-benchmark'))/('frames-'+label);out.mkdir(exist_ok=True)
def adb(*args):
 return subprocess.check_output([ADB,*args],text=True,encoding='utf-8',errors='replace',timeout=30)
def dump():
 adb('shell','uiautomator','dump','/sdcard/sipbuddy-benchmark.xml')
 text=adb('shell','cat','/sdcard/sipbuddy-benchmark.xml')
 root=E.fromstring(text)
 assert any(n.get('package')==PKG for n in root.iter('node')), 'Benchmark app is not in foreground; no taps sent'
 return root
def bounds(n):
 v=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
 return v if len(v)==4 and v[2]>v[0] and v[3]>v[1] else None
def find(root,text):
 parents={child:parent for parent in root.iter() for child in parent}
 for node in root.iter('node'):
  if text not in (node.get('text','')+' '+node.get('content-desc','')):continue
  current=node
  while current is not None:
   if (current.get('clickable')=='true' or current.get('selected')=='true') and current.get('enabled')=='true' and bounds(current):
    b=bounds(current);return ((b[0]+b[2])//2,(b[1]+b[3])//2)
   current=parents.get(current)
def tap(pos):
 focus=adb('shell','dumpsys','window','windows')
 assert any(PKG in line for line in focus.splitlines() if 'mCurrentFocus' in line), 'Benchmark is not focused; no tap sent'
 adb('shell','input','tap',str(pos[0]),str(pos[1]));time.sleep(.65)
def scroll(root=None):
 root=root if root is not None else dump()
 for n in root.iter('node'):
  if n.get('scrollable')=='true' and bounds(n):
   b=bounds(n);x=(b[0]+b[2])//2
   adb('shell','input','swipe',str(x),str(b[1]+(b[3]-b[1])*3//4),str(x),str(b[1]+(b[3]-b[1])//4),'300');time.sleep(.5);return
def click(text):
 for _ in range(3):
  root=dump();pos=find(root,text)
  if pos:tap(pos);print('Tapped',text,flush=True);return
  scroll(root)
 raise RuntimeError('Unable to find '+text)
def measure(name,action):
 adb('shell','dumpsys','gfxinfo',PKG,'reset');start=time.monotonic();action();time.sleep(.5)
 elapsed=time.monotonic()-start
 raw=adb('shell','dumpsys','gfxinfo',PKG,'framestats')
 (out/(name+'.txt')).write_text(raw,encoding='utf-8')
 if name == 'mascot':
  layer=PKG+'/com.llsit.sipduddy.MainActivity#0'
  present=adb('shell','dumpsys','SurfaceFlinger','--latency',layer)
  (out/(name+'-present.txt')).write_text(present,encoding='utf-8')
 (out/(name+'.json')).write_text(json.dumps({'elapsed_s':elapsed,'label':label,'scenario':name}),encoding='utf-8')
 print(label,name,'captured',round(elapsed,2),flush=True)
adb('shell','am','start','-W','-n',PKG+'/com.llsit.sipduddy.MainActivity');time.sleep(3)
root=dump();nav={s:find(root,s) for s in ['วันนี้','ประวัติ','ตั้งค่า']};assert all(nav.values()),nav
for s in ['ประวัติ','ตั้งค่า','วันนี้']:tap(nav[s])
measure('mascot',lambda:time.sleep(8))
measure('navigation',lambda:[tap(nav[s]) for _ in range(8) for s in ['ประวัติ','ตั้งค่า','วันนี้']])
def diary():
 for _ in range(3):
  click('บันทึกเครื่องดื่ม');click('เติมให้บัดดี้');click('แก้ไข');click('500 มล.');click('บันทึกการแก้ไข');click('ลบ');click('ลบรายการ')
measure('diary',diary)
print('DONE',label,flush=True)
