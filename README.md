# New Pilipino Version Bible (Android)

Offline Android Bible reader built from the uploaded `NPV.bbl.mybible` SQLite database.

## Included
- Offline reading (the SQLite database is bundled in app assets)
- Book and chapter selection based on books present in the supplied database
- Full-text substring search across Scripture
- Adjustable reading text size
- Dark/light theme toggle
- Long-press a verse to bookmark or copy it
- Bookmark list stored locally on the device

## Build
1. Install Android Studio (Hedgehog or newer) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Let Gradle sync.
4. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio creates the APK under `app/build/outputs/apk/debug/`.

The project uses Java and Android platform widgets; no network permission or runtime internet access is requested.

## Database provenance
The supplied database's `Details` row identifies the text as **Bagong Salin sa Pilipino (NPV), New Pilipino Version**, by Manila International Bible Society, 1986, with digital text conversion/update noted as July 23, 2018. The database contains 65 distinct book IDs and 31,077 verse records. The app displays the records present in the database; it does not add missing books or alter the Scripture text.
