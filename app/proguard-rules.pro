# Add project specific ProGuard rules here.
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# JSR 305 annotations are often optional or provided at compile-time only
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# --- Room ---
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-keep class * implements androidx.room.RoomDatabaseDelegate { *; }
-keep class androidx.room.util.TableInfo { *; }
-keep class androidx.room.util.TableInfo$Column { *; }
-keep class androidx.room.util.TableInfo$ForeignKey { *; }
-keep class androidx.room.util.TableInfo$Index { *; }

# --- SQLCipher ---
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.SQLiteDatabase { *; }
-keep class net.zetetic.database.sqlcipher.SQLiteOpenHelper { *; }
-keep class net.zetetic.database.sqlcipher.SupportOpenHelperFactory { *; }
-keepclassmembers class net.zetetic.database.sqlcipher.SQLiteDatabase {
    private long nativeHandle;
}

# --- AndroidX Crypto ---
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# --- App Data Models ---
-keep class dev.bmg.edgeclip.data.** { *; }
