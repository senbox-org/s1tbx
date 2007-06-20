#! /bin/sh

export BEAM4_HOME=$USER_INSTALL_DIR$
export JAVA_HOME=$JAVA_HOME$

if [ -z "$DOLLAR${JAVA_HOME}" ]; then
    echo
    echo Error: JAVA_HOME not found in your environment.
    echo Please set the JAVA_HOME variable in your environment to match the
    echo location of your Java installation
    echo
    exit 1
fi

if [ -z "$DOLLAR${BEAM4_HOME}" ]; then
    echo
    echo Error: BEAM4_HOME not found in your environment.
    echo Please set the BEAM4_HOME variable in your environment to match the
    echo location of the BEAM 4.x installation
    echo
    exit 2
fi

"$DOLLAR${JAVA_HOME}/bin/java" \
    -Xmx1024M \
    -Dceres.context=beam \
    "-Dbeam.mainClass=${beam.mainClass}" \
    "-Dbeam.processorClass=${beam.processorClass}" \
    "-Dbeam.home=$DOLLAR${BEAM4_HOME}" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$DOLLAR${BEAM4_HOME}/modules/lib-hdf-2.3/lib/linux/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$DOLLAR${BEAM4_HOME}/modules/lib-hdf-2.3/lib/linux/libjhdf5.so" \
    -jar "$DOLLAR${BEAM4_HOME}/bin/ceres-launcher.jar" "$DOLLAR$@"

exit 0
