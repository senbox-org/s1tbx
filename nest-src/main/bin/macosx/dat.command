#! /bin/sh

if [ -z "$NEST_HOME" ]; then
	export NEST_HOME=@@INSTALL_DIR@@
fi

java \
    -Xmx1024M -XX:CompileThreshold=10000 -Xverify:none -XX:+UseParallelGC \
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio \
    -Dceres.context=nest \
    -Dnest.debug=false \
    "-Dnest.home=$NEST_HOME" \
	"-Djava.library.path=$PATH:$NEST_HOME" \
    -jar "$NEST_HOME/bin/ceres-launcher.jar" "$@"

exit 0


