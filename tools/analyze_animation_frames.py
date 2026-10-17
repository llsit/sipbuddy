from pathlib import Path
import re,json,sys
root=Path(sys.argv[1]) if len(sys.argv)>1 else Path("app/build/reports/frame-benchmark")
results=[]
for folder in root.glob('frames-*'):
 for file in folder.glob('*.txt'):
  text=file.read_text(encoding='utf-8')
  if 'Total frames rendered:' not in text: continue
  def value(pattern):
   m=re.search(pattern,text);return float(m.group(1)) if m else None
  rows=[];columns=None
  for line in text.splitlines():
   if line.startswith('Flags,'):columns=line.strip(',').split(',')
   elif columns and re.match(r'^\d+,',line):
    parts=line.strip(',').split(',')
    if len(parts)>=len(columns):
     row=dict(zip(columns,map(int,parts)))
     if row['Flags']==0 and row['IntendedVsync']>0:rows.append(row)
  times=sorted(set(r['IntendedVsync'] for r in rows))
  fps=(len(times)-1)*1e9/(times[-1]-times[0]) if len(times)>1 else None
  result={'variant':folder.name[7:],'scenario':file.stem,'frames':value(r'Total frames rendered: (\d+)'),
   'jank':value(r'Janky frames: (\d+)'),'jankPercent':value(r'Janky frames: \d+ \(([\d.]+)%\)'),
   'p50ms':value(r'50th percentile: (\d+)ms'),'p95ms':value(r'95th percentile: (\d+)ms'),
   'p99ms':value(r'99th percentile: (\d+)ms'),'recentFrameCadenceFps':fps if file.stem=='mascot' else None,'recentSampleFrames':len(times),
   'slowUiFrames':value(r'Number Slow UI thread: (\d+)'),'slowDrawFrames':value(r'Number Slow issue draw commands: (\d+)')}
  if result['frames']==0:
   result['p50ms']=result['p95ms']=result['p99ms']=None
  present_file=file.with_name(file.stem+'-present.txt')
  if present_file.exists():
   actual=[]
   for line in present_file.read_text(encoding='utf-8').splitlines()[1:]:
    parts=line.split()
    if len(parts)==3 and 0<int(parts[1])<9223372036854775807:actual.append(int(parts[1]))
   actual=sorted(set(actual))
   if len(actual)>1:result['surfacePresentationFps']=(len(actual)-1)*1e9/(actual[-1]-actual[0])
  results.append(result)
(root/'frame-results.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
for r in results:print(r)
