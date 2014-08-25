#! /bin/sh

$S1TBX_HOME/jre/bin/java \
	-Xmx1024M \
	-Dceres.context=s1tbx \
	-Ds1tbx.home="$S1TBX_HOME" \
	-Djava.library.path="$PATH:$S1TBX_HOME" \
	-jar "$S1TBX_HOME/bin/snap-launcher.jar" -d "$@" 

exit $?