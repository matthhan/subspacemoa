-libraryjars /usr/lib/jvm/default-java/jre/lib/rt.jar

-dontoptimize
-dontobfuscate
-dontnote
-dontwarn


-keep class moa.clusterers.predeconStream.* {
    <methods>;
}

-keep class moa.clusterers.hddstream.* {
    <methods>;
}

-keep class moa.clusterers.macrosubspace.* {
    <methods>;
}

-keep class moa.clusterers.clustream.* {
    <methods>;
}

-keep class moa.clusterers.denstream.* {
    <methods>;
}

-keep class moa.streams.clustering.RandomRBFSubspaceGeneratorEvents {
    <methods>;
}

-keep class moa.streams.clustering.SubspaceARFFStream {
    <methods>;
}

-keep class moa.core.SubspaceInstance {
    <methods>;
}

-keep class moa.clusterers.AbstractSubspaceClusterer {
    <methods>;
}

-keep class moa.cluster.SubspaceClustering {
    <methods>;
}

# Keep names - Native method names. Keep all native class/method names.
-keepclasseswithmembers,includedescriptorclasses,allowshrinking class * {
    native <methods>;
}
