# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/gradle/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard/index.html

# Keep Firebase classes
-keep class com.google.firebase.** { *; }

# Keep Room classes
-keep class androidx.room.** { *; }

# Keep Stripe classes
-keep class com.stripe.** { *; }

# Keep iText classes
-keep class com.itextpdf.** { *; }

# Keep GSON classes
-keep class com.google.gson.** { *; }
-keep class com.smartexam.models.** { *; }
