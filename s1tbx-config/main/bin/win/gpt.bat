@echo off

set S1TBX_HOME=${installer:sys.installationDir}

"%S1TBX_HOME%\jre\bin\java.exe" ^
    -Xms512M -Xmx${installer:maxHeapSize} ^
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods ^
    -XX:+UseParallelGC -XX:+UseNUMA ^
    -Dceres.context=${installer:context} ^
    "-D${installer:context}.mainClass=org.esa.beam.framework.gpf.main.GPT" ^
    "-D${installer:context}.home=%S1TBX_HOME%" ^
    "-D${installer:context}.debug=false" ^
	"-D${installer:context}.consoleLog=false" ^
	"-D${installer:context}.logLevel=WARNING" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%S1TBX_HOME%\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%S1TBX_HOME%\jhdf5.dll" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" %*

exit /B 0
