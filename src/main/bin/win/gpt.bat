@echo off

set S3TBX_HOME=${installer:sys.installationDir}

"%S3TBX_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=s3tbx ^
    "-Ds3tbx.mainClass=org.esa.beam.framework.gpf.main.GPT" ^
    "-Ds3tbx.home=%S3TBX_HOME%" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%S3TBX_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%S3TBX_HOME%\modules\lib-hdf-${hdf.version}\lib\jhdf5.dll" ^
    -jar "%S3TBX_HOME%\bin\snap-launcher.jar" %*

exit /B %ERRORLEVEL%
