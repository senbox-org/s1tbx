@echo off

set S1TBX_HOME=${installer:sys.installationDir}

"%S1TBX_HOME%\jre\bin\java.exe" ^
    -Xms512M -Xmx${installer:maxHeapSize} ^
    -Xverify:none -XX:+AggressiveOpts -XX:+UseFastAccessorMethods ^
    -XX:+UseParallelGC -XX:+UseNUMA -XX:+UseLoopPredicate ^
    "-Dceres.context=${installer:context}" ^
    "-D${installer:context}.debug=true" ^
    "-D${installer:context}.consoleLog=true" ^
    "-D${installer:context}.logLevel=INFO" ^
    "-D${installer:context}.home=%S1TBX_HOME%" ^
    -jar "%S1TBX_HOME%\bin\snap-launcher.jar" -d %*

exit /B 0
