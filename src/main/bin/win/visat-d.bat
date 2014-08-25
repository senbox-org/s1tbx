@echo off

set S3TBX_HOME=${installer:sys.installationDir}

"%S3TBX_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=s3tbx ^
    -Ds3tbx.debug=true ^
    "-Ds3tbx.home=%S3TBX_HOME%" ^
    -jar "%S3TBX_HOME%\bin\snap-launcher.jar" -d %*

exit /B %ERRORLEVEL%
