@echo off

set BEAM4_HOME=${installer:sys.installationDir}

"${installer:sys.installationDir}\jre\bin\java.exe" ^
    -Xmx1024M ^
    -Dceres.context=beam ^
    -Dceres.debug=true ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" -d %*

exit /B 0
