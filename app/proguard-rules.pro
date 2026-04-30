# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Vosk (JNA tabanlı native kütüphane) ──────────────────────────────────────
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

# ── Data modelleri (Room entity'leri reflection kullanır) ────────────────────
-keep class com.borc.takip.data.model.** { *; }

# ── Genel Kotlin ─────────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
