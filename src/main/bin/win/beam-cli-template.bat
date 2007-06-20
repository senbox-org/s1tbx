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

if not "%BEAM4_HOME%"=="" goto OkBeamHome
@echo.
@echo Error: BEAM4_HOME not found in your environment.
@echo Please set the BEAM4_HOME variable in your environment to match the
@echo location of the BEAM 4.x installation
@echo.
exit /B 2
:OkBeamHome

set OLD_PATH=%PATH%
set PATH=%PATH%;%BEAM4_HOME%\bin

"%JAVA_HOME%\bin\java.exe" ^
    -Xmx1024M ^
    -Dceres.context=beam ^
    "-Dbeam.mainClass=${beam.mainClass}" ^
    "-Dbeam.processorClass=${beam.processorClass}" ^
    "-Dbeam.home=%BEAM4_HOME%" ^
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=%BEAM4_HOME%\modules\lib-hdf-2.3\lib\win\jhdf.dll" ^
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=%BEAM4_HOME%\modules\lib-hdf-2.3\lib\win\jhdf5.dll" ^
    -jar "%BEAM4_HOME%\bin\ceres-launcher.jar" %*

set PATH=%OLD_PATH%

exit /B 0
