#! /bin/sh

${installer:sys.installationDir}/jre/bin/java \
	-Xmx${installer:maxHeapSize} \
	-Dceres.context=${installer:context} \
	"-D${installer:context}.mainClass-org.esa.snap.framework.gpf.main.Main" \
	-D${installer:context}.home="${installer:sys.installationDir}" \
	-Djava.library.path="$PATH:${installer:sys.installationDir}" \
	"-Dncsa.hdf.hdflib.HDFLibrary.hdflib=${installer:sys.installationDir}/modules/lib-hdf-2.3/lib/macosx/libjhdf.so" \
	"-Dncsa.hdf.hdf5lib.H5.hdf5lib=${installer:sys.installationDir}/modules/lib-hdf-2.3/lib/macosx/libjhdf5.so" \
	-jar "${installer:sys.installationDir}/bin/snap-launcher.jar" -d "$@" 

exit $?
