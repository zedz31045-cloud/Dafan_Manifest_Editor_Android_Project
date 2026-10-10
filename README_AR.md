# Dafan Manifest Editor (Android)

مشروع Android Studio باسم **Dafan Manifest Editor**، بواجهة عربية/إنجليزية.

## الميزات التي تعمل داخل التطبيق
- اختيار `AndroidManifest.xml` عبر منتقي الملفات، ويمكن اختيار الملف من Downloads.
- عرض وتحرير النص وحفظه إلى الملف المحدد عبر Storage Access Framework.
- التحقق من صحة بنية XML قبل الحفظ.
- إنشاء نسخة احتياطية من المحتوى الأصلي داخل مساحة التطبيق الخاصة قبل كل حفظ، مع سجل للنسخ الاحتياطية.
- اختيار APK وتحليل معلوماته الأساسية باستخدام Android PackageManager، مع نسخة بايت-ببايت احتياطية داخل مساحة التطبيق.
- تصدير ملف APK المعدل غير متاح مباشرة داخل التطبيق؛ راجع حدود APK أدناه.

## حدود تعديل APK
`AndroidManifest.xml` داخل APK يكون عادةً بصيغة Android Binary XML، وليس XML نصيًا. التطبيق يستطيع قراءة بيانات الحزمة وعمل نسخة احتياطية من APK، لكنه لا يفك APK ويعيد بناءه/توقيعه بنفسه في هذه النسخة. لهذا يلزم **Apktool** وأدوات Android build/signing خارجية. لا تعرض الواجهة نجاح تعديل APK أو بناءه.

مثال أوامر خارجية على كمبيوتر أو بيئة متوافقة فيها Java وApktool وAndroid build-tools:
```bash
apktool d app.apk -o app_decoded
# عدّل app_decoded/AndroidManifest.xml
apktool b app_decoded -o app-unsigned.apk
# ثم وقّع APK باستخدام apksigner من Android build-tools
apksigner sign --ks my-release-key.jks --out app-signed.apk app-unsigned.apk
apksigner verify app-signed.apk
```
احتفظ بمفتاح توقيعك آمنًا. إعادة التوقيع بمفتاح مختلف تمنع عادةً التحديث فوق التطبيق الأصلي.

## البناء
- Android Studio حديث
- JDK 17
- Android SDK Platform 35
- Android Gradle Plugin 8.6.1 / Gradle 8.7

افتح المجلد في Android Studio ثم اختر **Build > Build APK(s)**. ملف debug المعتاد:
`app/build/outputs/apk/debug/app-debug.apk`

لا يحتوي المشروع على APK جاهز، بل على ملفات المصدر اللازمة لبنائه.
