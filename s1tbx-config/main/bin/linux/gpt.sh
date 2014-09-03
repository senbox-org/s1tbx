#! /bin/sh

if [ -z "$S1TBX_HOME" ]; then
    export S1TBX_HOME=$PWD
fi

$S1TBX_HOME/jre/bin/java \
    -server -Xms512M -Xmx800M -Xverify:none \
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods \
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache \
    -Dceres.context=s1tbx \
    "-Ds1tbx.mainClass=org.esa.beam.framework.gpf.main.GPT" \
    "-Ds1tbx.home=$S1TBX_HOME" \
	"-Ds1tbx.debug=false" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$S1TBX_HOME/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$S1TBX_HOME/libjhdf5.so" \
    -jar $S1TBX_HOME/bin/snap-launcher.jar "$@"

exit 0
