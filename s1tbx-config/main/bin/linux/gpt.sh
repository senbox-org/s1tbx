#! /bin/sh

set S1TBX_HOME=${installer:sys.installationDir}

$S1TBX_HOME/jre/bin/java \
    -Xms512M -Xmx${installer:maxHeapSize} \
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods \
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate \
    -Dceres.context=${installer:context} \
    "-D${installer:context}.mainClass=org.esa.beam.framework.gpf.main.GPT" \
    "-D${installer:context}.home=$S1TBX_HOME" \
	"-D${installer:context}.debug=false" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$S1TBX_HOME/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$S1TBX_HOME/libjhdf5.so" \
    -jar $S1TBX_HOME/bin/snap-launcher.jar "$@"

exit 0
