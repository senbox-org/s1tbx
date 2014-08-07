#! /bin/sh

$NEST_HOME/jre/bin/java \
	-Xmx1024M \
	-Dceres.context=s1tbx \
	"-Ds1tbx.mainClass-org.esa.beam.framework.gpf.main.Main" \
	-Ds1tbx.home="$NEST_HOME" \
	-Djava.library.path="$PATH:$NEST_HOME" \
	"-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$NEST_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf.so" \
	"-Dncsa.hdf.hdf5lib.H5.hdf5lib=$NEST_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf5.so" \
	-jar "$NEST_HOME/bin/ceres-launcher.jar" -d "$@" 

exit 0