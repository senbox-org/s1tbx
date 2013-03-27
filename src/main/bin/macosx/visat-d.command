#! /bin/sh

export BEAM4_HOME=${installer:sys.installationDir}

if [ -z "$BEAM4_HOME" ]; then
    echo
    echo Error: BEAM4_HOME not found in your environment.
    echo Please set the BEAM4_HOME variable in your environment to match the
    echo location of the BEAM 4.x installation.
    echo
    exit 2
fi

java \
    -Xmx${installer:maxHeapSize} \
    -Dceres.context=beam \
    -Dbeam.debug=true \
    "-Dbeam.home=$BEAM4_HOME" \
    -jar "$BEAM4_HOME/bin/ceres-launcher.jar" -d "$@"

exit $?


