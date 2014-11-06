@echo off

IF ["%SNAP_HOME%"]==[] echo "SNAP_HOME is not defined. Please set SNAP_HOME=snap_installation_folder"

IF [%SNAP_HOME:~-1%]==[/] set SNAP_HOME=%SNAP_HOME:~0,-1%
IF [%SNAP_HOME:~-1%]==[\] set SNAP_HOME=%SNAP_HOME:~0,-1%

"%SNAP_HOME%\jre\bin\java.exe" ^
    -server -Xms512M -Xmx800M -Xverify:none ^
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio -XX:CompileThreshold=10000 ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache ^
    -Dceres.context=snap ^
    "-Ds1tbx.mainClass=org.esa.beam.framework.gpf.main.Main" ^
    "-Ds1tbx.home=%SNAP_HOME%" ^
    "-Ds1tbx.debug=false" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%SNAP_HOME%\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%SNAP_HOME%\jhdf5.dll" ^
    -jar "%SNAP_HOME%\bin\ceres-launcher.jar" %*

exit /B 0
