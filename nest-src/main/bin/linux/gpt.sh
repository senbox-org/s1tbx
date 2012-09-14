#! /bin/sh

if [ -z "$NEST_HOME" ]; then
    export NEST_HOME=$PWD
fi

$NEST_HOME/jre/bin/java \
    -server -Xms512M -Xmx800M -Xverify:none \
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio -XX:CompileThreshold=10000 \
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache \
    -Dceres.context=nest \
    "-Dnest.mainClass=org.esa.beam.framework.gpf.main.Main" \
    "-Dnest.home=$NEST_HOME" \
	"-Dnest.debug=false" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$NEST_HOME/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$NEST_HOME/libjhdf5.so" \
    -jar $NEST_HOME/bin/ceres-launcher.jar "$@"

exit 0
