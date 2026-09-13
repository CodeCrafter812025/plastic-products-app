# kotlinx.serialization — standard rules from the library's own documentation
# (https://github.com/Kotlin/kotlinx.serialization#android), adapted to this
# app's package. Without these, R8 can strip or rename the generated
# $serializer companions for the @Serializable data classes in data/model/,
# breaking Retrofit's kotlinx-serialization converter at runtime.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class ir.codecrafter.plasticproducts.**$$serializer { *; }
-keepclassmembers class ir.codecrafter.plasticproducts.** {
    *** Companion;
}
-keepclasseswithmembers class ir.codecrafter.plasticproducts.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Tink (used under androidx.security:security-crypto) references these
# compile-time-only annotations (Google's error-prone, javax.annotation,
# the Checker Framework) that aren't on the runtime classpath — a known R8
# "missing class" warning with Tink, harmless since nothing here is actually
# called at runtime.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
