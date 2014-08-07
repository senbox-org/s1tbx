#! /bin/sh

$NEST_HOME/jre/bin/java \
	-Xmx1024M \
	-Dceres.context=s1tbx \
	-Ds1tbx.home="$NEST_HOME" \
	-Djava.library.path="$PATH:$NEST_HOME" \
	-jar "$NEST_HOME/bin/ceres-launcher.jar" -d "$@" 

exit $?