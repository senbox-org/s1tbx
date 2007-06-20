@echo off

set BEAM4_HOME=$USER_INSTALL_DIR$
set JAVA_HOME=$JAVA_HOME$

if not "%BEAM4_HOME%" == "" goto OkBeamHome
set BEAM4_HOME=$USER_INSTALL_DIR$
:OkBeamHome

if not "%JAVA_HOME%" == "" goto OkJavaHome
JAVA_HOME=$JAVA_HOME$
:OkJavaHome

echo.
@echo Welcome to the BEAM command-line interface!
@echo The following command-line tools are available:
@echo   pconvert.bat       - General product conversion and quicklook generation
@echo   mapproj.bat        - General map projections
@echo   binning.bat        - General level 3 binning processor
@echo   mosaic.bat         - General level 3 mosaicing processor
@echo   flhmci.bat         - General FLH / MCI processor
@echo   meris-rad2refl.bat - Envisat/MERIS radiiance to reflectance processor
@echo   meris-smac.bat     - Envisat/MERIS atmospheric correction (SMAC)
@echo   meris-smac.bat     - Envisat/MERIS radiance to reflectance processor
@echo   aatsr-sst.bat      - Envisat/AATSR sea surface temperaure processor
@echo   visat-d.bat        - VISAT application launcher for debugging
@echo Typing the name of the tool will output its usage information.
echo.

prompt $DOLLAR$G$DOLLAR$S
