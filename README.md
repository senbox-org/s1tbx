The SENTINEL-1 Toolbox
======================

Building S1TBX from the source

1. Download and install the required build tools
	* Install J2SE 1.8 JDK and set JAVA_HOME accordingly. 
	* Install Maven and set MAVEN_HOME accordingly. 
	* Install git
2. Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

3. Clone the S1TBX source code and related repositories into SNAP/

	git clone https://github.com/senbox-org/s1tbx.git
	
    git clone https://github.com/senbox-org/snap-desktop.git
    
	git clone https://github.com/senbox-org/snap-engine.git
    
    git clone https://github.com/senbox-org/snap-installer.git
	
4. CD into SNAP/snap-engine:

   mvn install

5. CD into SNAP/snap-desktop:

   mvn install

6. CD into SNAP/s1tbx:

   mvn install
	
Setting up IntelliJ IDEA

1. Create an empty project with the SNAP/ directory as project directory

2. Import the pom.xml files of snap-engine, snap-desktop and s1tbx as modules. Ensure **not** to enable
the option *'Create module groups for multi-module Maven projects'*. Everything can be default values.

3. Set the used SDK for the main project. A JDK 1.8 or later is needed.

4. Use the following configuration to run SNAP in the IDE:
	* **Main class:** org.esa.snap.nbexec.Launcher
	* **VM parameters:** -Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false
	All VM parameters are optional
    * **Program arguments:** 
    --clusters
    "E:\build\SNAP\s1tbx\target\nbm\netbeans\s1tbx";"E:\build\SNAP\s1tbx\rstb\target\nbm\netbeans\rstb"
    --patches
    "E:\build\SNAP\snap-engine\$\target\classes";"E:\build\SNAP\s1tbx\$\target\classes"
    
	* **Working directory:** SNAP/snap-desktop/snap-application/target/snap/
	* **Use classpath of module:** nbexec

Enjoy!
