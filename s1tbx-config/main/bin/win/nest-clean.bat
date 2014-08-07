rem cleans the .nest folder where preferences and temporary files are stored

IF NOT EXIST "%USERPROFILE%\.nest" exit

del /S /F /Q "%USERPROFILE%\.nest"
rmdir /S /Q "%USERPROFILE%\.nest"
