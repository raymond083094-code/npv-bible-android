package com.npvbible.app;

import android.app.Activity;
import android.os.Bundle;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    LinearLayout root, toolbar, content;
    Spinner books, chapters;
    TextView heading;
    EditText searchBox;
    int currentBook = 1, currentChapter = 1, textSize = 18;
    boolean dark = false;
    SharedPreferences prefs;

    String[] bookNames = {"","Genesis","Exodo","Levitico","Mga Bilang","Deuteronomio","Josue","Mga Hukom","Ruth","1 Samuel","2 Samuel","1 Mga Hari","2 Mga Hari","1 Mga Cronica","2 Mga Cronica","Ezra","Nehemias","Esther","Job","Mga Awit","Mga Kawikaan","Mangangaral","Awit ni Solomon","Isaias","Jeremias","Mga Panaghoy","Ezekiel","Daniel","Hosea","Joel","Amos","Obadias","Jonas","Mikas","Nahum","Habacuc","Zefanias","Hagai","Zacarias","Malakias","Mateo","Marcos","Lucas","Juan","Mga Gawa","Roma","1 Corinto","2 Corinto","Galacia","Efeso","Filipos","Colosas","1 Tesalonica","2 Tesalonica","1 Timoteo","2 Timoteo","Tito","Filemon","Hebreo","Santiago","1 Pedro","2 Pedro","1 Juan","2 Juan","3 Juan","Judas","Pahayag"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("npv", 0);
        openDb();
        buildUi();
        if (db != null) {
            loadBooks();
            showChapter();
        }
    }

    void openDb() {
        try {
            File f = new File(getFilesDir(), "npv.sqlite");
            if (!f.exists()) {
                try (InputStream in = getAssets().open("npv.sqlite");
                     FileOutputStream out = new FileOutputStream(f)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
            }
            db = SQLiteDatabase.openDatabase(f.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            Toast.makeText(this, "Hindi mabuksan ang NPV database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    TextView makeText(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        t.setPadding(10, 8, 10, 8);
        return t;
    }

    Button makeButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        return b;
    }

    void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(12, 8, 12, 8);
        setContentView(root);

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        TextView appTitle = makeText("New Pilipino Version Bible", 20, true);
        appTitle.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        toolbar.addView(appTitle);
        Button theme = makeButton("☾");
        theme.setOnClickListener(v -> { dark = !dark; applyTheme(); });
        toolbar.addView(theme);
        root.addView(toolbar);

        books = new Spinner(this);
        chapters = new Spinner(this);
        root.addView(books);
        root.addView(chapters);

        books.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos >= 0 && pos < bookIds.size()) {
                    currentBook = bookIds.get(pos);
                    currentChapter = 1;
                    loadChapters();
                    showChapter();
                }
            }
        });
        chapters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> p) {}
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                currentChapter = pos + 1;
                showChapter();
            }
        });

        LinearLayout searchRow = new LinearLayout(this);
        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setHint("Maghanap ng salita o parirala");
        searchBox.setTextSize(15);
        searchBox.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        searchRow.addView(searchBox);
        Button search = makeButton("Hanap");
        searchRow.addView(search);
        search.setOnClickListener(v -> searchVerses());
        root.addView(searchRow);

        LinearLayout controls = new LinearLayout(this);
        Button smaller = makeButton("A−");
        Button larger = makeButton("A+");
        Button saveChapter = makeButton("Save chapter");
        Button bookmarks = makeButton("Bookmarks");
        controls.addView(smaller);
        controls.addView(larger);
        controls.addView(saveChapter);
        controls.addView(bookmarks);
        root.addView(controls);

        smaller.setOnClickListener(v -> { textSize = Math.max(14, textSize - 2); showChapter(); });
        larger.setOnClickListener(v -> { textSize = Math.min(30, textSize + 2); showChapter(); });
        saveChapter.setOnClickListener(v -> bookmarkChapter());
        bookmarks.setOnClickListener(v -> showBookmarks());

        heading = makeText("", 18, true);
        root.addView(heading);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        applyTheme();
    }

    ArrayList<Integer> bookIds = new ArrayList<>();
    ArrayList<String> bookLabels = new ArrayList<>();

    void loadBooks() {
        bookIds.clear();
        bookLabels.clear();
        Cursor c = db.rawQuery("SELECT DISTINCT Book FROM Bible ORDER BY Book", null);
        while (c.moveToNext()) {
            int id = c.getInt(0);
            bookIds.add(id);
            bookLabels.add(id < bookNames.length ? bookNames[id] : "Book " + id);
        }
        c.close();
        books.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bookLabels));
        int index = bookIds.indexOf(currentBook);
        if (index >= 0) books.setSelection(index);
        loadChapters();
    }

    void loadChapters() {
        if (db == null) return;
        Cursor c = db.rawQuery("SELECT DISTINCT Chapter FROM Bible WHERE Book=? ORDER BY Chapter",
                new String[]{String.valueOf(currentBook)});
        ArrayList<String> values = new ArrayList<>();
        while (c.moveToNext()) values.add(String.valueOf(c.getInt(0)));
        c.close();
        chapters.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        if (currentChapter > values.size()) currentChapter = 1;
        chapters.setSelection(Math.max(0, currentChapter - 1));
    }

    String bookName(int id) {
        return id > 0 && id < bookNames.length ? bookNames[id] : "Book " + id;
    }

    void showChapter() {
        if (db == null || content == null) return;
        heading.setText(bookName(currentBook) + " " + currentChapter + " • NPV");
        content.removeAllViews();
        Cursor c = db.rawQuery("SELECT Verse, Scripture FROM Bible WHERE Book=? AND Chapter=? ORDER BY Verse",
                new String[]{String.valueOf(currentBook), String.valueOf(currentChapter)});
        while (c.moveToNext()) {
            int verse = c.getInt(0);
            String scripture = c.getString(1);
            TextView t = makeText(verse + "   " + scripture, textSize, false);
            t.setTextColor(dark ? Color.WHITE : Color.rgb(35, 35, 35));
            t.setOnLongClickListener(v -> {
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Talata " + verse)
                    .setItems(new String[]{"I-bookmark ang talata", "Kopyahin ang talata"}, (dialog, choice) -> {
                        if (choice == 0) saveVerse(currentBook, currentChapter, verse);
                        else {
                            android.content.ClipboardManager cm =
                                (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("NPV",
                                bookName(currentBook) + " " + currentChapter + ":" + verse + " " + scripture));
                            Toast.makeText(this, "Nakopya ang talata.", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
                return true;
            });
            content.addView(t);
        }
        c.close();
        applyTheme();
    }

    void searchVerses() {
        String q = searchBox.getText().toString().trim();
        if (q.length() < 2) {
            Toast.makeText(this, "Mag-type ng hindi bababa sa 2 character.", Toast.LENGTH_SHORT).show();
            return;
        }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        content.removeAllViews();
        heading.setText("Mga resulta sa paghahanap");
        Cursor c = db.rawQuery("SELECT Book,Chapter,Verse,Scripture FROM Bible WHERE Scripture LIKE ? ORDER BY Book,Chapter,Verse LIMIT 200",
                new String[]{"%" + q + "%"});
        int count = 0;
        while (c.moveToNext()) {
            int b = c.getInt(0), ch = c.getInt(1), v = c.getInt(2);
            String scripture = c.getString(3);
            TextView t = makeText(bookName(b) + " " + ch + ":" + v + "\n" + scripture, 17, false);
            t.setTextColor(dark ? Color.WHITE : Color.BLACK);
            t.setOnClickListener(view -> {
                currentBook = b;
                currentChapter = ch;
                int index = bookIds.indexOf(b);
                if (index >= 0) books.setSelection(index);
                loadChapters();
                chapters.setSelection(Math.max(0, ch - 1));
                showChapter();
            });
            content.addView(t);
            count++;
        }
        c.close();
        if (count == 0) content.addView(makeText("Walang nahanap na resulta.", 17, false));
        else content.addView(makeText(count + " resulta (hanggang 200).", 13, false));
        applyTheme();
    }

    void saveVerse(int book, int chapter, int verse) {
        Set<String> saved = new HashSet<>(prefs.getStringSet("bookmarks", new HashSet<>()));
        saved.add(book + ":" + chapter + ":" + verse);
        prefs.edit().putStringSet("bookmarks", saved).apply();
        Toast.makeText(this, "Na-bookmark ang talata.", Toast.LENGTH_SHORT).show();
    }

    void bookmarkChapter() {
        Cursor c = db.rawQuery("SELECT Verse FROM Bible WHERE Book=? AND Chapter=? ORDER BY Verse",
                new String[]{String.valueOf(currentBook), String.valueOf(currentChapter)});
        Set<String> saved = new HashSet<>(prefs.getStringSet("bookmarks", new HashSet<>()));
        while (c.moveToNext()) saved.add(currentBook + ":" + currentChapter + ":" + c.getInt(0));
        c.close();
        prefs.edit().putStringSet("bookmarks", saved).apply();
        Toast.makeText(this, "Na-save ang mga talata sa kabanatang ito.", Toast.LENGTH_SHORT).show();
    }

    void showBookmarks() {
        content.removeAllViews();
        heading.setText("Mga naka-bookmark");
        Set<String> saved = prefs.getStringSet("bookmarks", new HashSet<>());
        for (String item : saved) {
            String[] parts = item.split(":");
            if (parts.length != 3) continue;
            int b = Integer.parseInt(parts[0]), ch = Integer.parseInt(parts[1]), v = Integer.parseInt(parts[2]);
            Cursor c = db.rawQuery("SELECT Scripture FROM Bible WHERE Book=? AND Chapter=? AND Verse=?",
                    new String[]{String.valueOf(b), String.valueOf(ch), String.valueOf(v)});
            if (c.moveToFirst()) {
                String scripture = c.getString(0);
                TextView t = makeText(bookName(b) + " " + ch + ":" + v + "\n" + scripture, 17, false);
                t.setTextColor(dark ? Color.WHITE : Color.BLACK);
                t.setOnClickListener(view -> {
                    currentBook = b;
                    currentChapter = ch;
                    books.setSelection(bookIds.indexOf(b));
                    loadChapters();
                    chapters.setSelection(Math.max(0, ch - 1));
                    showChapter();
                });
                content.addView(t);
            }
            c.close();
        }
        if (saved.isEmpty()) content.addView(makeText(
                "Wala pang naka-bookmark. Pindutin nang matagal ang isang talata para mag-bookmark.", 16, false));
        applyTheme();
    }

    void applyTheme() {
        int background = dark ? Color.rgb(25, 27, 30) : Color.rgb(250, 248, 242);
        int foreground = dark ? Color.WHITE : Color.rgb(35, 35, 35);
        root.setBackgroundColor(background);
        for (int i = 0; i < root.getChildCount(); i++) root.getChildAt(i).setBackgroundColor(background);
        if (heading != null) heading.setTextColor(foreground);
        if (content != null) {
            content.setBackgroundColor(background);
            for (int i = 0; i < content.getChildCount(); i++) {
                View child = content.getChildAt(i);
                if (child instanceof TextView) ((TextView)child).setTextColor(foreground);
            }
        }
    }

    @Override public void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
          }
