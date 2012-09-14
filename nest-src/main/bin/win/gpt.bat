@echo off

IF ["%NEST_HOME%"]==[] echo "NEST_HOME is not defined. Please set NEST_HOME=nest_installation_folder"

IF [%NEST_HOME:~-1%]==[/] set NEST_HOME=%NEST_HOME:~0,-1%
IF [%NEST_HOME:~-1%]==[\] set NEST_HOME=%NEST_HOME:~0,-1%

"%NEST_HOME%\jre\bin\java.exe" ^
    -server -Xms512M -Xmx800M -Xverify:none ^
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio -XX:CompileThreshold=10000 ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache ^
    -Dceres.context=nest ^
    "-Dnest.mainClass=org.esa.beam.framework.gpf.main.Main" ^
    "-Dnest.home=%NEST_HOME%" ^
    "-Dnest.debug=false" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%NEST_HOME%\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%NEST_HOME%\jhdf5.dll" ^
    -jar "%NEST_HOME%\bin\ceres-launcher.jar" %*

exit /B 0
