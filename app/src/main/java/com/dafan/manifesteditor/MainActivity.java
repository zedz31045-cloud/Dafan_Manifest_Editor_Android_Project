package com.dafan.manifesteditor;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int PICK_MANIFEST = 10;
    private static final int PICK_APK = 11;
    private static final int CREATE_EXPORT = 12;
    private Uri manifestUri;
    private Uri apkUri;
    private EditText editor;
    private TextView status;
    private TextView picked;
    private boolean english = false;
    private String pendingExportText;
    private String pendingExportName = "manifest-backup.xml";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        render();
    }

    private String tr(String ar, String en) { return english ? en : ar; }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    private void render() {
        ScrollView sc = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(20));
        root.setBackgroundColor(0xFFF3F6FA);
        sc.addView(root);

        TextView title = new TextView(this);
        title.setText("Dafan Manifest Editor"); title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setTextColor(0xFF14213D);
        root.addView(title);
        TextView sub = new TextView(this);
        sub.setText(tr("محرر AndroidManifest.xml وفاحص APK","AndroidManifest.xml editor and APK inspector"));
        sub.setTextSize(14); sub.setTextColor(0xFF4B5D73); sub.setPadding(0, dp(5), 0, dp(10)); root.addView(sub);

        addButton(root, tr("🌐 English / العربية","🌐 العربية / English"), v -> { english = !english; render(); });
        addButton(root, tr("📄 اختيار AndroidManifest.xml من Downloads أو أي مجلد","📄 Choose AndroidManifest.xml from Downloads or any folder"), v -> pickManifest());
        addButton(root, tr("📦 اختيار APK لتحليل معلوماته وعمل نسخة احتياطية","📦 Select APK to inspect and back it up"), v -> pickApk());
        picked = new TextView(this); picked.setText(tr("لم يتم اختيار ملف بعد","No file selected"));
        picked.setTextColor(0xFF243247); picked.setTextSize(12); picked.setPadding(dp(8), dp(8), dp(8), dp(8)); root.addView(picked);

        TextView label = new TextView(this); label.setText(tr("محتوى Manifest النصي","Manifest text"));
        label.setTextSize(16); label.setTypeface(Typeface.DEFAULT, Typeface.BOLD); label.setTextColor(0xFF14213D);
        label.setPadding(0, dp(8), 0, dp(5)); root.addView(label);
        editor = new EditText(this); editor.setGravity(Gravity.TOP | Gravity.START); editor.setTextSize(12);
        editor.setTypeface(Typeface.MONOSPACE); editor.setMinLines(12); editor.setPadding(dp(10), dp(10), dp(10), dp(10));
        editor.setHint("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">...");
        editor.setBackgroundColor(0xFFFFFFFF); root.addView(editor, new LinearLayout.LayoutParams(-1, dp(280)));

        addButton(root, tr("🔎 التحقق من XML","🔎 Validate XML"), v -> validateXml());
        addButton(root, tr("💾 حفظ مع نسخة احتياطية تلقائية","💾 Save with automatic backup"), v -> saveManifest());
        addButton(root, tr("📤 تصدير نسخة من النص","📤 Export text copy"), v -> exportText());
        addButton(root, tr("🗂️ عرض النسخ الاحتياطية","🗂️ List backups"), v -> listBackups());
        addButton(root, tr("🧰 تعليمات تعديل APK","🧰 APK editing instructions"), v -> showApkHelp());

        status = new TextView(this); status.setText(tr("الحالة: جاهز. اختر Manifest نصيًا للتعديل أو APK للفحص.","Status: Ready. Select a text Manifest to edit or an APK to inspect."));
        status.setTextSize(13); status.setTextColor(0xFF243247); status.setBackgroundColor(0xFFFFFFFF);
        status.setPadding(dp(12), dp(12), dp(12), dp(12)); status.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2); slp.setMargins(0, dp(12), 0, 0); root.addView(status, slp);
        TextView note = new TextView(this);
        note.setText(tr("تنبيه: APK يحتوي غالبًا على Manifest ثنائي. التطبيق يحلل APK ويحفظ نسخة منه، لكنه لا يعيد بناء APK أو توقيعه داخليًا. تحتاج Apktool وapksigner خارجيين لذلك.",
                "Note: APKs usually contain binary manifests. This app inspects and backs up APKs but does not rebuild or sign them internally. External Apktool and apksigner are required."));
        note.setTextSize(12); note.setTextColor(0xFF8A3B12); note.setPadding(0, dp(10), 0, 0); root.addView(note);
        setContentView(sc);
    }

    private interface Click { void on(View v); }
    private void addButton(LinearLayout root, String text, Click click) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(14); b.setOnClickListener(v -> click.on(v));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(4), 0, dp(4)); root.addView(b, lp);
    }

    private void pickManifest() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_MANIFEST);
    }
    private void pickApk() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_APK);
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        try { getContentResolver().takePersistableUriPermission(u, data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION)); } catch (Exception ignored) {}
        if (requestCode == PICK_MANIFEST) {
            manifestUri = u;
            try { editor.setText(readText(u)); picked.setText(tr("Manifest: ", "Manifest: ") + displayName(u)); status.setText(tr("تم تحميل الملف. تحقق من XML قبل الحفظ.", "File loaded. Validate XML before saving.")); }
            catch (Exception e) { status.setText(tr("تعذرت قراءة الملف: ", "Could not read file: ") + e.getMessage()); }
        } else if (requestCode == PICK_APK) {
            apkUri = u;
            picked.setText("APK: " + displayName(u));
            inspectApk(u);
        } else if (requestCode == CREATE_EXPORT) {
            try (OutputStream out = getContentResolver().openOutputStream(u, "w")) { out.write(pendingExportText.getBytes("UTF-8")); status.setText(tr("تم تصدير الملف.", "File exported.")); }
            catch (Exception e) { status.setText(tr("فشل التصدير: ", "Export failed: ") + e.getMessage()); }
        }
    }

    private String readText(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new Exception("Input stream unavailable");
            byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return out.toString("UTF-8");
        }
    }
    private String displayName(Uri uri) {
        Cursor c = null;
        try { c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null); if (c != null && c.moveToFirst()) return c.getString(0); }
        catch (Exception ignored) {} finally { if (c != null) c.close(); }
        return uri.getLastPathSegment() == null ? uri.toString() : uri.getLastPathSegment();
    }

    private void validateXml() {
        String content = editor.getText().toString();
        if (content.trim().isEmpty()) { status.setText(tr("لا يوجد نص لفحصه.", "There is no text to validate.")); return; }
        try {
            XmlPullParser p = Xml.newPullParser(); p.setInput(new StringReader(content));
            while (p.next() != XmlPullParser.END_DOCUMENT) {}
            status.setText(tr("✅ بنية XML صحيحة. هذا لا يضمن صحة إعدادات Android أو نجاح البناء.", "✅ XML syntax is valid. This does not guarantee Android configuration or build validity."));
        } catch (Exception e) { status.setText(tr("❌ خطأ XML: ", "❌ XML error: ") + e.getMessage()); }
    }

    private File backupDir() { File d = new File(getFilesDir(), "backups"); if (!d.exists()) d.mkdirs(); return d; }
    private String stamp() { return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()); }
    private void saveManifest() {
        if (manifestUri == null) { status.setText(tr("اختر ملف Manifest أولًا.", "Select a Manifest file first.")); return; }
        String content = editor.getText().toString();
        if (!isXmlValid(content)) { status.setText(tr("لم يتم الحفظ: أصلح خطأ XML أولًا.", "Not saved: fix the XML error first.")); return; }
        try {
            String original = readText(manifestUri);
            File backup = new File(backupDir(), "manifest_" + stamp() + ".xml.bak");
            try (FileOutputStream fos = new FileOutputStream(backup)) { fos.write(original.getBytes("UTF-8")); }
            try (OutputStream out = getContentResolver().openOutputStream(manifestUri, "wt")) {
                if (out == null) throw new Exception("Cannot open selected file for writing");
                out.write(content.getBytes("UTF-8")); out.flush();
            }
            status.setText(tr("✅ تم الحفظ. النسخة الاحتياطية المحلية: ", "✅ Saved. Local backup: ") + backup.getAbsolutePath());
        } catch (Exception e) { status.setText(tr("فشل الحفظ: ", "Save failed: ") + e.getMessage()); }
    }
    private boolean isXmlValid(String content) {
        try { XmlPullParser p = Xml.newPullParser(); p.setInput(new StringReader(content)); while (p.next() != XmlPullParser.END_DOCUMENT) {} return true; }
        catch (Exception e) { return false; }
    }
    private void exportText() {
        pendingExportText = editor.getText().toString(); pendingExportName = "manifest-export.xml";
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/xml"); i.putExtra(Intent.EXTRA_TITLE, pendingExportName); startActivityForResult(i, CREATE_EXPORT);
    }
    private void listBackups() {
        File[] fs = backupDir().listFiles(); StringBuilder s = new StringBuilder();
        if (fs == null || fs.length == 0) s.append(tr("لا توجد نسخ احتياطية بعد.", "No backups yet."));
        else { for (File f : fs) s.append(f.getName()).append(" — ").append(f.length()).append(" bytes\n"); }
        status.setText(s.toString());
    }
    private void inspectApk(Uri uri) {
        try {
            File backup = new File(backupDir(), "apk_" + stamp() + ".apk.bak");
            try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(backup)) {
                if (in == null) throw new Exception("Cannot read APK"); byte[] b = new byte[16384]; int n; while ((n = in.read(b)) > 0) out.write(b, 0, n);
            }
            PackageManager pm = getPackageManager(); PackageInfo info;
            if (android.os.Build.VERSION.SDK_INT >= 33) info = pm.getPackageArchiveInfo(backup.getAbsolutePath(), PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS));
            else info = pm.getPackageArchiveInfo(backup.getAbsolutePath(), PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS);
            StringBuilder s = new StringBuilder(); s.append(tr("نسخة APK محفوظة في مساحة التطبيق:\n", "APK backup saved in app-private storage:\n")).append(backup.getAbsolutePath()).append("\n\n");
            if (info == null) s.append(tr("تعذر قراءة بيانات الحزمة. قد يكون الملف غير صالح أو غير متوافق.", "Could not read package metadata. The file may be invalid or incompatible."));
            else {
                s.append("Package: ").append(info.packageName).append("\n");
                if (android.os.Build.VERSION.SDK_INT >= 28) s.append("Version: ").append(info.getLongVersionCode()).append("\n"); else s.append("Version: ").append(info.versionCode).append("\n");
                s.append("Min SDK: ").append(info.applicationInfo == null ? "unknown" : info.applicationInfo.minSdkVersion).append("\n");
                s.append("Target SDK: ").append(info.applicationInfo == null ? "unknown" : info.applicationInfo.targetSdkVersion).append("\n\n");
                s.append(tr("معلومات APK قُرئت بنجاح. تعديل Manifest الثنائي وإعادة بناء APK يحتاجان Apktool وapksigner خارجيين.", "APK metadata read successfully. Editing binary Manifest and rebuilding requires external Apktool and apksigner."));
            }
            status.setText(s.toString());
        } catch (Exception e) { status.setText(tr("فشل تحليل/نسخ APK: ", "APK inspect/backup failed: ") + e.getMessage()); }
    }
    private void showApkHelp() {
        String help = "apktool d app.apk -o app_decoded\n" +
                "# edit app_decoded/AndroidManifest.xml\n" +
                "apktool b app_decoded -o app-unsigned.apk\n" +
                "apksigner sign --ks my-release-key.jks --out app-signed.apk app-unsigned.apk\n" +
                "apksigner verify app-signed.apk\n\n" +
                tr("تحتاج Java وApktool وAndroid build-tools. احفظ نسخة من APK الأصلي ولا تستخدم مفتاح توقيع غير موثوق.", "Requires Java, Apktool, and Android build-tools. Keep the original APK and protect your signing key.");
        status.setText(help);
        ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("APK instructions", help));
        Toast.makeText(this, tr("تم عرض التعليمات ونسخها", "Instructions shown and copied"), Toast.LENGTH_SHORT).show();
    }
}
