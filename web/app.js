(()=>{"use strict";
const app=document.getElementById("app");
const buttons=document.querySelectorAll(".tabs button");
const DAYS=["gloriosos","gozosos","dolorosos","gloriosos","luminosos","dolorosos","gozosos"];
const SETS=["gozosos","luminosos","dolorosos","gloriosos"];
const LABELS={gozosos:"Gozosos",luminosos:"Luminosos",dolorosos:"Dolorosos",gloriosos:"Gloriosos"};
const KEY="terco-contemple-v03";
const DEFAULT={set:"auto",fatima:true,silence:30,intention:"",sessions:{},history:[]};
let data=null,page="inicio",audio=null,offline=false;
let saved={...DEFAULT};
try{saved={...DEFAULT,...JSON.parse(localStorage.getItem(KEY)||"{}")}}catch{}
if(!saved.sessions||typeof saved.sessions!=="object")saved.sessions={};
if(!Array.isArray(saved.history))saved.history=[];
function persist(){try{localStorage.setItem(KEY,JSON.stringify(saved))}catch{}}
function dateKey(date=new Date()){return [date.getFullYear(),String(date.getMonth()+1).padStart(2,"0"),String(date.getDate()).padStart(2,"0")].join("-")}
function selected(){return saved.set==="auto"?DAYS[new Date().getDay()]:saved.set}
function sessionKey(){return dateKey()+"|"+selected()}
function current(){const key=sessionKey();if(!saved.sessions[key])saved.sessions[key]={index:0,done:false};return saved.sessions[key]}
function escape(s){return String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))}
function toast(s){const node=document.getElementById("toast");node.textContent=s;node.className="show";setTimeout(()=>node.className="",2500)}
function stopAudio(){if(audio){audio.pause();audio.src="";audio=null}}
function go(name){stopAudio();page=name;render();scrollTo({top:0,behavior:"instant"})}
function steps(){const list=[{kind:"cruz"},{kind:"credo"},{kind:"pai"},...Array.from({length:3},()=>({kind:"ave"})),{kind:"gloria"}];
 for(let i=0;i<5;i++){list.push({kind:"meditation",decade:i},{kind:"pai",decade:i});for(let j=1;j<=10;j++)list.push({kind:"ave",decade:i,bead:j});list.push({kind:"gloria",decade:i});if(saved.fatima)list.push({kind:"fatima",decade:i})}
 list.push({kind:"salve"},{kind:"final"});return list}
function btn(label,action,val="",extra=""){return '<button class="btn '+extra+'" data-action="'+action+'" data-value="'+escape(val)+'">'+escape(label)+'</button>'}
function image(id){return '<img class="cover" src="./assets/misterios/'+id+'.webp" alt="Ilustração devocional: '+escape(id)+'" loading="lazy">'}
function progress(){const s=current(),n=steps().length;return s.done?100:Math.round(100*Math.min(n,s.index)/n)}
function season(date){const y=date.getFullYear();let a=y%19,b=Math.floor(y/100),c=y%100,d=Math.floor(b/4),e=b%4,f=Math.floor((b+8)/25),g=Math.floor((b-f+1)/3),h=(19*a+b-d-g+15)%30,i=Math.floor(c/4),k=c%4,l=(32+2*e+2*i-h-k+7)%7,m=Math.floor((a+11*h+22*l)/451),month=Math.floor((h+l-7*m+114)/31),day=(h+l-7*m+114)%31+1;
 const easter=new Date(y,month-1,day,12);const plus=n=>new Date(y,month-1,day+n,12);const christmas=new Date(y,11,25,12),advent=new Date(y,11,25-((christmas.getDay()||7)+21),12);
 const epiphany=new Date(y,0,2,12);while(epiphany.getDay()!==0)epiphany.setDate(epiphany.getDate()+1);const baptism=new Date(epiphany);baptism.setDate(baptism.getDate()+7);
 let label="Tempo Comum";if(date<=baptism||date>=christmas)label="Tempo do Natal";if(date>=plus(-46)&&date<plus(-3))label="Quaresma";if(date>=plus(-3)&&date<easter)label="Tríduo Pascal";if(date>=easter&&date<=plus(49))label="Tempo Pascal";if(date>=advent&&date<christmas)label="Advento";
 let holy="";if(dateKey(date)===dateKey(easter))holy="Páscoa do Senhor";else if(dateKey(date)===dateKey(plus(-46)))holy="Quarta-feira de Cinzas";else if(dateKey(date)===dateKey(plus(49)))holy="Pentecostes";else if(dateKey(date)===dateKey(advent))holy="Primeiro Domingo do Advento";else if(dateKey(date)===dateKey(christmas))holy="Natal";else if(dateKey(date)===y+"-10-12")holy="Nossa Senhora Aparecida";
 return {label,holy,easter,advent,christmas,pentecost:plus(49),ash:plus(-46)}}
function coverHeader(sub,title,lead){return '<p class="eyebrow">'+sub+'</p><h1 class="title">'+title+'</h1><p class="lead">'+lead+'</p>'}
function renderHome(){const set=selected(),list=data.mysteries[set],lit=season(new Date()),first=list[0],s=current();
 return coverHeader("ORAÇÃO E CONTEMPLAÇÃO","Um encontro com a fé.","Acompanhe o Santo Terço, contemple a vida de Jesus e Maria e reze no seu ritmo.")
 +image(first.id)
 +'<section class="card"><p class="eyebrow">HOJE • '+escape(new Intl.DateTimeFormat("pt-BR",{weekday:"long",day:"numeric",month:"long"}).format(new Date()))+'</p><h2>Mistérios '+LABELS[set]+'</h2><p>'+escape(first.title)+'</p><p class="meta">'+escape(lit.holy||lit.label)+' • '+(offline?"Disponível offline ✓":"Preparando o modo offline")+'</p><div class="progress"><div style="width:'+progress()+'%"></div></div><p class="mini">Progresso: '+progress()+'%</p>'+btn(s.done?"Rezar novamente":s.index?"Continuar o meu terço":"Começar a rezar","start","","primary full")+btn("Contemplar os 5 mistérios","goto","misterios","full")+'</section>'
 +'<section class="card"><h3>Minha intenção</h3><p>Por quem ou por qual situação você deseja rezar?</p><textarea id="intention" maxlength="240" placeholder="Ex.: Pela paz em minha família…">'+escape(saved.intention)+'</textarea><p class="mini">Guardada somente neste aparelho.</p></section>'
 +'<p class="mini">Sem conta, sem anúncios e com orações, imagens e áudio guardados no aparelho após a instalação completa.</p>'}
function renderMysteries(){const set=selected();let out=coverHeader("CONHEÇA E CONTEMPLE","Os 20 Mistérios","Escolha um conjunto para conhecer cada acontecimento, a passagem bíblica e a proposta de meditação.");
 out+='<div class="chips">'+SETS.map(s=>btn(LABELS[s],"choose",s,"chip "+(s===set?"selected":""))).join("")+'</div>';
 data.mysteries[set].forEach((m,i)=>{out+='<article class="mystery">'+image(m.id)+'<p class="eyebrow">'+(i+1)+'º Mistério</p><h3>'+escape(m.title)+'</h3><div class="ref">'+escape(m.reference)+'</div><p>'+escape(m.scene)+'</p><p>'+escape(m.reflection)+'</p><p><b>Fruto espiritual:</b> '+escape(m.fruit)+'</p><p><i>'+escape(m.question)+'</i></p>'+btn("Ouvir a contemplação","listen",m.id)+'</article>'});return out}
function beads(step,list,index){if(step.decade==null)return "";let count=step.kind==="ave"?step.bead-1:["gloria","fatima"].includes(step.kind)?10:0;let html='<div class="beads" role="img" aria-label="'+count+' de 10 Ave-Marias concluídas na dezena">';
 for(let i=0;i<10;i++)html+='<span class="bead '+(i<count?"done":i===count?"current":"")+'"></span>';return html+"</div>"}
function renderPray(){const list=steps(),session=current();if(session.done)return coverHeader("ORAÇÃO CONCLUÍDA","Amém!","Você contemplou os cinco mistérios do Terço. Que esta oração produza frutos de fé, esperança e caridade.")+'<section class="card"><h3>Obrigado pelo seu momento de oração.</h3>'+btn("Começar outro terço","restart","","primary full")+btn("Voltar ao início","goto","inicio","full")+'</section>';
 const n=Math.min(session.index,list.length-1),step=list[n],m=step.decade==null?null:data.mysteries[selected()][step.decade],med=step.kind==="meditation",kind=step.kind,prayer=med?null:data.prayers[kind],title=med?m.title:({cruz:"Sinal da Cruz",credo:"Creio",pai:"Pai-Nosso",ave:"Ave-Maria",gloria:"Glória ao Pai",fatima:"Jaculatória de Fátima",salve:"Salve-Rainha",final:"Oração final"}[kind]||kind);
 let out='<p class="eyebrow">REZANDO O SANTO TERÇO</p><h1 class="title">'+escape(title)+'</h1><p class="meta">Oração '+(n+1)+' de '+list.length+(m?' • '+(step.decade+1)+'º mistério':'')+'</p><div class="progress"><div style="width:'+progress()+'%"></div></div>';
 if(med)out+=image(m.id)+'<section class="card"><p class="eyebrow">'+escape(m.reference)+'</p><p>'+escape(m.scene)+'</p><p>'+escape(m.reflection)+'</p><p><b>Fruto:</b> '+escape(m.fruit)+'</p><p><i>'+escape(m.question)+'</i></p>'+btn("Ficar em silêncio por "+saved.silence+"s","silence")+'</section>';
 else out+='<section class="card"><p style="font-size:18px;line-height:1.8;white-space:pre-wrap">'+escape(prayer)+'</p></section>';
 out+=beads(step,list,n)+'<div class="btnrow">'+btn("← Voltar","back")+btn("Ouvir áudio","listen",med?m.id:"oracao-"+kind)+'</div>'+btn("Concluir e avançar →","next","","primary full");
 return out}
function renderCalendar(){const now=new Date(),lit=season(now),y=now.getFullYear(),fmt=d=>new Intl.DateTimeFormat("pt-BR",{day:"2-digit",month:"long"}).format(d);
 return coverHeader("LITURGIA NO BRASIL","Caminhe com a Igreja.","Uma visão simplificada dos principais tempos do ano litúrgico, calculados mesmo sem internet.")
 +'<section class="card"><p class="eyebrow">'+dateKey(now)+'</p><h2>'+escape(lit.holy||lit.label)+'</h2><p>Tempo litúrgico: '+lit.label+'</p></section>'
 +'<section class="card"><h3>Datas principais de '+y+'</h3><p>Quarta-feira de Cinzas: '+fmt(lit.ash)+'</p><p>Páscoa: '+fmt(lit.easter)+'</p><p>Pentecostes: '+fmt(lit.pentecost)+'</p><p>Advento: '+fmt(lit.advent)+'</p><p>Natal: 25 de dezembro</p><p>Nossa Senhora Aparecida: 12 de outubro</p></section><p class="mini">Referência devocional, não substitui o calendário litúrgico oficial de cada diocese.</p>'}
function renderSettings(){return coverHeader("PREFERÊNCIAS","Sua oração, seu ritmo.","Personalize o terço sem distrações.")
 +'<section class="card"><h3>Mistérios</h3><select id="select-set"><option value="auto">Automáticos pelo dia</option>'+SETS.map(s=>'<option '+(saved.set===s?"selected":"")+' value="'+s+'">'+LABELS[s]+'</option>').join("")+'</select><label><input id="fatima" type="checkbox" '+(saved.fatima?"checked":"")+'> Incluir a jaculatória de Fátima</label><p>Silêncio contemplativo:</p><select id="silence"><option value="30">30 segundos</option><option value="60" '+(saved.silence===60?"selected":"")+'>60 segundos</option><option value="90" '+(saved.silence===90?"selected":"")+'>90 segundos</option></select></section>'
 +'<section class="card"><h3>Histórico</h3><p>'+saved.history.length+' terço(s) concluído(s) neste dispositivo.</p>'+saved.history.slice(-8).reverse().map(x=>'<p class="mini">'+escape(x)+'</p>').join("")+btn("Apagar histórico e progresso","reset","","full")+'</section>'
 +'<section class="card"><h3>Instalação e privacidade</h3><p>Na versão web, o primeiro download completo de imagens e áudios requer conexão. Depois, o conteúdo fica disponível sem internet enquanto o cache for preservado.</p><p>A versão Android Java usa os mesmos conteúdos gravados diretamente no APK e não solicita permissão de internet.</p><p class="meta">'+(offline?"Pacote offline completo ✓":"Aguardando o cache offline…")+'</p></section>'}
function render(){if(!data)return;buttons.forEach(b=>{const active=b.dataset.page===page;b.classList.toggle("active",active);b.setAttribute("aria-current",active?"page":"false")});app.innerHTML=page==="inicio"?renderHome():page==="rezar"?renderPray():page==="misterios"?renderMysteries():page==="calendario"?renderCalendar():renderSettings()}
function play(slug){stopAudio();audio=new Audio("./assets/audio/"+slug+".ogg");audio.play().catch(()=>toast("O áudio não iniciou. Use o texto enquanto reza."))}
function next(){const s=current(),len=steps().length;if(s.done)return;s.index++;if(s.index>=len){s.done=true;s.index=len;saved.history.push(dateKey()+" • Mistérios "+LABELS[selected()]);}persist();render()}
function choose(v){saved.set=v;persist();render()}
document.body.addEventListener("click",e=>{const b=e.target.closest("[data-page],[data-action]");if(!b)return;if(b.dataset.page){go(b.dataset.page);return}const act=b.dataset.action,val=b.dataset.value;
 if(act==="goto")go(val);
 if(act==="start"){if(current().done){current().index=0;current().done=false;persist()}go("rezar")}
 if(act==="restart"){current().index=0;current().done=false;persist();render()}
 if(act==="next")next();
 if(act==="back"){current().index=Math.max(0,current().index-1);persist();render()}
 if(act==="choose")choose(val);
 if(act==="listen")play(val);
 if(act==="silence"){stopAudio();toast("Permaneça em silêncio por "+saved.silence+" segundos.");setTimeout(()=>toast("Continue em oração."),saved.silence*1000)}
 if(act==="reset"&&confirm("Apagar o histórico, as intenções e todo o progresso deste aparelho?")){saved={...DEFAULT,sessions:{},history:[]};persist();go("inicio")}
});
document.body.addEventListener("input",e=>{if(e.target.id==="intention"){saved.intention=e.target.value.slice(0,240);persist()}});
document.body.addEventListener("change",e=>{if(e.target.id==="select-set")choose(e.target.value);if(e.target.id==="fatima"){saved.fatima=e.target.checked;persist();render()}if(e.target.id==="silence"){saved.silence=Number(e.target.value);persist();render()}});
document.addEventListener("visibilitychange",()=>{if(document.hidden)stopAudio()});
async function init(){try{const r=await fetch("./content.json");if(!r.ok)throw Error("conteúdo");data=await r.json();render()}catch{app.innerHTML='<section class="card"><h2>Conteúdo indisponível</h2><p>Na primeira instalação, conecte-se à internet e recarregue a página para guardar o terço completo.</p></section>'}
 if("serviceWorker"in navigator&&location.protocol.startsWith("http"))try{await navigator.serviceWorker.register("./sw.js");await navigator.serviceWorker.ready;const reg=await navigator.serviceWorker.getRegistration();const ch=new MessageChannel();ch.port1.onmessage=e=>{offline=Boolean(e.data?.complete);render()};reg.active?.postMessage({type:"STATUS"},[ch.port2])}catch{}}
init();
})();