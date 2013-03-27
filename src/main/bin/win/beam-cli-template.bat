@echo off

set BEAM4_HOME=${installer:sys.installationDir}

"%BEAM4_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=beam ^
    "-Dbeam.mainClass=${beam.mainClass}" ^
    "-Dbeam.processorClass=${beam.processorClass}" ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%BEAM4_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%BEAM4_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf5.dll" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" %*

exit /B %ERRORLEVEL%
