The SENTINEL-1 Toolbox
======================

Building S1TBX from the source

1. Download and install the required build tools
	* Install J2SE 1.7 JDK and set JAVA_HOME accordingly. 
	* Install Maven and set MAVEN_HOME accordingly. 
	* Install git
2. Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

3. Clone the S1TBX source code and related repositories into MY_PROJECTS/
	git clone https://github.com/arraydev/s1tbx.git
	git clone https://github.com/arraydev/beam.git
	git clone https://github.com/bcdev/ceres.git
4. Check out the nestmod branch in the beam repository
	CD into MY_PROJECTS/beam
	git checkout nestmod
5. CD into MY_PROJECTS/s1tbx and build S1TBX from source: 
	mvn compile or mvn package
6. Open the pom.xml file from within IntelliJ IDEA to import.
7. Use the following configuration to run DAT:

    * Main class: com.bc.ceres.launcher.Launcher
    * VM parameters: -Xmx4G -Dceres.context=s1tbx
    * Program parameters: none
    * Working directory: MY_PROJECTS/s1tbx/output
    * Use classpath of module: s1tbx-bootstrap


Enjoy!
