package br.com.tercocontemple;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.text.SimpleDateFormat;

/** Cálculo offline das estações do rito romano no Brasil (não é Ordo oficial). */
public final class LiturgicalCalendar {
    private LiturgicalCalendar() { }
    private static Calendar date(int y,int month,int day) {
        Calendar c = new GregorianCalendar(y,month-1,day,12,0,0);
        c.set(Calendar.MILLISECOND,0);return c;
    }
    private static Calendar plus(Calendar base,int days) {
        Calendar c=(Calendar)base.clone();c.add(Calendar.DAY_OF_MONTH,days);return c;
    }
    private static boolean ge(Calendar a,Calendar b) { return a.compareTo(b)>=0; }
    private static boolean le(Calendar a,Calendar b) { return a.compareTo(b)<=0; }
    public static String iso(Calendar date) { return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.getTime()); }
    public static String display(Calendar date) { return new SimpleDateFormat("dd/MM/yyyy",new Locale("pt","BR")).format(date.getTime()); }
    public static Calendar easter(int y) {
        int a=y%19,b=y/100,c=y%100,x=b/4,e=b%4,f=(b+8)/25,g=(b-f+1)/3;
        int h=(19*a+b-x-g+15)%30,i=c/4,k=c%4,L=(32+2*e+2*i-h-k+7)%7;
        int m=(a+11*h+22*L)/451,t=h+L-7*m+114;
        return date(y,t/31,t%31+1);
    }
    public static LinkedHashMap<String,Calendar> events(int y) {
        Calendar easter=easter(y),christmas=date(y,12,25);
        int day=christmas.get(Calendar.DAY_OF_WEEK);int weekday=day==Calendar.SUNDAY?7:day-1;
        Calendar epiphany=date(y,1,2);
        while(epiphany.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY)epiphany.add(Calendar.DATE,1);
        LinkedHashMap<String,Calendar> out=new LinkedHashMap<>();
        out.put("Epifania",epiphany);out.put("Batismo do Senhor",plus(epiphany,7));
        out.put("Quarta-feira de Cinzas",plus(easter,-46));out.put("Domingo de Ramos",plus(easter,-7));
        out.put("Quinta-feira Santa",plus(easter,-3));out.put("Sexta-feira da Paixão",plus(easter,-2));
        out.put("Sábado Santo",plus(easter,-1));out.put("Páscoa",easter);
        out.put("Ascensão (Brasil)",plus(easter,42));out.put("Pentecostes",plus(easter,49));
        out.put("Corpus Christi",plus(easter,60));out.put("Início do Advento",plus(christmas,-(weekday+21)));
        out.put("Natal",christmas);return out;
    }
    public static Info day(Calendar day) {
        int y=day.get(Calendar.YEAR);LinkedHashMap<String,Calendar> e=events(y);
        Calendar christmas=e.get("Natal"),lent=e.get("Quarta-feira de Cinzas"),triduum=e.get("Quinta-feira Santa"),easter=e.get("Páscoa"),pent=e.get("Pentecostes"),advent=e.get("Início do Advento");
        String season="Tempo Comum",color="verde";
        if(le(day,e.get("Batismo do Senhor"))) { season="Tempo do Natal";color="branco"; }
        if(ge(day,lent)&&!ge(day,triduum)) { season="Quaresma";color="roxo"; }
        if(ge(day,triduum)&&!ge(day,easter)) { season="Tríduo Pascal";color="branco"; }
        if(ge(day,easter)&&le(day,pent)) { season="Tempo Pascal";color="branco"; }
        if(ge(day,advent)&&!ge(day,christmas)) { season="Advento";color="roxo"; }
        if(ge(day,christmas)) { season="Tempo do Natal";color="branco"; }
        String celeb="";
        for(String title:e.keySet())if(iso(day).equals(iso(e.get(title)))) {celeb=title;break;}
        if(celeb.isEmpty()) {
            if(iso(day).endsWith("-10-12"))celeb="Nossa Senhora Aparecida — Padroeira do Brasil";
            if(iso(day).endsWith("-01-01"))celeb="Santa Maria, Mãe de Deus";
            if(iso(day).endsWith("-11-02"))celeb="Fiéis Defuntos";
        }
        int liturgicalYear=ge(day,advent)?y+1:y;
        String[] years={"A","B","C"};int index=((liturgicalYear-2026)%3+3)%3;
        return new Info(season,color,celeb,years[index]);
    }
    public static final class Info {
        public final String season,color,celebration,cycle;
        public Info(String season,String color,String celebration,String cycle) {
            this.season=season;this.color=color;this.celebration=celebration;this.cycle=cycle;
        }
    }
}
