The SENTINEL-1 Toolbox
======================

Building S1TBX from the source

1. Download and install the required build tools
	* Install J2SE 1.7 JDK and set JAVA_HOME accordingly. 
	* Install Maven and set MAVEN_HOME accordingly. 
2. Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

3. Download the S1TBX source code and unpack into $MY_PROJECTS/s1tbx.
4. CD into $MY_PROJECTS/s1tbx
5. Build S1TBX from source: Type mvn compile or mvn package

6. Open the pom.xml file from within IntelliJ IDEA to import.
7. Use the following configuration to run DAT:

    * Main class: com.bc.ceres.launcher.Launcher
    * VM parameters: -Xmx1024M -Dceres.context=s1tbx
    * Program parameters: none
    * Working directory: $MY_PROJECTS/nest/output
    * Use classpath of module: s1tbx-bootstrap


Enjoy!
