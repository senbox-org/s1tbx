About NEST Version 4C

The Next ESA SAR Toolbox (NEST) is a user friendly open source toolbox for reading, post-processing, analysing and visualising the large archive of data (from Level 1) from ESA SAR missions including ERS-1 & 2, ENVISAT and in the future Sentinel-1. In addition, NEST supports handling of products from third party missions including JERS-1, ALOS PALSAR, TerraSAR-X, Radarsat-2 and Cosmo-Skymed.

NEST helps the remote sensing community by supporting the handling of various SAR products and complimenting existing software packages. NEST has been built using the BEAM Earth Observation Toolbox and Development Platform and it covers the functionality of the older Basic Envisat SAR Toolbox BEST. NEST is currently undergoing development with periodic new releases. The major new functionality in NEST over BEST is an integrated viewer and orthorectification and mosaicking of SAR images.

NEST is extensible with an API that allows users to easily create their own plugin Readers, Processors and Writers. NEST is open source under the GNU General Public License (GNU GPL). If you are interested in contributing by developing a reader or writer for a product please contact us.

NEST is being developed by Array Systems Computing Inc. of Toronto Canada under ESA Contract number 20698/07/I-LG.  InSAR functionalities are being developed by a joint effort of PPO.labs, Delft University of Technology and Array.


Installation
* Download the latest NEST build (www.array.ca/nest/), unzip the binaries files and run the DAT application 

Building NEST from the source

1. Download and install the required build tools
* Install J2SE 1.6 JDK and set JAVA_HOME accordingly. 
* Install Maven and set MAVEN_HOME accordingly. 
2. Add $JAVA_HOME/bin, $MAVEN_HOME/bin to your PATH.

3. Download the NEST source code and unpack into $MY_PROJECTS/nest.
4. Cd into $MY_PROJECTS/nest
5. Build NEST from source: Type mvn compile or mvn package
6. Open project in the your IDE. IntelliJ IDEA users:

    * To build IDEA project files for NEST: Type mvn compile idea:idea
    * In IDEA, go to the IDEA Main Menu/File/Open Project and simply open the created project file $MY_PROJECTS/nest/nest.ipr

7. Open project in the your IDE. Eclipse users:

    * To build Eclipse project files for NEST: Type mvn eclipse:eclipse
    * Make sure that M2_REPO classpath variable is set:

        1. Open Window/Preferences..., then select Java/Build Path/Classpath Variables
        2. Select New... and add variable M2_REPO
        3. Select Folder... and choose the location of your Maven local repository, e.g ~/.m2/repository


    * Click Main Menu/File/Import
    * Select General/Existing Project into Workspace
    * Select Root Directory $MY_PROJECTS/nest
    * Click Finish

9. Use the following configuration to run DAT:

    * Main class: com.bc.ceres.launcher.Launcher
    * VM parameters: -server -Xmx1024M -Dceres.context=nest
    * Program parameters: none
    * Working directory: $MY_PROJECTS/nest/output
    * Use classpath of module: nest-bootstrap

10. To package a distribution
	mvn package assembly:assembly
	note: the corresponding JRE for the platform will be needed in the NEST install folder 
	
Enjoy!