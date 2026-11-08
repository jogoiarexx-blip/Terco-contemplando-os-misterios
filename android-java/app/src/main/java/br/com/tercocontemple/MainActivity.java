package br.com.tercocontemple;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Terço — Contemple os Mistérios Android v0.4.1 — Interface nativa Java, oração e conteúdo offline. */
public final class MainActivity extends Activity {
    private static final int BG=0xff0d252a,CARD=0xff19383b,GOLD=0xffefd1a0,TXT=0xfff7f2e8,SOFT=0xffbaccc8;
    private static final String[] KEYS={"gozosos","luminosos","dolorosos","gloriosos"};
    private static final String[] LABELS={"Gozosos","Luminosos","Dolorosos","Gloriosos"};
    private static final String[] DAY={"gloriosos","gozosos","dolorosos","gloriosos","luminosos","dolorosos","gozosos"};
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private JSONObject content;
    private LinearLayout page;
    private String tab="hoje",detailSet="",selectedDateKey="";
    private Calendar shownMonth=Calendar.getInstance();
    private MediaPlayer player;
    private TextToSpeech tts;
    private Voice offlineVoice;
    private boolean ttsReady=false;
    private Runnable silenceTick;
    private int silenceRemaining=0;
    private boolean fatima=true;

    private static class Step {
        final String kind;final int decade,bead;
        Step(String kind,int decade,int bead){this.kind=kind;this.decade=decade;this.bead=bead;}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        prefs=getSharedPreferences("terco-vivo-v03",MODE_PRIVATE);
        fatima=prefs.getBoolean("fatima",true);
        try(InputStream in=getAssets().open("content.json")){
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            content=new JSONObject(out.toString("UTF-8"));
        }catch(Exception ex){new AlertDialog.Builder(this).setTitle("Conteúdo indisponível").setMessage("Não foi possível abrir o conteúdo local: "+ex.getMessage()).setPositiveButton("Fechar",(d,w)->finish()).show();return;}
        tts=new TextToSpeech(this,status->{
            if(status!=TextToSpeech.SUCCESS||tts==null)return;
            ttsReady=true;tts.setSpeechRate(.82f);
            Set<Voice> voices=tts.getVoices();
            if(voices!=null){
                Voice fallback=null;
                for(Voice v:voices){
                    if(v.isNetworkConnectionRequired() || v.getFeatures()!=null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED))continue;
                    if(!v.getLocale().getLanguage().equals("pt"))continue;
                    if("BR".equals(v.getLocale().getCountry())){offlineVoice=v;break;}
                    fallback=v;
                }
                if(offlineVoice==null)offlineVoice=fallback;
            }
        });
        show("hoje");
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable border(int color,int radius){GradientDrawable d=bg(color,radius);d.setStroke(dp(1),0xff426066);return d;}
    private void pad(View v,int a){v.setPadding(dp(a),dp(a),dp(a),dp(a));}
    private TextView text(String value,int size,int color,boolean bold){
        TextView tv=new TextView(this);tv.setText(value);tv.setTextColor(color);tv.setTextSize(size);tv.setLineSpacing(dp(3),1.07f);
        if(bold)tv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        tv.setTextIsSelectable(false);return tv;
    }
    private void addText(LinearLayout area,String val,int size,int color,boolean bold){TextView tv=text(val,size,color,bold);tv.setPadding(0,dp(5),0,dp(7));area.addView(tv);}
    private LinearLayout stack(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout card(){LinearLayout a=stack();pad(a,17);a.setBackground(border(CARD,17));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(10),0,dp(10));page.addView(a,lp);return a;}
    private Button btn(String label,Runnable click,boolean primary){
        Button b=new Button(this);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(primary?BG:TXT);b.setBackground(border(primary?GOLD:0xff26484b,13));
        b.setMinHeight(dp(49));b.setOnClickListener(v->click.run());return b;
    }
    private void addButton(LinearLayout into,String name,Runnable onClick,boolean primary){
        Button b=btn(name,onClick,primary);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));into.addView(b,p);
    }
    private String dayDate(){return LiturgicalCalendar.iso(Calendar.getInstance());}
    private String selectedSet(){String v=prefs.getString("set","auto");if(!"auto".equals(v))return v;return DAY[Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-1];}
    private String setName(String set){for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(set))return LABELS[i];return set;}
    private JSONObject mysteries(){return content.optJSONObject("MYSTERIES");}
    private JSONObject group(String set){return mysteries().optJSONObject(set);}
    private JSONObject mystery(String set,int decade){return group(set).optJSONArray("items").optJSONObject(decade);}
    private JSONObject prayer(String kind){return content.optJSONObject("PRAYERS").optJSONObject(kind);}
    private String sessionId(){return dayDate()+"|"+selectedSet();}
    private int index(){return Math.max(0,Math.min(prefs.getInt("step:"+sessionId(),0),sequence().size()-1));}
    private boolean finished(){return prefs.getBoolean("done:"+sessionId(),false);}
    private void saveIndex(int index){prefs.edit().putInt("step:"+sessionId(),index).putBoolean("done:"+sessionId(),false).apply();}
    private ArrayList<Step> sequence(){
        ArrayList<Step> out=new ArrayList<>();
        out.add(new Step("cruz",-1,0));out.add(new Step("credo",-1,0));out.add(new Step("pai",-1,0));
        for(int i=1;i<=3;i++)out.add(new Step("ave",-1,i));
        out.add(new Step("gloria",-1,0));
        for(int decade=0;decade<5;decade++){
            out.add(new Step("meditation",decade,0));out.add(new Step("pai",decade,0));
            for(int bead=1;bead<=10;bead++)out.add(new Step("ave",decade,bead));
            out.add(new Step("gloria",decade,0));
            if(fatima)out.add(new Step("fatima",decade,0));
        }
        out.add(new Step("salve",-1,0));out.add(new Step("final",-1,0));out.add(new Step("cruz",-1,0));
        return out;
    }
    private void show(String newTab){
        stopSound();stopSilence();tab=newTab;
        LinearLayout root=stack();root.setBackgroundColor(BG);
        TextView brand=text("✝  TERÇO · CONTEMPLE",19,GOLD,true);brand.setGravity(Gravity.CENTER);brand.setPadding(dp(10),dp(17),dp(10),dp(15));root.addView(brand);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        page=stack();page.setPadding(dp(18),0,dp(18),dp(30));scroll.addView(page);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setHorizontalScrollBarEnabled(false);horizontal.setBackgroundColor(0xff143238);
        LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(5),dp(7),dp(5),dp(9));
        String[] keys={"hoje","rezar","misterios","calendario","historico","ajustes"};
        String[] labels={"Início","Rezar","Mistérios","Calendário","Histórico","Ajustes"};
        for(int i=0;i<keys.length;i++){
            final String key=keys[i];Button b=btn(labels[i],()->show(key),key.equals(tab));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(44));p.setMargins(dp(3),0,dp(3),0);nav.addView(b,p);
        }
        horizontal.addView(nav);root.addView(horizontal);
        setContentView(root);
        if("hoje".equals(tab))home();
        else if("rezar".equals(tab))pray();
        else if("misterios".equals(tab))mysteriesScreen();
        else if("calendario".equals(tab))calendarScreen();
        else if("historico".equals(tab))historyScreen();
        else settingsScreen();
    }
    private void art(LinearLayout target,String set,int num){
        String path="misterios/"+set+"-"+(num+1)+".webp";
        try(InputStream in=getAssets().open(path)){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            Bitmap bitmap=BitmapFactory.decodeStream(in,null,options);
            if(bitmap==null)throw new IllegalStateException("Pintura inválida: "+path);
            ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription("Pintura do mistério "+mystery(set,num).optString("title"));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(235));p.setMargins(0,dp(10),0,dp(12));target.addView(image,p);
        }catch(Exception e){addText(target,"Imagem indisponível",13,SOFT,false);}
    }
    private void title(String small,String big,String description){
        addText(page,small.toUpperCase(new Locale("pt","BR")),12,GOLD,true);addText(page,big,27,TXT,true);
        if(description!=null)addText(page,description,15,SOFT,false);
    }
    private void home(){
        String set=selectedSet();LiturgicalCalendar.Info li=LiturgicalCalendar.day(Calendar.getInstance());
        title("REZE E CONTEMPLE","Um encontro diário com a fé","Sem cadastro e sem internet. Tudo dentro do seu Android.");
        LinearLayout season=card();addText(season,"TEMPO LITÚRGICO · ANO "+li.cycle,12,GOLD,true);
        addText(season,li.celebration.isEmpty()?li.season:li.celebration,20,TXT,true);
        addText(season,"Brasil · "+li.season,14,SOFT,false);
        addButton(season,"Ver calendário litúrgico",()->show("calendario"),false);
        LinearLayout main=card();addText(main,"MISTÉRIOS DE HOJE",12,GOLD,true);addText(main,"Mistérios "+setName(set),24,TXT,true);
        art(main,set,0);addText(main,mystery(set,0).optString("title"),18,TXT,true);
        int percent=finished()?100:index()*100/sequence().size();
        addText(main,"Seu progresso: "+percent+"% · "+(finished()?"Concluído":"salvo automaticamente"),14,SOFT,false);
        addButton(main,finished()?"Rezar novamente":index()>0?"Continuar meu terço":"Começar a rezar",()->{
            if(finished())resetPrayer();show("rezar");
        },true);
        addButton(main,"Contemplar os 20 mistérios",()->show("misterios"),false);
        LinearLayout intent=card();addText(intent,"MINHA INTENÇÃO DE ORAÇÃO",14,GOLD,true);
        EditText value=new EditText(this);value.setSingleLine(false);value.setMaxLines(3);value.setTextColor(TXT);value.setHintTextColor(SOFT);value.setTextSize(16);value.setBackground(border(0xff244548,8));pad(value,10);
        value.setHint("Pela minha família…");value.setText(prefs.getString("intention",""));
        value.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int c,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){prefs.edit().putString("intention",s.toString()).apply();}public void afterTextChanged(Editable e){}});
        intent.addView(value);addText(intent,"Esta intenção permanece somente no aparelho.",12,SOFT,false);
        addText(page,"✓ Orações, 20 pinturas, 28 narrações e calendário incluídos no APK. Sem permissão de internet.",13,GOLD,false);
    }
    private void pray(){
        String set=selectedSet();ArrayList<Step> steps=sequence();
        if(finished()){
            title("TERÇO CONCLUÍDO","Amém!","Você concluiu os cinco mistérios "+setName(set)+" de hoje.");
            addText(card(),"A contemplação continua na vida: fé, esperança e caridade.",18,TXT,false);
            addButton(page,"Rezar novamente",()->{resetPrayer();show("rezar");},true);
            addButton(page,"Meu histórico",()->show("historico"),false);return;
        }
        int pos=index();Step step=steps.get(pos);
        title("ORAÇÃO EM ANDAMENTO","Meu Santo Terço","Etapa "+(pos+1)+" de "+steps.size()+" · "+(pos*100/steps.size())+"% concluído");
        LinearLayout progress=card();
        for(int dec=0;dec<5;dec++){
            int counted=0,total=0;
            for(int j=0;j<steps.size();j++){Step s=steps.get(j);if(s.decade==dec&&s.kind.equals("ave")){total++;if(j<pos)counted++;}}
            addText(progress,"Mistério "+(dec+1)+" · "+counted+"/"+total+" Ave-Marias "+(counted==10?"✓":""),13,counted==10?GOLD:SOFT,false);
            LinearLayout beads=new LinearLayout(this);beads.setGravity(Gravity.CENTER_VERTICAL);
            for(int b=0;b<10;b++){TextView dot=text(b<counted?"●":"○",17,b<counted?GOLD:SOFT,true);dot.setPadding(dp(3),0,dp(3),0);beads.addView(dot);}
            progress.addView(beads);
        }
        String intent=prefs.getString("intention","");if(!intent.isEmpty())addText(page,"♡ Intenção: "+intent,14,GOLD,false);
        LinearLayout body=card();
        if(step.decade>=0){JSONObject m=mystery(set,step.decade);addText(body,(step.decade+1)+"º mistério · "+setName(set),12,GOLD,true);addText(body,m.optString("title"),21,TXT,true);}
        if("meditation".equals(step.kind)){
            JSONObject m=mystery(set,step.decade);art(body,set,step.decade);
            addText(body,"Referência bíblica: "+m.optString("ref"),14,GOLD,true);
            addText(body,"1. Imagine a cena",16,GOLD,true);addText(body,m.optString("scene"),16,TXT,false);
            addText(body,"2. Contemple com o coração",16,GOLD,true);addText(body,m.optString("reflection"),16,TXT,false);
            addText(body,"3. Leve para a vida",16,GOLD,true);addText(body,m.optString("question"),16,TXT,false);
            addText(body,"Fruto espiritual: "+m.optString("fruit"),15,GOLD,true);
        }else{
            JSONObject p=prayer(step.kind);addText(body,p.optString("name"),24,TXT,true);
            if(step.kind.equals("ave") && step.decade>=0)addText(body,step.bead+"ª Ave-Maria da dezena",14,GOLD,true);
            addText(body,p.optString("text"),20,TXT,false);
        }
        addButton(body,"▶ Ouvir em português (offline)",()->playStep(step),false);
        if("meditation".equals(step.kind))addButton(body,"Iniciar silêncio de "+prefs.getInt("silence",30)+" segundos",()->startSilence(prefs.getInt("silence",30)),false);
        LinearLayout buttons=stack();page.addView(buttons);
        if(pos>0)addButton(buttons,"← Voltar uma oração",()->{saveIndex(pos-1);show("rezar");},false);
        addButton(buttons,pos==steps.size()-1?"Concluir terço ✓":"Terminei esta oração →",()->advance(),true);
        addButton(page,"Pausar e voltar ao início",()->show("hoje"),false);
        addText(page,"O avanço é manual para respeitar seu ritmo. Seu progresso permanece no aparelho.",12,SOFT,false);
    }
    private void advance(){int p=index(),length=sequence().size();stopSound();
        if(p<length-1)saveIndex(p+1);
        else {
            prefs.edit().putBoolean("done:"+sessionId(),true).apply();
            try{JSONArray history=new JSONArray(prefs.getString("history","[]"));
                JSONObject entry=new JSONObject().put("date",dayDate()).put("set",selectedSet());history.put(entry);
                prefs.edit().putString("history",history.toString()).apply();
            }catch(Exception e){Toast.makeText(this,"Oração concluída; histórico não pôde ser atualizado.",Toast.LENGTH_LONG).show();}
        }
        show("rezar");
    }
    private void resetPrayer(){prefs.edit().putBoolean("done:"+sessionId(),false).putInt("step:"+sessionId(),0).apply();}
    private void mysteriesScreen(){
        title("20 MISTÉRIOS","Contemple Jesus com Maria","Pinturas e meditações disponíveis em qualquer lugar.");
        for(String key:KEYS){
            LinearLayout groupCard=card();addText(groupCard,"MISTÉRIOS "+setName(key).toUpperCase(Locale.ROOT),14,GOLD,true);
            JSONArray items=group(key).optJSONArray("items");
            for(int i=0;i<5;i++){
                JSONObject m=items.optJSONObject(i);
                addText(groupCard,(i+1)+". "+m.optString("title"),18,TXT,true);art(groupCard,key,i);
                addText(groupCard,m.optString("ref")+" · "+m.optString("fruit"),13,GOLD,false);
                addText(groupCard,m.optString("reflection"),15,TXT,false);
            }
            addButton(groupCard,"Rezar estes mistérios",()->{
                prefs.edit().putString("set",key).apply();show("rezar");
            },true);
        }
    }
    private void calendarScreen(){
        title("CALENDÁRIO LITÚRGICO","O ano com a Igreja","Tempos, cores e principais celebrações do Brasil, sem internet.");
        int y=shownMonth.get(Calendar.YEAR),mon=shownMonth.get(Calendar.MONTH);
        LinearLayout monthCard=card();
        addText(monthCard,new SimpleDateFormat("MMMM 'de' yyyy",new Locale("pt","BR")).format(shownMonth.getTime()),20,GOLD,true);
        LinearLayout buttons=new LinearLayout(this);monthCard.addView(buttons);
        Button prev=btn("‹ Anterior",()->{shownMonth.add(Calendar.MONTH,-1);show("calendario");},false);
        Button next=btn("Próximo ›",()->{shownMonth.add(Calendar.MONTH,1);show("calendario");},false);
        buttons.addView(prev,new LinearLayout.LayoutParams(0,dp(44),1));buttons.addView(next,new LinearLayout.LayoutParams(0,dp(44),1));
        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);monthCard.addView(grid);
        for(String day:new String[]{"D","S","T","Q","Q","S","S"}){
            TextView label=text(day,12,GOLD,true);label.setGravity(Gravity.CENTER);grid.addView(label,gridCell());
        }
        Calendar c=Calendar.getInstance();c.clear();c.set(y,mon,1,12,0);
        int padding=c.get(Calendar.DAY_OF_WEEK)-1,max=c.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<padding;i++){TextView gap=text("",14,TXT,false);grid.addView(gap,gridCell());}
        for(int i=1;i<=max;i++){
            Calendar current=(Calendar)c.clone();current.set(Calendar.DAY_OF_MONTH,i);
            LiturgicalCalendar.Info info=LiturgicalCalendar.day(current);boolean selected=LiturgicalCalendar.iso(current).equals(selectedDateKey);
            Button cell=btn(String.valueOf(i),()->{selectedDateKey=LiturgicalCalendar.iso(current);show("calendario");},selected);
            cell.setTextSize(12);cell.setMinHeight(dp(40));cell.setContentDescription(LiturgicalCalendar.display(current)+", "+(info.celebration.isEmpty()?info.season:info.celebration));
            grid.addView(cell,gridCell());
        }
        if(selectedDateKey.isEmpty())selectedDateKey=dayDate();
        String[] chosen=selectedDateKey.split("-");Calendar selected=Calendar.getInstance();selected.clear();selected.set(Integer.parseInt(chosen[0]),Integer.parseInt(chosen[1])-1,Integer.parseInt(chosen[2]),12,0);
        LiturgicalCalendar.Info info=LiturgicalCalendar.day(selected);
        addText(monthCard,"Dia selecionado: "+LiturgicalCalendar.display(selected),15,GOLD,true);
        addText(monthCard,info.celebration.isEmpty()?info.season:info.celebration,18,TXT,true);
        addText(monthCard,info.season+" · Ano "+info.cycle+" · cor principal: "+info.color,13,SOFT,false);
        LinearLayout dates=card();addText(dates,"DATAS PRINCIPAIS DE "+y,14,GOLD,true);
        for(Map.Entry<String,Calendar> e:LiturgicalCalendar.events(y).entrySet()){
            addText(dates,e.getKey()+" — "+LiturgicalCalendar.display(e.getValue()),14,TXT,false);
        }
        addText(page,"Calendário resumido. Consulte o calendário oficial da sua diocese para festas locais e leituras diárias.",12,SOFT,false);
    }
    private GridLayout.LayoutParams gridCell(){
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(45);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1,1f);
        p.setMargins(dp(1),dp(2),dp(1),dp(2));return p;
    }
    private void historyScreen(){
        title("CAMINHADA DE FÉ","Meu histórico","O histórico fica guardado exclusivamente neste Android.");
        LinearLayout entries=card();JSONArray list;
        try{list=new JSONArray(prefs.getString("history","[]"));}catch(Exception ex){list=new JSONArray();}
        addText(entries,list.length()+" terço(s) concluído(s)",24,GOLD,true);
        if(list.length()==0)addText(entries,"Após a primeira conclusão, seus terços aparecerão aqui.",15,SOFT,false);
        for(int i=list.length()-1;i>=0;i--){JSONObject it=list.optJSONObject(i);if(it!=null)addText(entries,it.optString("date")+" · Mistérios "+setName(it.optString("set")),16,TXT,false);}
    }
    private void settingsScreen(){
        title("PREFERÊNCIAS","Do seu jeito","Escolha o ritmo, as orações e o áudio. Tudo salvo no aparelho.");
        LinearLayout settings=card();
        addText(settings,"MISTÉRIOS DO DIA",14,GOLD,true);
        String[] options={"Automático (dia da semana)","Gozosos","Luminosos","Dolorosos","Gloriosos"};
        String[] vals={"auto","gozosos","luminosos","dolorosos","gloriosos"};
        addButton(settings,"Escolha atual: "+("auto".equals(prefs.getString("set","auto"))?"automático":setName(prefs.getString("set","auto"))),()->{
            new AlertDialog.Builder(this).setTitle("Mistérios para rezar").setItems(options,(dialog,index)->{prefs.edit().putString("set",vals[index]).apply();show("ajustes");}).show();
        },false);
        addButton(settings,"Jaculatória de Fátima: "+(fatima?"ativada":"desativada"),()->{fatima=!fatima;prefs.edit().putBoolean("fatima",fatima).apply();show("ajustes");},false);
        addButton(settings,"Silêncio: "+prefs.getInt("silence",30)+" segundos",()->{
            new AlertDialog.Builder(this).setTitle("Tempo de contemplação").setItems(new String[]{"30 segundos","60 segundos","90 segundos"},(dialog,index)->{
                prefs.edit().putInt("silence",(index+1)*30).apply();show("ajustes");}).show();
        },false);
        addText(settings,"NARRAÇÃO EM PORTUGUÊS",14,GOLD,true);
        boolean useTts=prefs.getBoolean("tts",false);
        addButton(settings,"Modo de voz: "+(useTts?"voz Android offline":"áudio português incluído"),()->{
            if(!useTts&&(!ttsReady||offlineVoice==null)){
                new AlertDialog.Builder(this).setTitle("Voz Android indisponível").setMessage("Instale uma voz portuguesa que funcione offline nas configurações de síntese de voz do Android. O aplicativo continua com os 28 áudios locais.").setPositiveButton("OK",null).show();return;
            }
            prefs.edit().putBoolean("tts",!useTts).apply();show("ajustes");
        },false);
        addText(settings,"A narração incluída é uma voz sintetizada simples em pt-BR. Pode ser usada sem conexão e não depende de serviços externos.",13,SOFT,false);
        LinearLayout privacy=card();addText(privacy,"PRIVACIDADE",14,GOLD,true);
        addText(privacy,"Sem conta, sem anúncios, sem permissão de Internet. Orações, histórico e intenções ficam no Android.",15,TXT,false);
        addButton(privacy,"Apagar histórico e progresso",()->new AlertDialog.Builder(this).setTitle("Apagar progresso?").setMessage("O histórico, as intenções e as sessões serão apagados deste dispositivo.").setNegativeButton("Cancelar",null).setPositiveButton("Apagar",(dialog,which)->{prefs.edit().clear().apply();fatima=true;show("hoje");}).show(),false);
        addText(page,"Terço — Contemple os Mistérios 0.4.1 — Projeto Android nativo em Java. Android 6 ou superior. Fonte e conteúdos incluídos.",12,SOFT,false);
    }
    private void stopSound(){
        if(player!=null){try{player.stop();player.release();}catch(Exception ignored){}player=null;}
        if(tts!=null&&ttsReady)tts.stop();
    }
    private void playStep(Step s){
        stopSound();
        String set=selectedSet(),name=s.kind.equals("meditation")?set+"-"+(s.decade+1)+".mp3":"oracao-"+s.kind+".mp3";
        if(prefs.getBoolean("tts",false)&&offlineVoice!=null&&ttsReady){
            try{
                tts.setVoice(offlineVoice);
                String said;
                if(s.kind.equals("meditation")){
                    JSONObject m=mystery(set,s.decade);said=m.optString("title")+". "+m.optString("scene")+". "+m.optString("reflection")+". "+m.optString("question");
                }else said=prayer(s.kind).optString("text");
                tts.speak(said,TextToSpeech.QUEUE_FLUSH,null,"terco-vivo");return;
            }catch(Exception ignored){}
        }
        try{
            File file=new File(getCacheDir(),name);
            if(!file.exists()||file.length()<100){
                String path="audio/"+name;
                try(InputStream in=getAssets().open(path);OutputStream out=new FileOutputStream(file)){
                    byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                }
            }
            player=new MediaPlayer();player.setDataSource(file.getAbsolutePath());player.setOnCompletionListener(p->{p.release();if(player==p)player=null;});player.setOnErrorListener((mp,what,extra)->{if(player==mp)player=null;mp.release();Toast.makeText(this,"Áudio indisponível neste aparelho. A oração permanece no texto.",Toast.LENGTH_LONG).show();return true;});player.prepare();player.start();
        }catch(Exception ex){Toast.makeText(this,"O áudio não abriu. Você pode rezar acompanhando o texto.",Toast.LENGTH_LONG).show();}
    }
    private void stopSilence(){if(silenceTick!=null){handler.removeCallbacks(silenceTick);silenceTick=null;}silenceRemaining=0;}
    private void startSilence(int seconds){
        stopSilence();silenceRemaining=seconds;
        Toast.makeText(this,"Contemplação: "+seconds+" segundos de silêncio",Toast.LENGTH_SHORT).show();
        silenceTick=new Runnable(){@Override public void run(){if(--silenceRemaining>0){handler.postDelayed(this,1000);}else{silenceTick=null;Toast.makeText(MainActivity.this,"Silêncio concluído. Siga em oração.",Toast.LENGTH_LONG).show();}}};
        handler.postDelayed(silenceTick,1000);
    }
    @Override public void onBackPressed(){if(!tab.equals("hoje"))show("hoje");else super.onBackPressed();}
    @Override protected void onPause(){stopSound();stopSilence();super.onPause();}
    @Override protected void onDestroy(){stopSound();if(tts!=null){tts.shutdown();tts=null;}super.onDestroy();}
}
