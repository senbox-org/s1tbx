#! /bin/sh

${installer:sys.installationDir}/jre/bin/java \
    -Xms512M -Xmx${installer:maxHeapSize} \
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods \
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate \
    -Dceres.context=${installer:context} \
    "-D${installer:context}.mainClass=org.esa.snap.framework.gpf.main.GPT" \
    "-D${installer:context}.home=${installer:sys.installationDir}" \
	"-D${installer:context}.debug=false" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=${installer:sys.installationDir}/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=${installer:sys.installationDir}/libjhdf5.so" \
    -jar "${installer:sys.installationDir}/bin/snap-launcher.jar" "$@"

exit $?
