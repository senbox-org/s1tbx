The SENTINEL-1 Toolbox
======================

Building S1TBX from the source

1. Download and install the required build tools
	* Install J2SE 1.8 JDK and set JAVA_HOME accordingly. 
	* Install Maven and set MAVEN_HOME accordingly. 
	* Install git
2. Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

3. Clone the S1TBX source code and related repositories into MY_PROJECTS/

	git clone https://github.com/senbox-org/s1tbx.git
	
	git clone https://github.com/arraydev/snap-engine.git
	
4. For the stable release branch of the software, check out the snap-1.0.x branch in the repositories

	CD into MY_PROJECTS/snap-engine
	
	git checkout snap-1.0.x
	
	CD into MY_PROJECTS/s1tbx
	
	git checkout snap-1.0.x
	
5. CD into MY_PROJECTS/s1tbx and build S1TBX from source: 

	mvn compile -P withbeam
	
6. Open the pom.xml file from within IntelliJ IDEA to import.
7. Activate the maven profile withbeam
8. Use the following configuration to run DAT:

    * Main class: com.bc.ceres.launcher.Launcher
    * VM parameters: -Xmx4G -Dceres.context=s1tbx
    * Program parameters: none
    * Working directory: MY_PROJECTS\output
    * Use classpath of module: s1tbx-bootstrap


Enjoy!
