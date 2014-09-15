@echo off

set S3TBX_HOME=${installer:sys.installationDir}

echo.
@echo Welcome to the S3TBX command-line interface!
@echo The following command-line tools are available:
@echo   gpt.bat            - General Graph Processing Tool
@echo   pconvert.bat       - General product conversion and quicklook generation
@echo   visat-d.bat        - VISAT application launcher for debugging
@echo Typing the name of the tool will output its usage information.
echo.

cd "%S3TBX_HOME%\bin"

prompt $G$S
