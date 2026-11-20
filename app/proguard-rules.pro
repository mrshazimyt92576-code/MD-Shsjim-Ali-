# Preserve Firestore data models and annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }
