#! /bin/sh
echo Starting Sentinel Application Platform...

if [ -z "$SNAP_HOME" ]; then
    export SNAP_HOME=$PWD
fi

$SNAP_HOME/jre/bin/java \
	-server -Xms512M -Xmx3000M -XX:PermSize=512m -XX:MaxPermSize=512m -Xverify:none \
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods \
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache \
    -Dceres.context=snap \
	"-Ds1tbx.home=$SNAP_HOME" \
    "-Ds1tbx.debug=false" \
    "-Djava.library.path=$PATH:$SNAP_HOME" \
	"-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$SNAP_HOME/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$SNAP_HOME/libjhdf5.so" \
    -jar $SNAP_HOME/bin/ceres-launcher.jar

exit 0


