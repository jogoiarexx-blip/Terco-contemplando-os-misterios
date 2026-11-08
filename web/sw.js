const VERSION="terco-contemple-v0.3.2";
const sets=["gozosos","luminosos","dolorosos","gloriosos"];
const images=sets.flatMap(s=>[1,2,3,4,5].map(n=>"./assets/misterios/"+s+"-"+n+".webp"));
const audio=[...sets.flatMap(s=>[1,2,3,4,5].map(n=>"./assets/audio/"+s+"-"+n+".ogg")),...["cruz","credo","pai","ave","gloria","fatima","salve","final"].map(n=>"./assets/audio/oracao-"+n+".ogg")];
const resources=["./","./index.html","./style.css","./app.js","./content.json","./icon.svg","./icon-192.png","./icon-512.png","./manifest.webmanifest",...images,...audio];
self.addEventListener("install",e=>e.waitUntil(caches.open(VERSION).then(cache=>cache.addAll(resources)).then(()=>self.skipWaiting())));
self.addEventListener("activate",e=>e.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith("terco-contemple-")&&k!==VERSION).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener("fetch",e=>{if(e.request.method!=="GET"||new URL(e.request.url).origin!==self.location.origin)return;e.respondWith(caches.match(e.request).then(r=>r||fetch(e.request).then(res=>{if(res.ok){const copy=res.clone();caches.open(VERSION).then(c=>c.put(e.request,copy))}return res}).catch(()=>e.request.mode==="navigate"?caches.match("./index.html"):Response.error())))});
self.addEventListener("message",e=>{if(e.data?.type==="STATUS")e.waitUntil(caches.open(VERSION).then(async cache=>{const checks=await Promise.all(resources.map(a=>cache.match(a)));e.ports?.[0]?.postMessage({complete:checks.every(Boolean),count:checks.filter(Boolean).length,total:resources.length})}))});