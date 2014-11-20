@echo off

IF ["%S1TBX_HOME%"]==[] echo "S1TBX_HOME is not defined. Please set S1TBX_HOME=installation_folder"

IF [%S1TBX_HOME:~-1%]==[/] set S1TBX_HOME=%S1TBX_HOME:~0,-1%
IF [%S1TBX_HOME:~-1%]==[\] set S1TBX_HOME=%S1TBX_HOME:~0,-1%

%comspec% /k gpt.bat -h

