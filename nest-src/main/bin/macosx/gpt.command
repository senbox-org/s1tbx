#! /bin/sh

if [ -z "$NEST_HOME" ]; then
	export NEST_HOME=@@INSTALL_DIR@@
fi

java \
    -Xmx1024M -XX:CompileThreshold=10000 -Xverify:none -XX:+UseParallelGC \
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio \
    -Dceres.context=nest \
    "-Dnest.mainClass=org.esa.beam.framework.gpf.main.Main" \
    "-Dnest.home=$NEST_HOME" \
	"-Dnest.debug=false" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$NEST_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$NEST_HOME/modules/lib-hdf-2.3/lib/macosx/libjhdf5.so" \
    -jar "$NEST_HOME/bin/ceres-launcher.jar" "$@"

exit 0
