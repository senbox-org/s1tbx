@echo off

IF ["%NEST_HOME%"]==[] echo "NEST_HOME is not defined. Please set NEST_HOME=nest_installation_folder"

IF [%NEST_HOME:~-1%]==[/] set NEST_HOME=%NEST_HOME:~0,-1%
IF [%NEST_HOME:~-1%]==[\] set NEST_HOME=%NEST_HOME:~0,-1%

%comspec% /k gpt.bat -h

