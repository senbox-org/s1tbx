@echo off

set BEAM4_HOME=${installer:sys.installationDir}

"%BEAM4_HOME%\jre\bin\java.exe" ^
    -Xmx${installer:maxHeapSize} ^
    -Dceres.context=beam ^
    -Dbeam.debug=true ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" -d %*

exit /B %ERRORLEVEL%
