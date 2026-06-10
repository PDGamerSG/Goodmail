# Goodmail R8 rules. Compose, Room, Hilt, OkHttp, and Retrofit ship their own
# consumer rules; only reflection the libraries can't declare is listed here.

# Keep crash stack traces readable after obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- kotlinx.serialization ---------------------------------------------------
# Serializers are looked up reflectively via the generated Companion/serializer().
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class com.example.goodmail.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.goodmail.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.goodmail.**$$serializer { *; }

# --- Retrofit ----------------------------------------------------------------
# Keep generic signatures so suspend functions and Response<T> resolve at runtime.
-keepattributes Signature,Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface com.example.goodmail.** {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**

# --- Google API client / Gmail -----------------------------------------------
# The HTTP client populates model classes reflectively via @Key fields.
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-keep class com.google.api.services.gmail.model.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
