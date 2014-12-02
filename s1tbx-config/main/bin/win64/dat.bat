@echo off

set S1TBX_HOME=${installer:sys.installationDir}

"%S1TBX_HOME%\jre\bin\java.exe" ^
	-Xmx${installer:maxHeapSize} ^
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate ^
    "-Dceres.context=s1tbx" ^
    "-Ds1tbx.debug=true" ^
    "-Ds1tbx.consoleLog=true" ^
    "-Ds1tbx.logLevel=INFO" ^
    "-Ds1tbx.home=%S1TBX_HOME%" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" -d %*

exit /B 0
