@echo off

set BEAM4_HOME=$USER_INSTALL_DIR$
set JAVA_HOME=$JAVA_HOME$

if not "%JAVA_HOME%" == "" goto OkJavaHome
@echo.
@echo Error: JAVA_HOME not found in your environment.
@echo Please set the JAVA_HOME variable in your environment to match the
@echo location of your Java installation
@echo.
exit /B 1
:OkJavaHome

if not "%BEAM4_HOME%" == "" goto OkBeamHome
@echo.
@echo Error: BEAM4_HOME not found in your environment.
@echo Please set the BEAM4_HOME variable in your environment to match the
@echo location of the BEAM 4.x installation
@echo.
exit /B 2
:OkBeamHome

set OLD_PATH=%PATH%
set PATH=%PATH%;%BEAM4_HOME%\bin

call "%JAVA_HOME%\bin\java.exe" ^
    -Xmx1024M ^
    "-splash:%BEAM4_HOME%\bin\splash.png" ^
    -Dceres.context=beam ^
    -Dceres.debug=true ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" -d %*

set PATH=%OLD_PATH%

exit /B 0
