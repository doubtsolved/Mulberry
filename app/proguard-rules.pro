# Project-specific ProGuard / R8 rules for Mulberry

# Preserve line numbers and source file names for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ------------------------------------------------------------------------------
# Room Database Keep Rules
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * {
    public <methods>;
}
-keep @androidx.room.Entity class * {
    public <fields>;
    public <init>(...);
}
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# Gson Serialization Rules (UserProfile, Metadata, BackupManager)
# ------------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ------------------------------------------------------------------------------
# PdfiumCore Native JNI Bindings (TOC & PDF Metadata extraction)
# ------------------------------------------------------------------------------
-keep class com.shockwave.pdfium.** { *; }
-keepclassmembers class com.shockwave.pdfium.** {
    native <methods>;
    public <fields>;
    public <init>(...);
}

# ------------------------------------------------------------------------------
# Coroutines & General Keep Rules
# ------------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# PDFBox Android ProGuard Rules
# ------------------------------------------------------------------------------
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
