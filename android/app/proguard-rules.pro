# MediaPipe finds protobuf fields by name. Release shrinking renamed
# those classes, which produced "Field platform_ for du not found".
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 {
    <fields>;
}
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**

# MediaPipe logs through Flogger, which finds its caller by walking the
# stack. Release shrinking renamed FluentLogger to "og" and inlined the
# lookup, which throws "no caller found on the stack".
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-dontoptimize
-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod
-assumevalues class com.google.common.flogger.util.FastStackGetter {
    public static com.google.common.flogger.util.FastStackGetter createIfSupported() return null;
}
