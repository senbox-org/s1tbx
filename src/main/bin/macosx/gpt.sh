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

java \
    -Xmx${installer:maxHeapSize} \
    -Dceres.context=beam \
    "-Dbeam.mainClass=org.esa.beam.framework.gpf.main.GPT" \
    "-Dbeam.home=$BEAM4_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$BEAM4_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf.jnilib" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$BEAM4_HOME/modules/lib-hdf-${hdf.version}/lib/libjhdf5.jnilib" \
    -jar "$BEAM4_HOME/bin/ceres-launcher.jar" "$@"

exit $?
