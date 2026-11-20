# Settings are stored as JSON via kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class app.liveisland.**$$serializer { *; }
-keepclassmembers class app.liveisland.** { *** Companion; }
