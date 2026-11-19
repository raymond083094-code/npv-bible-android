package com.npvbible.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private SQLiteDatabase db;
    private SharedPreferences prefs;
    private LinearLayout root, topBar, body, bottomBar, verseList;
    private TextView title, subtitle;
    private Spinner bookSpinner, chapterSpinner;
    private EditText searchInput;
    private int currentBook = 1, currentChapter = 1, textSize = 19;
    private boolean dark = false;
    private String page = "home";
    private final int gold = Color.rgb(224, 169, 48);
    private final int cream = Color.rgb(250, 246, 236);
    private final int ink = Color.rgb(38, 35, 31);
    private final String[] bookNames = {"","Genesis","Exodo","Levitico","Mga Bilang","Deuteronomio","Josue","Mga Hukom","Ruth","1 Samuel","2 Samuel","1 Mga Hari","2 Mga Hari","1 Mga Cronica","2 Mga Cronica","Ezra","Nehemias","Esther","Job","Mga Awit","Mga Kawikaan","Mangangaral","Awit ni Solomon","Isaias","Jeremias","Mga Panaghoy","Ezekiel","Daniel","Hosea","Joel","Amos","Obadias","Jonas","Mikas","Nahum","Habacuc","Zefanias","Hagai","Zacarias","Malakias","Mateo","Marcos","Lucas","Juan","Mga Gawa","Roma","1 Corinto","2 Corinto","Galacia","Efeso","Filipos","Colosas","1 Tesalonica","2 Tesalonica","1 Timoteo","2 Timoteo","Tito","Filemon","Hebreo","Santiago","1 Pedro","2 Pedro","1 Juan","2 Juan","3 Juan","Judas","Pahayag"};
    private final ArrayList<Integer> bookIds = new ArrayList<>();
    private final ArrayList<String> bookLabels = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("npv", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        textSize = prefs.getInt("textSize", 19);
        currentBook = prefs.getInt("lastBook", 1);
        currentChapter = prefs.getInt("lastChapter", 1);
        openDb();
        buildShell();
        if (db != null) { loadBooks(); showHome(); }
        else showMessage("Hindi mabuksan ang NPV Bible database.");
    }

    private void openDb() {
        try {
            File f = new File(getFilesDir(), "npv.sqlite");
            if (!f.exists()) try (InputStream in=getAssets().open("npv.sqlite"); FileOutputStream out=new FileOutputStream(f)) {
                byte[] b=new byte[8192]; int n; while((n=in.read(b))>0) out.write(b,0,n);
            }
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch(Exception e) { Toast.makeText(this,"Database error: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }

    private int bg() { return dark ? Color.rgb(20,22,26) : cream; }
    private int panel() { return dark ? Color.rgb(34,37,43) : Color.WHITE; }
    private int fg() { return dark ? Color.rgb(245,242,234) : ink; }
    private int muted() { return dark ? Color.rgb(180,180,180) : Color.rgb(115,108,96); }

    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        if(stroke!=0) d.setStroke(1,stroke); return d;
    }
    private TextView text(String s,int size,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(fg());
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setIncludeFontPadding(true); return t;
    }
    private Button button(String s) {
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(13);
        b.setTextColor(dark?fg():ink); b.setBackground(shape(dark?Color.rgb(58,54,45):Color.rgb(248,224,164),18,0));
        return b;
    }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void pad(View v,int l,int t,int r,int b){v.setPadding(l,t,r,b);}
    private void buildShell() {
        root=column(); root.setBackgroundColor(bg()); setContentView(root);
        topBar=row(); pad(topBar,18,12,18,8);
        LinearLayout brand=column(); TextView name=text("NPV Bible",23,true); name.setTextColor(dark?gold:Color.rgb(125,85,15));
        brand.addView(name); brand.addView(text("New Pilipino Version",12,false));
        topBar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button theme=button(dark?"☀":"☾"); topBar.addView(theme);
        theme.setOnClickListener(v->{dark=!dark;prefs.edit().putBoolean("dark",dark).apply();renderPage();});
        root.addView(topBar);
        body=column(); root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        bottomBar=row(); bottomBar.setGravity(Gravity.CENTER); bottomBar.setBackgroundColor(panel()); pad(bottomBar,4,6,4,6);
        root.addView(bottomBar);
        addNav("⌂","Home","home"); addNav("▤","Bibliya","bible"); addNav("★","Paborito","saved"); addNav("✎","Mga Tala","notes"); addNav("☰","Higit Pa","more");
    }
    private void addNav(String icon,String label,String target) {
        LinearLayout item=column(); item.setGravity(Gravity.CENTER); pad(item,3,5,3,5);
        TextView i=text(icon,21,true); i.setTextColor(page.equals(target)?gold:muted()); i.setGravity(Gravity.CENTER);
        TextView l=text(label,10,page.equals(target)); l.setTextColor(page.equals(target)?gold:muted()); l.setGravity(Gravity.CENTER);
        item.addView(i); item.addView(l); bottomBar.addView(item,new LinearLayout.LayoutParams(0,-2,1));
        item.setOnClickListener(v->{page=target;renderPage();});
    }
    private void refreshNav() {
        bottomBar.removeAllViews(); addNav("⌂","Home","home"); addNav("▤","Bibliya","bible");
        addNav("★","Paborito","saved"); addNav("✎","Mga Tala","notes"); addNav("☰","Higit Pa","more");
    }
    private void renderPage() {
        root.setBackgroundColor(bg()); topBar.setBackgroundColor(bg()); body.removeAllViews();
        refreshNav();
        if(page.equals("home")) showHome();
        else if(page.equals("bible")) showBible();
        else if(page.equals("saved")) showSaved();
        else if(page.equals("notes")) showNotes();
        else if(page.equals("history")) showHistory();
        else showMore();
    }
    private void loadBooks() {
        bookIds.clear(); bookLabels.clear();
        Cursor c=db.rawQuery("SELECT DISTINCT Book FROM Bible ORDER BY Book",null);
        while(c.moveToNext()){int id=c.getInt(0);bookIds.add(id);bookLabels.add(bookName(id));} c.close();
        if(!bookIds.contains(currentBook)) currentBook=bookIds.isEmpty()?1:bookIds.get(0);
    }
    private String bookName(int id){return id>0&&id<bookNames.length?bookNames[id]:"Book "+id;}
    private String ref(int b,int ch,int v){return bookName(b)+" "+ch+":"+v;}
    private String key(int b,int ch,int v){return b+":"+ch+":"+v;}
    private Set<String> getSet(String k){return new HashSet<>(prefs.getStringSet(k,new HashSet<>()));}
    private void saveSet(String k,Set<String>s){prefs.edit().putStringSet(k,new HashSet<>(s)).apply();}
    private void addHeader(String heading,String caption) {
        LinearLayout h=column(); pad(h,18,12,18,10); h.addView(text(heading,24,true));
        if(caption!=null)h.addView(text(caption,13,false)); body.addView(h);
    }
    private void addCard(View v) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(14,6,14,8);
        v.setBackground(shape(panel(),22,dark?Color.rgb(58,58,58):Color.rgb(236,225,204))); pad(v,14,12,14,12); body.addView(v,lp);
    }
    private void showHome() {
        addHeader("Magandang araw!","Basahin at pagnilayan ang Salita ng Diyos.");
        LinearLayout hero=column(); hero.setBackground(shape(dark?Color.rgb(70,54,25):Color.rgb(247,218,151),24,0)); pad(hero,20,20,20,20);
        TextView ey=text("TALATA NG ARAW",12,true); ey.setTextColor(Color.rgb(110,72,12)); hero.addView(ey);
        hero.addView(text("“Sapagkat ako ang daan, at ang katotohanan, at ang buhay.”",20,true));
        TextView ref=text("Juan 14:6",13,true); ref.setTextColor(Color.rgb(110,72,12)); hero.addView(ref);
        Button read=button("Magpatuloy sa pagbabasa  →"); hero.addView(read);
        read.setOnClickListener(v->{page="bible";renderPage();});
        addCard(hero);
        addHeader("Ipagpatuloy ang pagbabasa","Huling binasa: "+bookName(currentBook)+" "+currentChapter);
        LinearLayout cont=row(); cont.addView(text("📖  "+bookName(currentBook)+" "+currentChapter,17,true),new LinearLayout.LayoutParams(0,-2,1));
        Button open=button("Buksan");cont.addView(open);open.setOnClickListener(v->{page="bible";renderPage();});
        addCard(cont);
        addHeader("Mabilisang access",null);
        LinearLayout shortcuts=row();
        String[][] opts={{"★","Mga Paborito","saved"},{"⌕","Maghanap","search"},{"✎","Mga Tala","notes"}};
        for(String[] o:opts){Button b=button(o[0]+" "+o[1]);shortcuts.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->{if(o[2].equals("search"))showSearchDialog();else{page=o[2];renderPage();}});}
        addCard(shortcuts);
    }
    private void showBible() {
        addHeader("BIBLIYA","Piliin ang aklat at kabanata.");
        LinearLayout selectors=row(); pad(selectors,14,2,14,8);
        bookSpinner=new Spinner(this);bookSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,bookLabels));
        int bi=bookIds.indexOf(currentBook); if(bi>=0)bookSpinner.setSelection(bi);
        selectors.addView(bookSpinner,new LinearLayout.LayoutParams(0,-2,2));
        chapterSpinner=new Spinner(this);selectors.addView(chapterSpinner,new LinearLayout.LayoutParams(0,-2,1));body.addView(selectors);
        loadChapterSpinner();
        bookSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?>p){} public void onItemSelected(AdapterView<?>p,View v,int pos,long id){if(pos>=0&&pos<bookIds.size()){currentBook=bookIds.get(pos);currentChapter=1;loadChapterSpinner();showVerses();}}});
        chapterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?>p){}public void onItemSelected(AdapterView<?>p,View v,int pos,long id){currentChapter=pos+1;showVerses();}});
        LinearLayout tools=row();pad(tools,14,2,14,8);
        Button search=button("⌕ Hanap");Button smaller=button("A−");Button bigger=button("A+");
        tools.addView(search,new LinearLayout.LayoutParams(0,-2,1));tools.addView(smaller);tools.addView(bigger);body.addView(tools);
        search.setOnClickListener(v->showSearchDialog());
        smaller.setOnClickListener(v->{textSize=Math.max(15,textSize-1);prefs.edit().putInt("textSize",textSize).apply();showVerses();});
        bigger.setOnClickListener(v->{textSize=Math.min(30,textSize+1);prefs.edit().putInt("textSize",textSize).apply();showVerses();});
        ScrollView sc=new ScrollView(this);verseList=column();sc.addView(verseList);body.addView(sc,new LinearLayout.LayoutParams(-1,0,1));showVerses();
    }
    private void loadChapterSpinner(){
        if(chapterSpinner==null)return;
        Cursor c=db.rawQuery("SELECT DISTINCT Chapter FROM Bible WHERE Book=? ORDER BY Chapter",new String[]{""+currentBook});
        ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(""+c.getInt(0));c.close();
        chapterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,a));
        chapterSpinner.setSelection(Math.max(0,Math.min(currentChapter-1,a.size()-1)));
    }
    private void showVerses(){
        if(verseList==null||db==null)return;
        verseList.removeAllViews(); prefs.edit().putInt("lastBook",currentBook).putInt("lastChapter",currentChapter).apply();
        Set<String> hist=getSet("history"); hist.remove(currentBook+":"+currentChapter); hist.add(currentBook+":"+currentChapter); saveSet("history",hist);
        TextView heading=text(bookName(currentBook)+" "+currentChapter+"  •  NPV",19,true);pad(heading,18,12,18,10);verseList.addView(heading);
        Cursor c=db.rawQuery("SELECT Verse,Scripture FROM Bible WHERE Book=? AND Chapter=? ORDER BY Verse",new String[]{""+currentBook,""+currentChapter});
        while(c.moveToNext()){
            final int v=c.getInt(0);final String scripture=c.getString(1);final String k=key(currentBook,currentChapter,v);
            LinearLayout card=column();card.setBackground(shape(panel(),16,dark?Color.rgb(58,58,58):Color.rgb(235,225,207)));
            LinearLayout line=row();TextView verse=text(""+v,15,true);verse.setTextColor(gold);line.addView(verse);
            TextView words=text(scripture,textSize,false);words.setLineSpacing(3,1f);pad(words,10,0,2,0);line.addView(words,new LinearLayout.LayoutParams(0,-2,1));card.addView(line);
            if(getSet("highlights").contains(k))card.setBackground(shape(dark?Color.rgb(65,58,35):Color.rgb(255,237,183),14,gold));
            LinearLayout actions=row(); actions.setGravity(Gravity.END);
            Button mark=smallAction(getSet("bookmarks").contains(k)?"🔖":"♧");
            Button fav=smallAction(getSet("favorites").contains(k)?"★":"☆");
            Button note=smallAction("✎"); Button hi=smallAction("🖍");
            actions.addView(mark);actions.addView(fav);actions.addView(note);actions.addView(hi);card.addView(actions);
            mark.setOnClickListener(view->toggle("bookmarks",k,"Na-bookmark ang "+ref(currentBook,currentChapter,v)));
            fav.setOnClickListener(view->toggle("favorites",k,"Na-update ang paborito."));
            note.setOnClickListener(view->editNote(currentBook,currentChapter,v));
            hi.setOnClickListener(view->{Set<String>s=getSet("highlights");if(s.contains(k))s.remove(k);else s.add(k);saveSet("highlights",s);showVerses();});
            card.setOnLongClickListener(view->{verseActions(currentBook,currentChapter,v,scripture);return true;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(14,4,14,7);pad(card,12,10,12,8);verseList.addView(card,lp);
        } c.close();
    }
    private Button smallAction(String s){Button b=button(s);b.setMinWidth(42);b.setMinimumWidth(42);b.setTextSize(15);return b;}
    private void toggle(String bucket,String k,String msg){Set<String>s=getSet(bucket);if(s.contains(k))s.remove(k);else s.add(k);saveSet(bucket,s);Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();if(page.equals("bible"))showVerses();else renderPage();}
    private void verseActions(int b,int ch,int v,String scripture){
        new AlertDialog.Builder(this).setTitle(ref(b,ch,v)).setItems(new String[]{"I-bookmark","Idagdag sa Paborito","Maglagay ng tala","I-highlight","Kopyahin"},(d,n)->{
            String k=key(b,ch,v);
            if(n==0)toggle("bookmarks",k,"Na-bookmark.");
            if(n==1)toggle("favorites",k,"Naidagdag sa paborito.");
            if(n==2)editNote(b,ch,v);
            if(n==3){Set<String>s=getSet("highlights");if(s.contains(k))s.remove(k);else s.add(k);saveSet("highlights",s);showVerses();}
            if(n==4){ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("NPV",ref(b,ch,v)+" "+scripture));Toast.makeText(this,"Nakopya.",Toast.LENGTH_SHORT).show();}
        }).show();
    }
    private JSONObject notes(){try{return new JSONObject(prefs.getString("notes","{}"));}catch(Exception e){return new JSONObject();}}
    private void editNote(int b,int ch,int v){
        String k=key(b,ch,v);JSONObject all=notes();EditText input=new EditText(this);input.setMinLines(3);input.setText(all.optString(k,""));input.setHint("Isulat ang iyong pagninilay...");
        new AlertDialog.Builder(this).setTitle("Tala: "+ref(b,ch,v)).setView(input).setPositiveButton("I-save",(d,w)->{JSONObject n=notes();try{String val=input.getText().toString().trim();if(val.isEmpty())n.remove(k);else n.put(k,val);prefs.edit().putString("notes",n.toString()).apply();Toast.makeText(this,"Na-save ang tala.",Toast.LENGTH_SHORT).show();}catch(Exception ignored){}}).setNegativeButton("Kanselahin",null).show();
    }
    private void showSaved(){
        addHeader("Mga Paborito at Bookmark","Mga talatang gusto mong balikan.");
        addSavedGroup("Mga Bookmark","bookmarks");addSavedGroup("Mga Paborito","favorites");
    }
    private void addSavedGroup(String label,String bucket){
        addHeader(label,null);Set<String>s=getSet(bucket);ArrayList<String>items=new ArrayList<>(s);Collections.sort(items);
        if(items.isEmpty()){addCard(text("Wala pang laman. Mag-bookmark o magdagdag ng paboritong talata habang nagbabasa.",14,false));return;}
        for(String k:items){String[]p=k.split(":");if(p.length!=3)continue;int b=Integer.parseInt(p[0]),ch=Integer.parseInt(p[1]),v=Integer.parseInt(p[2]);
            Cursor c=db.rawQuery("SELECT Scripture FROM Bible WHERE Book=? AND Chapter=? AND Verse=?",new String[]{""+b,""+ch,""+v});
            if(c.moveToFirst()){LinearLayout item=column();item.addView(text(ref(b,ch,v),15,true));item.addView(text(c.getString(0),15,false));Button open=button("Buksan");item.addView(open);open.setOnClickListener(view->{currentBook=b;currentChapter=ch;page="bible";renderPage();});addCard(item);}c.close();
        }
    }
    private void showNotes(){
        addHeader("Aking mga Tala","Mga pagninilay na isinulat mo sa mga talata.");
        JSONObject n=notes();Iterator<String>it=n.keys();boolean any=false;
        while(it.hasNext()){String k=it.next();any=true;String[]p=k.split(":");if(p.length!=3)continue;int b=Integer.parseInt(p[0]),ch=Integer.parseInt(p[1]),v=Integer.parseInt(p[2]);
            LinearLayout item=column();item.addView(text(ref(b,ch,v),16,true));item.addView(text(n.optString(k),15,false));
            Button edit=button("I-edit");item.addView(edit);edit.setOnClickListener(view->editNote(b,ch,v));addCard(item);
        }
        if(!any)addCard(text("Wala ka pang notes. Pindutin ang ✎ sa tabi ng isang talata para gumawa ng tala.",15,false));
    }
    private void showHistory(){
        addHeader("Reading History","Mga kabanatang huli mong binasa.");
        ArrayList<String> items=new ArrayList<>(getSet("history"));
        Collections.sort(items,Collections.reverseOrder());
        if(items.isEmpty()){addCard(text("Wala pang reading history.",15,false));return;}
        for(String item:items){String[]p=item.split(":");if(p.length!=2)continue;
            int b=Integer.parseInt(p[0]),ch=Integer.parseInt(p[1]);
            LinearLayout card=column();card.addView(text(bookName(b)+" "+ch,17,true));
            Button open=button("Magpatuloy sa pagbabasa");card.addView(open);
            open.setOnClickListener(v->{currentBook=b;currentChapter=ch;page="bible";renderPage();});
            addCard(card);
        }
    }
    private void showMore(){
        addHeader("Higit pang pagpipilian","I-personalize ang iyong pagbabasa.");
        Button theme=button(dark?"Lumipat sa Light Mode":"Lumipat sa Dark Mode");addCard(theme);theme.setOnClickListener(v->{dark=!dark;prefs.edit().putBoolean("dark",dark).apply();renderPage();});
        Button history=button("Reading History");addCard(history);history.setOnClickListener(v->{page="history";renderPage();});
        Button search=button("Maghanap sa Bibliya");addCard(search);search.setOnClickListener(v->showSearchDialog());
        Button size=button("Laki ng teksto: "+textSize+"  (pindutin para dagdagan)");addCard(size);size.setOnClickListener(v->{textSize=textSize>=29?15:textSize+2;prefs.edit().putInt("textSize",textSize).apply();renderPage();});
        Button clear=button("I-reset ang reading history");addCard(clear);clear.setOnClickListener(v->{prefs.edit().remove("history").apply();Toast.makeText(this,"Na-reset ang history.",Toast.LENGTH_SHORT).show();});
    }
    private void showSearchDialog(){
        EditText q=new EditText(this);q.setSingleLine(true);q.setHint("Hal. pag-ibig, pananampalataya");
        new AlertDialog.Builder(this).setTitle("Maghanap sa NPV Bible").setView(q).setPositiveButton("Hanap",(d,w)->search(q.getText().toString())).setNegativeButton("Kanselahin",null).show();
    }
    private void search(String query){
        String q=query.trim();if(q.length()<2){Toast.makeText(this,"Mag-type ng hindi bababa sa 2 character.",Toast.LENGTH_SHORT).show();return;}
        page="search";body.removeAllViews();refreshNav();addHeader("Mga resulta","Paghahanap: "+q);
        ScrollView sc=new ScrollView(this);LinearLayout results=column();sc.addView(results);body.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        Cursor c=db.rawQuery("SELECT Book,Chapter,Verse,Scripture FROM Bible WHERE Scripture LIKE ? ORDER BY Book,Chapter,Verse LIMIT 200",new String[]{"%"+q+"%"});
        int count=0;while(c.moveToNext()){int b=c.getInt(0),ch=c.getInt(1),v=c.getInt(2);String scripture=c.getString(3);
            LinearLayout item=column();item.addView(text(ref(b,ch,v),15,true));item.addView(text(scripture,15,false));Button open=button("Buksan ang kabanata");item.addView(open);open.setOnClickListener(view->{currentBook=b;currentChapter=ch;page="bible";renderPage();});addCardTo(results,item);count++;
        }c.close();if(count==0)addCardTo(results,text("Walang nahanap na resulta.",15,false));else addCardTo(results,text(count+" resulta (hanggang 200).",12,false));
    }
    private void addCardTo(LinearLayout target,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(14,5,14,7);v.setBackground(shape(panel(),18,0));pad(v,14,12,14,12);target.addView(v,lp);}
    private void showMessage(String s){addHeader("NPV Bible",s);}
    @Override public void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
