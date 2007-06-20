#!/bin/bash

#--------------------------------------
# Adopt the beam installation directory
#--------------------------------------
BEAM_HOME=.....
export BEAM_HOME

HDF4_LIB=$BEAM_HOME/bin/libjhdf.so
export HDF4_LIB

HDF5_LIB=$BEAM_HOME/bin/libjhdf5.so
export HDF5_LIB

JAVA_EXE=$BEAM_HOME/jre/bin/java
export JAVA_EXE

CLASSPATH=$BEAM_HOME/extensions/ndviprocessor.jar:$BEAM_HOME/lib/beam.jar:$BEAM_HOME/lib/clibwrapper_jiio.jar:$BEAM_HOME/lib/crimson.jar:$BEAM_HOME/lib/jai_codec.jar:$BEAM_HOME/lib/jai_core.jar:$BEAM_HOME/lib/jai_imageio.jar:$BEAM_HOME/lib/jaxp.jar:$BEAM_HOME/lib/jdom.jar:$BEAM_HOME/lib/jh.jar:$BEAM_HOME/lib/jhdf.jar$BEAM_HOME/lib/jhdf5.jar:$BEAM_HOME/lib/mlibwrapper_jai.jar:
export CLASSPATH

#------------------------------------------------------------------
# You can adjust the Java minimum and maximum heap space here.
# Just change the Xms and Xmx options. Space is given in megabyte.
#    '-Xms64M' sets the minimum heap space to 64 megabytes
#    '-Xmx512M' sets the maximum heap space to 512 megabytes
# If you want to get debugging messages out of Water-Processor,
# append "--debug" to the end of the following line.
#------------------------------------------------------------------

$JAVA_EXE -Xms64M -Xmx512M \
   -classpath $CLASSPATH \
   -Dbeam.home=$BEAM_HOME \
   -Dncsa.hdf.hdflib.HDFLibrary.hdflib=$HDF4_LIB \
   -Dncsa.hdf.hdf5lib.H5.hdf5lib=$HDF5_LIB \
    com.bc.beam.processor.ndvi.NdviProcessorMain "$@"

