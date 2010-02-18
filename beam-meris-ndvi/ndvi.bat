@ECHO OFF

::---------------------------------------
:: Adopt the beam installation directory
::---------------------------------------
SET BEAM_HOME=...

SET HDF4_LIB=$BEAM_HOME$\bin\jhdf.dll
SET HDF5_LIB=$BEAM_HOME$\bin\jhdf5.dll
SET JAVA_EXE=$BEAM_HOME$\jre\bin\java
SET LIB_DIR=$BEAM_HOME$\lib
SET OLD_CLASSPATH=%CLASSPATH%
SET CLASSPATH=$BEAM_HOME$\extensions\ndviprocessor.jar;$LIB_DIR$\beam.jar;$LIB_DIR$\clibwrapper_jiio.jar;$LIB_DIR$\crimson.jar;$LIB_DIR$\jai_codec.jar;$LIB_DIR$\jai_core.jar;$LIB_DIR$\jai_imageio.jar;$lib_dir$\jaxp.jar;$LIB_DIR$\jdom.jar;$LIB_DIR$\jh.jar;$LIB_DIR$\jhdf.jar$LIB_DIR$\jhdf5.jar;$LIB_DIR$\mlibwrapper_jai.jar;

::------------------------------------------------------------------
:: You can adjust the Java minimum and maximum heap space here.
:: Just change the Xms and Xmx options. Space is given in megabyte.
::    '-Xms64M' sets the minimum heap space to 64 megabytes
::    '-Xmx512M' sets the maximum heap space to 512 megabytes
:: If you want to get debugging messages out of FLH-MCI-Processor,
:: append "--debug" to the end of the following line.
::------------------------------------------------------------------

CALL "%JAVA_EXE%" -Xms64M -Xmx512M -classpath "%CLASSPATH%" "-Dbeam.home=%BEAM_HOME%" "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%HDF4_LIB%" "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%HDF5_LIB%"  com.bc.beam.processor.ndvi.NdviProcessorMain %1 %2 %3 %4 %5 %6 %7 %8 %9

SET CLASSPATH=%OLD_CLASSPATH%


