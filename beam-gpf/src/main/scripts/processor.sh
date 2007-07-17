#!/bin/bash


export BEAM_HOME=..

export JAVA_EXE=$JAVA_HOME/bin/java



#------------------------------------------------------------------
# You can adjust the Java minimum and maximum heap space here.
# Just change the Xms and Xmx options. Space is given in megabyte.
#    '-Xms64M' sets the minimum heap space to 64 megabytes
#    '-Xmx512M' sets the maximum heap space to 512 megabytes
# If you want to get debugging messages out of VISAT,
# append "--debug" to the end of the following line.
#------------------------------------------------------------------

$JAVA_EXE -Xms64M -Xmx1024M \
   -Dprocessor.home=$BEAM_HOME \
   -Dprocessor.app=ProcessorMain  \
   -Dprocessor.consoleLog=true\
   -jar $BEAM_HOME/modules/ceres-1.0-SNAPSHOT.jar \
   processor "$@"

