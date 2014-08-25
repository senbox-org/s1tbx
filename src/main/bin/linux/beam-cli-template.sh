#! /bin/sh

export S3TBX_HOME=${installer:sys.installationDir}

if [ -z "$S3TBX_HOME" ]; then
    echo
    echo Error: S3TBX_HOME not found in your environment.
    echo Please set the S3TBX_HOME variable in your environment to match the
    echo location of the BEAM 4.x installation
    echo
    exit 2
fi

. "$S3TBX_HOME/bin/detect_java.sh"

"$app_java_home/bin/java" \
    -Xmx${installer:maxHeapSize} \
    -Dceres.context=s3tbx \
    "-Ds3tbx.mainClass=${s3tbx.mainClass}" \
    "-Ds3tbx.processorClass=${s3tbx.processorClass}" \
    "-Ds3tbx.home=$S3TBX_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$S3TBX_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$S3TBX_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf5.so" \
    -jar "$S3TBX_HOME/bin/snap-launcher.jar" "$@"

exit $?
