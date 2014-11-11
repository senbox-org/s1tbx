@ECHO OFF
::------------------------------------------------------------------
::  Please adapt according to your system
::------------------------------------------------------------------
SET SOFTWAREDIR=C:\Users\bettina\Development\projects\waqss\software\ceres-metadata

SET CLASSPATH=%SOFTWAREDIR%;%SOFTWAREDIR%\lib\*

CALL java -cp "%CLASSPATH%" com.bc.ceres.standalone.MetadataEngineMain %1 %2 %3 %4 %5 %6 %7 %8 %9

