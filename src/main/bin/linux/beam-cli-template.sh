#! /bin/sh

export BEAM4_HOME=${installer:sys.installationDir}

if [ -z "$BEAM4_HOME" ]; then
    echo
    echo Error: BEAM4_HOME not found in your environment.
    echo Please set the BEAM4_HOME variable in your environment to match the
    echo location of the BEAM 4.x installation
    echo
    exit 2
fi

. "$BEAM4_HOME/bin/detect_java.sh"

"$app_java_home/bin/java" \
    -Xmx${installer:maxHeapSize} \
    -Dceres.context=s3tbx \
    "-Ds3tbx.mainClass=${s3tbx.mainClass}" \
    "-Ds3tbx.processorClass=${s3tbx.processorClass}" \
    "-Ds3tbx.home=$BEAM4_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$BEAM4_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$BEAM4_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf5.so" \
    -jar "$BEAM4_HOME/bin/snap-launcher.jar" "$@"

exit $?
