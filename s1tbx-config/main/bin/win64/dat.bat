@echo off

set S1TBX_HOME=%CD%

IF ["%S1TBX_HOME%"]==[] echo "S1TBX_HOME is not defined. Please set S1TBX_HOME=installation_folder"

IF [%S1TBX_HOME:~-1%]==[/] set S1TBX_HOME=%S1TBX_HOME:~0,-1%
IF [%S1TBX_HOME:~-1%]==[\] set S1TBX_HOME=%S1TBX_HOME:~0,-1%

"%S1TBX_HOME%\jre\bin\java.exe" ^
	-server -Xms512M -Xmx3000M -XX:PermSize=512m -XX:MaxPermSize=512m -Xverify:none ^
    -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -XX:CompileThreshold=10000 ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate -XX:+UseStringCache ^
    -Dceres.context=s1tbx ^
    -Ds1tbx.debug=true ^
    -Ds1tbx.consoleLog=true ^
    -Ds1tbx.logLevel=INFO ^
    "-Dnest.home=%S1TBX_HOME%" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" -d %*

exit /B 0
