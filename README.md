#SubspaceMOA

The subspspaceMOA package is a collections of algorithms and evaluation measures for Subspace Stream Clustering, i.e. Stream Clustering for high-dimensional data, wherein data points are only clustered in certain subspaces of the data space. It builds on the famous MOA package for data stream analysis and the opensubspace package.

##Build Instructions

To build SubspaceMOA, you should have the Gradle build tool installed. Then you can use the command `gradle fatJar` to build a jar that contains the package and all of its dependencies. This jar can be found in the `build/libs` folder.

Otherwise, you can run `gradle <task>` to run any of a list of tasks. The most important tasks are `jar`, which creates a jar that does not contain the dependencies, `test`, which runs a battery of tests and minify, which creates a minified version of the jar containing the package and all of its dependencies. This minified version is especially also used in the R package subspaceMOA.

To run the minify task, one should also have proguard installed and the Java Home folder should be located at `/usr/lib/jvm/default-java`.

