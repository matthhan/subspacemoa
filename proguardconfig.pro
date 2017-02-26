-injars build/libs/subspacemoa-all.jar(!**.png,!**.jpg,!**.gif,!**.txt,!**.xml,!**.so,!**.dll,!**.jnilib)
-outjars build/libs/subspacemoa-rjar.jar

-libraryjars /usr/lib/jvm/default-java/jre/lib/rt.jar

-dontoptimize
-dontobfuscate
-dontnote
-dontwarn


-keep class moa.r_interface.* {
  *;
}


# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,includedescriptorclasses,allowshrinking class * {
    native <methods>;
}
