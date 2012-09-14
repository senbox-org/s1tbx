
rem IF NOT EXIST "%USERPROFILE%\.nest" exit
rem del /S /F /Q "%USERPROFILE%\.nest"
rem rmdir /S /Q "%USERPROFILE%\.nest"

IF NOT EXIST "%NEST_HOME%" exit
del /S /F /Q "%NEST_HOME%"
rmdir /S /Q "%NEST_HOME%"
