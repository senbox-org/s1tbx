@echo off

IF ["%S1TBX_HOME%"]==[] echo "S1TBX_HOME is not defined. Please set S1TBX_HOME=installation_folder"

IF [%S1TBX_HOME:~-1%]==[/] set S1TBX_HOME=%S1TBX_HOME:~0,-1%
IF [%S1TBX_HOME:~-1%]==[\] set S1TBX_HOME=%S1TBX_HOME:~0,-1%

"%S1TBX_HOME%\jre\bin\java.exe" ^
    -server -Xms512M -Xmx800M -Xverify:none ^
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xconcurrentio -XX:CompileThreshold=10000 ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache ^
    -Dceres.context=s1tbx ^
    "-Ds1tbx.mainClass=org.esa.beam.framework.gpf.main.Main" ^
    "-Ds1tbx.home=%S1TBX_HOME%" ^
    "-Ds1tbx.debug=false" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%S1TBX_HOME%\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%S1TBX_HOME%\jhdf5.dll" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" %*

exit /B 0
