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
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private SQLiteDatabase db;
    private SharedPreferences prefs;
    private LinearLayout root, topBar, body, bottomBar, verseList, pageContent;
    private TextView title, subtitle;
    private Spinner bookSpinner, chapterSpinner;
    private EditText searchInput;
    private int currentBook = 1, currentChapter = 1, textSize = 19;
    private boolean dark = false;
    private String page = "home";
    private LinearLayout searchResultsContainer;
    private String activeSearchSql;
    private ArrayList<String> activeSearchArgs = new ArrayList<>();
    private int searchOffset = 0;
    private int searchTotal = 0;
    private Button loadMoreButton;
    private boolean showOldTestament = true;
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
        root=column(); root.setBackgroundColor(bg());
        // Keep content clear of Android status/navigation bars on edge-to-edge devices.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        setContentView(root);
        root.requestApplyInsets();
        topBar=row(); pad(topBar,18,10,18,8);
        LinearLayout brand=column(); TextView name=text("NPV Bible",21,true); name.setTextColor(dark?gold:Color.rgb(125,85,15));
        brand.addView(name); brand.addView(text("New Pilipino Version",12,false));
        topBar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        Button theme=button(dark?"☀":"☾"); topBar.addView(theme);
        theme.setOnClickListener(v->{dark=!dark;prefs.edit().putBoolean("dark",dark).apply();renderPage();});
        root.addView(topBar);
        body=column(); root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        bottomBar=row(); bottomBar.setGravity(Gravity.CENTER); bottomBar.setBackgroundColor(panel()); pad(bottomBar,6,5,6,5);
        bottomBar.setElevation(8);
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
        pageContent = null;
        root.setBackgroundColor(bg()); topBar.setBackgroundColor(bg()); body.removeAllViews();
        refreshNav();
        if(page.equals("home")) showHome();
        else if(page.equals("books")) showBooks();
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
    private LinearLayout contentTarget() { return pageContent != null ? pageContent : body; }
    private void addHeader(String heading,String caption) {
        LinearLayout h=column(); pad(h,18,12,18,10); h.addView(text(heading,21,true));
        if(caption!=null)h.addView(text(caption,13,false)); contentTarget().addView(h);
    }
    private void addCard(View v) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(16,5,16,7);
        v.setBackground(shape(panel(),22,dark?Color.rgb(58,58,58):Color.rgb(236,225,204))); pad(v,16,12,16,12); contentTarget().addView(v,lp);
    }
    private void showHome() {
        // Make the complete home screen scrollable so the full book list is reachable on phones.
        ScrollView homeScroll = new ScrollView(this);
        homeScroll.setFillViewport(true);
        pageContent = column();
        homeScroll.addView(pageContent);
        body.addView(homeScroll, new LinearLayout.LayoutParams(-1,0,1));
        addHeader("Magandang araw!","Basahin at pagnilayan ang Salita ng Diyos.");
        // Panatilihin ang dilaw na Talata ng Araw card at ang salitang "katotohanan".
        LinearLayout hero=column(); hero.setBackground(shape(dark?Color.rgb(70,54,25):Color.rgb(247,218,151),24,0)); pad(hero,18,16,18,16);
        TextView ey=text("TALATA NG ARAW",12,true); ey.setTextColor(Color.rgb(110,72,12)); hero.addView(ey);
        hero.addView(text("“Sapagkat ako ang daan, at ang katotohanan, at ang buhay.”",18,true));
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

        // A single clear entry point opens the separate Old/New Testament book list.
        addHeader("Mga Aklat ng Bibliya","Piliin ang aklat na nais mong basahin.");
        LinearLayout booksCard=column();
        TextView booksInfo=text("66 na aklat  •  Lumang Tipan at Bagong Tipan",13,false);
        booksCard.addView(booksInfo);
        Button openBooks=button("📚  BUKSAN ANG MGA AKLAT  →");
        openBooks.setTextSize(15);
        LinearLayout.LayoutParams openLp=new LinearLayout.LayoutParams(-1,-2);
        openLp.topMargin=10;
        booksCard.addView(openBooks,openLp);
        openBooks.setOnClickListener(v->{page="books";showOldTestament=true;renderPage();});
        addCard(booksCard);
        pageContent = null;
    }

    private void showBooks() {
        ScrollView sc = new ScrollView(this);
        pageContent = column();
        sc.addView(pageContent);
        body.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        addHeader("Pumili ng Aklat","Piliin muna ang Lumang Tipan o Bagong Tipan.");
        LinearLayout tabs=row();
        Button oldTab=button("Lumang Tipan");
        Button newTab=button("Bagong Tipan");
        oldTab.setTextColor(showOldTestament ? gold : fg());
        newTab.setTextColor(!showOldTestament ? gold : fg());
        tabs.addView(oldTab,new LinearLayout.LayoutParams(0,-2,1));
        tabs.addView(newTab,new LinearLayout.LayoutParams(0,-2,1));
        addCard(tabs);
        oldTab.setOnClickListener(v->{showOldTestament=true;renderPage();});
        newTab.setOnClickListener(v->{showOldTestament=false;renderPage();});
        addHeader(showOldTestament ? "Lumang Tipan" : "Bagong Tipan",showOldTestament ? "Genesis hanggang Malakias" : "Mateo hanggang Pahayag");
        addBookGrid(showOldTestament);
        pageContent = null;
    }

    private void addBookGrid(boolean oldTestament) {
        LinearLayout grid = column();
        ArrayList<Integer> visibleBooks = new ArrayList<>();
        for (int id : bookIds) {
            if (oldTestament ? id <= 39 : id >= 40) visibleBooks.add(id);
        }
        for (int i=0; i<visibleBooks.size(); i+=2) {
            LinearLayout r = row();
            for (int j=i; j<Math.min(i+2,visibleBooks.size()); j++) {
                final int bookId = visibleBooks.get(j);
                Button b = button(bookName(bookId)+"  ›");
                b.setTextSize(13);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0,-2,1);
                bp.setMargins(3,3,3,3);
                r.addView(b,bp);
                b.setOnClickListener(v->{currentBook=bookId;currentChapter=1;page="bible";renderPage();});
            }
            grid.addView(r);
        }
        addCard(grid);
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
            // Inalis ang apat na action icons sa ilalim ng talata para luminis ang reading screen.
            // Nananatiling available ang verse actions sa long-press menu.
            card.setOnLongClickListener(view->{verseActions(currentBook,currentChapter,v,scripture);return true;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(16,4,16,7);pad(card,12,10,12,8);verseList.addView(card,lp);
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
        LinearLayout form = column();
        pad(form,18,8,18,4);
        EditText q = new EditText(this);
        q.setSingleLine(true);
        q.setHint("Salita, parirala, o reference: Juan 3:16");
        form.addView(q);
        TextView scopeLabel = text("Saklaw ng paghahanap",13,true);
        pad(scopeLabel,0,10,0,3); form.addView(scopeLabel);
        Spinner scope = new Spinner(this);
        String[] scopes = {"Buong Bibliya (lahat ng aklat)","Isang aklat lamang"};
        scope.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,scopes));
        form.addView(scope);
        TextView bookLabel = text("Pumili ng aklat",13,true);
        pad(bookLabel,0,10,0,3); form.addView(bookLabel);
        Spinner book = new Spinner(this);
        book.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,bookLabels));
        int currentIndex = bookIds.indexOf(currentBook);
        if(currentIndex>=0) book.setSelection(currentIndex);
        form.addView(book);
        TextView hint = text("Halimbawa: pag-ibig, \"pag-ibig ng Diyos\", Juan 3:16, Genesis 1:1, o Awit 23.",11,false);
        pad(hint,0,8,0,0); form.addView(hint);
        bookLabel.setVisibility(View.GONE); book.setVisibility(View.GONE);
        scope.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                int visibility = position==1 ? View.VISIBLE : View.GONE;
                bookLabel.setVisibility(visibility); book.setVisibility(visibility);
            }
        });
        new AlertDialog.Builder(this).setTitle("Maghanap sa NPV Bible").setView(form)
            .setPositiveButton("Hanap",(d,w)->{
                int selectedBook = book.getSelectedItemPosition()>=0 && book.getSelectedItemPosition()<bookIds.size() ? bookIds.get(book.getSelectedItemPosition()) : currentBook;
                search(q.getText().toString(),scope.getSelectedItemPosition()==1,selectedBook);
            }).setNegativeButton("Kanselahin",null).show();
    }

    private String normalizeReferenceText(String value){
        String n=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        return n.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ");
    }

    private boolean searchReference(String query){
        String normalized=normalizeReferenceText(query);
        int matchedBook=-1; String remainder=null; int bestLength=-1;
        for(int id:bookIds){
            String name=normalizeReferenceText(bookName(id));
            if(normalized.startsWith(name+" ") && name.length()>bestLength){
                matchedBook=id; remainder=normalized.substring(name.length()).trim(); bestLength=name.length();
            }
        }
        // Karaniwang Filipino alias na maaaring gamitin sa paghahanap.
        if(matchedBook<0){
            String[][] aliases={{"awit","19"},{"salmo","19"},{"psalm","19"},{"apocalipsis","66"},{"revelation","66"}};
            for(String[] a:aliases){String prefix=a[0]+" ";if(normalized.startsWith(prefix)){matchedBook=Integer.parseInt(a[1]);remainder=normalized.substring(prefix.length()).trim();break;}}
        }
        if(matchedBook<0 || remainder==null)return false;
        Matcher m=Pattern.compile("^(\\d+)(?::(\\d+)(?:-(\\d+))?)?$").matcher(remainder);
        if(!m.matches())return false;
        int chapter=Integer.parseInt(m.group(1));
        Integer firstVerse=m.group(2)==null?null:Integer.parseInt(m.group(2));
        Integer lastVerse=m.group(3)==null?firstVerse:Integer.parseInt(m.group(3));
        StringBuilder where=new StringBuilder("Book=? AND Chapter=?");
        ArrayList<String> args=new ArrayList<>(); args.add(String.valueOf(matchedBook));args.add(String.valueOf(chapter));
        if(firstVerse!=null){where.append(" AND Verse BETWEEN ? AND ?");args.add(String.valueOf(firstVerse));args.add(String.valueOf(lastVerse));}
        Cursor check=db.rawQuery("SELECT COUNT(*) FROM Bible WHERE "+where,args.toArray(new String[0]));
        boolean exists=check.moveToFirst() && check.getInt(0)>0;check.close();
        if(!exists){Toast.makeText(this,"Hindi makita ang reference na "+query,Toast.LENGTH_LONG).show();return true;}
        String label=bookName(matchedBook)+" "+chapter+(firstVerse==null?"":":"+firstVerse+(lastVerse!=null && !lastVerse.equals(firstVerse)?"-"+lastVerse:""));
        openSearchResults("Reference: "+label,where.toString(),args);
        return true;
    }

    private void search(String query, boolean restrictToBook, int selectedBook){
        String q=query.trim();
        if(q.isEmpty()){Toast.makeText(this,"Ilagay ang salitang o reference na hahanapin.",Toast.LENGTH_SHORT).show();return;}
        if(searchReference(q))return;
        boolean exactPhrase=q.length()>=2 && q.startsWith("\"") && q.endsWith("\"");
        if(exactPhrase) q=q.substring(1,q.length()-1).trim();
        if(q.isEmpty()){Toast.makeText(this,"Walang laman ang hinahanap.",Toast.LENGTH_SHORT).show();return;}
        StringBuilder where=new StringBuilder(); ArrayList<String> args=new ArrayList<>();
        if(restrictToBook){where.append("Book=? AND ");args.add(String.valueOf(selectedBook));}
        if(exactPhrase){where.append("Scripture LIKE ? COLLATE NOCASE");args.add("%"+q+"%");}
        else {
            String[] terms=q.split("\\s+");
            ArrayList<String> clean=new ArrayList<>();
            for(String term:terms)if(!term.trim().isEmpty())clean.add(term.trim());
            if(clean.isEmpty()){Toast.makeText(this,"Walang valid na salita sa paghahanap.",Toast.LENGTH_SHORT).show();return;}
            where.append("(");
            for(int i=0;i<clean.size();i++){
                if(i>0)where.append(" OR ");
                where.append("Scripture LIKE ? COLLATE NOCASE");args.add("%"+clean.get(i)+"%");
            }
            where.append(")");
        }
        openSearchResults("Hanap: "+q+(restrictToBook?" • "+bookName(selectedBook):" • Buong Bibliya"),where.toString(),args);
    }

    private void openSearchResults(String description,String where,ArrayList<String> args){
        page="search";body.removeAllViews();refreshNav();
        addHeader("Mga resulta",description);
        ScrollView sc=new ScrollView(this);searchResultsContainer=column();sc.addView(searchResultsContainer);
        body.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        activeSearchSql="SELECT Book,Chapter,Verse,Scripture FROM Bible WHERE "+where;
        activeSearchArgs=new ArrayList<>(args);searchOffset=0;
        Cursor count=db.rawQuery("SELECT COUNT(*) FROM Bible WHERE "+where,args.toArray(new String[0]));
        searchTotal=count.moveToFirst()?count.getInt(0):0;count.close();
        addCardTo(searchResultsContainer,text(searchTotal+" kabuuang resulta",13,true));
        if(searchTotal==0){addCardTo(searchResultsContainer,text("Walang nahanap. Subukan ang ibang salita, parirala, o reference.",14,false));return;}
        loadNextSearchBatch();
    }

    private void loadNextSearchBatch(){
        if(loadMoreButton!=null){searchResultsContainer.removeView(loadMoreButton);loadMoreButton=null;}
        ArrayList<String> args=new ArrayList<>(activeSearchArgs);args.add(String.valueOf(searchOffset));
        Cursor c=db.rawQuery(activeSearchSql+" ORDER BY Book,Chapter,Verse LIMIT 100 OFFSET ?",args.toArray(new String[0]));
        int loaded=0;
        while(c.moveToNext()){
            int b=c.getInt(0),ch=c.getInt(1),v=c.getInt(2);String scripture=c.getString(3);
            LinearLayout item=column();item.addView(text(ref(b,ch,v),15,true));item.addView(text(scripture,15,false));
            Button open=button("Buksan ang kabanata");item.addView(open);
            open.setOnClickListener(view->{currentBook=b;currentChapter=ch;page="bible";renderPage();});
            addCardTo(searchResultsContainer,item);loaded++;
        }
        c.close();searchOffset+=loaded;
        if(searchOffset<searchTotal){
            loadMoreButton=button("Ipakita pa ang mga resulta ("+searchOffset+" / "+searchTotal+")");
            searchResultsContainer.addView(loadMoreButton);
            loadMoreButton.setOnClickListener(v->loadNextSearchBatch());
        } else {
            addCardTo(searchResultsContainer,text("Naipakita na ang lahat ng "+searchTotal+" resulta.",12,false));
        }
    }
    private void addCardTo(LinearLayout target,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(14,5,14,7);v.setBackground(shape(panel(),18,0));pad(v,14,12,14,12);target.addView(v,lp);}
    private void showMessage(String s){addHeader("NPV Bible",s);}
    @Override public void onDestroy(){if(db!=null)db.close();super.onDestroy();}
}
