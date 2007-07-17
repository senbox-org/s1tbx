@ECHO OFF

SET BEAM_HOME=..
SET JAVA_EXE=%JAVA_HOME%\jre\bin\java

::------------------------------------------------------------------
:: You can adjust the Java maximum heap space here.
:: Just change the Xmx option. Space is given in megabyte.
::    '-Xmx512M' sets the maximum heap space to 512 megabytes
:: If you want to get debugging messages out of VISAT,
:: append "-d" or "--debug" to the end of the following line.
::------------------------------------------------------------------
CALL "%JAVA_EXE%" -Xmx1024M "-Dprocessor.home=%BEAM_HOME%" -Dprocessor.app=ProcessorMain  -Dprocessor.consoleLog=true -jar "%BEAM_HOME%\modules\ceres-1.0-SNAPSHOT.jar" processor %1 %2 %3 %4 %5 %6 %7 %8 %9
