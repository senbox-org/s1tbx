#! /bin/sh

set S1TBX_HOME=${installer:sys.installationDir}

$S1TBX_HOME/jre/bin/java \
	-Xmx${installer:maxHeapSize} \
	-Dceres.context=${installer:context} \
	-D${installer:context}.home="$S1TBX_HOME" \
	-Djava.library.path="$PATH:$S1TBX_HOME" \
	-jar "$S1TBX_HOME/bin/snap-launcher.jar" -d "$@" 

exit $?
