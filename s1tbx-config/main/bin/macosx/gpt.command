#! /bin/sh

set S1TBX_HOME=${installer:sys.installationDir}

$S1TBX_HOME/jre/bin/java \
	-Xmx${installer:maxHeapSize} \
	-Dceres.context=s1tbx \
	"-Ds1tbx.mainClass-org.esa.beam.framework.gpf.main.Main" \
	-Ds1tbx.home="$S1TBX_HOME" \
	-Djava.library.path="$PATH:$S1TBX_HOME" \
	"-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$S1TBX_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf.so" \
	"-Dncsa.hdf.hdf5lib.H5.hdf5lib=$S1TBX_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf5.so" \
	-jar "$S1TBX_HOME/bin/snap-launcher.jar" -d "$@" 

exit 0