# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-keep public class pan.alexander.tordnscrypt.vpn.** {
#  public *;
#}

-keep class com.android.vending.billing.**
#fix android.view.InflateException: Error inflating class com.google.android.material.chip.Chip for Android 4.4.2
-keepclassmembers,allowoptimization,allowobfuscation public class com.google.android.material.chip.** {*;}

-keepattributes *Annotation*,SourceFile,LineNumberTable

# General Options
-verbose

# Suppress duplicate warning for system classes;  Blaze is passing android.jar
# to proguard multiple times.
-dontnote android.**
-dontnote dalvik.**
-dontnote com.android.**
-dontnote google.**
-dontnote com.google.**
-dontnote java.**
-dontnote javax.**
-dontnote junit.**
-dontnote org.apache.**
-dontnote org.json.**
-dontnote org.w3c.dom.**
-dontnote org.xml.sax.**
-dontnote org.xmlpull.v1.**


# Stop warnings about missing unused classes
-dontwarn android.**
-dontwarn dalvik.**
-dontwarn com.android.**
-dontwarn google.**
-dontwarn com.google.**
-dontwarn java.**
-dontwarn javax.**
-dontwarn junit.**
-dontwarn org.apache.**
-dontwarn org.json.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**
-dontwarn org.xmlpull.v1.**


-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod,AnnotationDefault,JavascriptInterface
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keep class com.google.** { *; }
-dontwarn com.google.**
-dontwarn com.squareup.picasso.**

-keepclasseswithmembernames class * {
  native <methods>;
}


# glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}


# okio & okhttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontnote okhttp3.**
-dontnote okio.**
-dontnote com.squareup.okhttp.**
-dontnote org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

-keepparameternames
-renamesourcefileattribute SourceFile
#
# fulldive
-keep class com.fulldive.evry.MainApp { *; }

-dontnote com.fulldive.**
-dontnote in.fulldive.**

-keep class java.lang.invoke.** { *; }
-dontwarn java.lang.invoke.**
-dontnote java.lang.invoke.**

# Needed by google-api-client to keep generic types and @Key annotations accessed via reflection
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}

-keepclasseswithmembers class * {
  @com.google.api.client.util.Value <fields>;
}

-keepnames class com.google.api.client.http.HttpTransport
-keepnames class com.google.api.client.http.javanet.NetHttpTransport

# Needed by Guava (google-api-client)
-dontnote sun.misc.Unsafe
-dontwarn sun.misc.Unsafe


-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

#Keep web libs untouched
-keep class org.w3c.** { *; }

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
# For using GSON @Expose annotation
# Gson specific classes
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.stream.** { *; }
# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
##---------------End: proguard configuration for Gson  ----------

-keep class com.android.vending.billing.**


# Webview
-keep class com.digits.** { *; }
-keep interface com.digits.** { *; }
-keep class com.twitter.** { *; }
-keep interface com.twitter.** { *; }
-keep class com.crashlytics.** { *; }
-dontwarn com.crashlytics.**
-keep public class * extends java.lang.Exception

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-dontnote fqcn.of.javascript.interface.**

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

# Parcelable implementations are accessed by introspection
-keepclassmembers class * implements android.os.Parcelable {*;}
-keep class * implements android.os.Parcelable {*;}
-keepnames class * implements android.os.Parcelable {*;}
-dontwarn com.fulldive.**$Creator

-dontnote org.w3c.**
-dontwarn org.w3c.**
-dontwarn org.w3c.dom.bootstrap.DOMImplementationRegistry

# Tests
-keep class org.amshove.kluent.**{*;}


# Proguard configuration for Jackson 2.x (fasterxml package instead of codehaus package)
-keep class com.fasterxml.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keep class com.fasterxml.jackson.databind.ObjectMapper {
    public <methods>;
    protected <methods>;
}
-keep class com.fasterxml.jackson.databind.ObjectWriter {
    public ** writeValueAsString(**);
}
-dontwarn com.fasterxml.jackson.databind.**

-keep class org.codehaus.** { *; }
-keepclassmembers public final enum com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility {
    public static final com.fasterxml.jackson.annotation.JsonAutoDetect$Visibility *;
}

# Kotlin
-keepclassmembers class **$WhenMappings {
    <fields>;
}

#Kotlin reflect
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keep class org.jetbrains.annotations.** { *; }

-keepclassmembers class ** {
  @org.jetbrains.annotations.ReadOnly public *;
}

-dontwarn kotlin.reflect.**
-dontwarn org.jetbrains.annotations.**
-dontnote kotlin.internal.PlatformImplementationsKt
-dontnote kotlin.reflect.jvm.internal.**
-dontnote org.jetbrains.annotations.**

#apache
-dontnote org.apache.**
-dontwarn org.apache.**
-dontwarn android.net.**
-keep class org.apache.** {*;}
-keep class org.apache.http.** { *; }

-dontwarn **$$Lambda$*

-dontwarn com.fulldive.networking.pulse.services.**
-keepclassmembers class com.fulldive.networking.** { public *; }

# facebook
-dontwarn com.facebook.appevents.**
-dontnote com.facebook.appevents.**

-dontwarn .instantiator.perc.**
-dontwarn org.objenesis.instantiator.perc.**
-dontwarn obj.objenesis.instantiator.sun.**
-dontwarn io.grpc.**
-dontwarn sun.reflect.**
-dontnote obj.objenesis.instantiator.perc.**
-dontnote org.objenesis.instantiator.perc.**
-dontnote obj.objenesis.instantiator.sun.**
-dontnote io.grpc.**
-dontnote sun.reflect.**

-keep class io.grpc.internal.DnsNameResolveProvider
-keep class io.grpc.okhttp.OkHttpChannelProvider
-keep class io.grpc.override.ContextStorageOverride
-keep class COM.newmonics.PercClassLoader.**

-keep class androidx.core.app.CoreComponentFactory { *; }
-keep class androidx.** { *; }
-keepnames class androidx.** { *; }
-keepclassmembers class androidx.** {
    *;
}

# retrofit
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Pollfish SDK
-dontwarn com.pollfish.**
-keep class com.pollfish.** { *; }

# TapResearch SDK
-keep class com.tapr.** { *; }
-keep interface com.tapr.** { *; }


# MoPub Proguard Config

# Keep public classes and methods.
-keepclassmembers class com.mopub.** { *; }
-keep public class com.mopub.**
-keep public class android.webkit.JavascriptInterface {}

# Explicitly keep any BaseAd and CustomEventNative classes in any package.
-keep class * extends com.mopub.mobileads.BaseAd {}
-keep class * extends com.mopub.nativeads.CustomEventNative {}
-keep class * extends com.mopub.nativeads.MoPubStreamAdPlacer {}

# Keep methods that are accessed via reflection
-keepclassmembers class ** { @com.mopub.common.util.ReflectionTarget *; }

# Support for Android Advertiser ID.
-keep class com.google.android.gms.common.GooglePlayServicesUtil {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {*;}

# Support for Google Play Services
# http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keepnames class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

#
# chat
#

# For com.fasterxml.jackson to work on versions of Android prior to L.
-keep class java.beans.Transient.** {*;}
-keep class java.beans.ConstructorProperties.** {*;}
-keep class java.nio.file.Path.** {*;}

# Classes which define json wire protocol.
-keepnames class com.fasterxml.jackson.** {*;}
-keepnames interface com.fasterxml.jackson.** {*;}
-dontwarn com.fasterxml.jackson.databind.**
-keep public class * extends java.lang.Exception

# Don't mangle serializable classes.
-keep class * implements java.io.Serializable

# Pangle
-keep class com.bytedance.sdk.** { *; }
-keep class com.pgl.sys.ces.* {*;}