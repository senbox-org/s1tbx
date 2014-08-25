@echo off

set BEAM4_HOME=${installer:sys.installationDir}

"%BEAM4_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=s3tbx ^
    -Ds3tbx.debug=true ^
    "-Ds3tbx.home=%BEAM4_HOME%" ^
    -jar "%BEAM4_HOME%\bin\snap-launcher.jar" -d %*

exit /B %ERRORLEVEL%
