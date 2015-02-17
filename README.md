# SeNtinel Application Platform

SNAP is the common software platform for the three Sentinel Toolboxes which are developed 
by the European Space Agency (ESA) for the scientific exploitation 
of the Sentinel-1, Sentinel-2 and Sentinel-3 missions.

The intention of the development is to primarily support the Sentinel missions for not
only research but also in an operational context. In addition, a great deal of the 
development will be to improve and produce a common application platform for all 
toolboxes for the next 10 years.

ESA is developing three new free open source toolboxes for the scientific exploitation 
of the Sentinel-1, Sentinel-2 and Sentinel-3 missions.

The Toolboxes will be based on the proven BEAM/NEST architecture inheriting
all current BEAM and NEST functionality including multi-mission support for
SAR and optical missions and provide an evolutionary update to the architecture
to support ESA and third party missions for years to come.
In practical terms this means that all three toolboxes will be based on the extension 
of the existing open source BEAM and NEST toolbox framework. We will be restructuring 
all the code from BEAM and NEST into this common Sentinel framework. The common 
platform will be jointly developed by Array (S-1), C-S (S-2) and Brockmann Consult (S-3). 
The project will run through the next two years with releases every four months. The 
first public release of the software will be available in September 2014 and be 
released under GNU GPL open source license.

This work is funded through the *Scientific Exploitation of Operational Missions* (SEOM)
a new program element of ESA's fourth period of the Earth Observation Envelope Programme 
(2013-2017), referred to as EOEP-4.

EOEP-4 will pursue a long-term exploitation strategy and address the broad and growing 
spectrum of EO user communities, including science, applications and services development.

# Building toolboxes from the source

## Checkout
Clone the source code and related repositories into a folder _SNAP/_

	git clone https://github.com/senbox-org/snap-engine.git
	
	git clone https://github.com/senbox-org/snap-desktop.git

After checking out the source code from the repositories the further steps differ depending
on either if you want to use the new NetBeans based runtime or if you still want to use the old ceres runtime based configuration.
Read further at the corresponding section

:exclamation:Note: the snap-engine and snap-desktop repositories are mandatory. The toolboxes are each optional and
currently only the Sentinel-3 Toolbox is currently configured to be used with SNAP.

	git clone https://github.com/senbox-org/s1tbx.git

	git clone https://github.com/senbox-org/s2tbx.git

	git clone https://github.com/senbox-org/s3tbx.git


## Build with NetBeans
1. CD into SNAP/snap-engine:

   mvn install

2. CD into SNAP/snap-desktop:

   mvn install

3. CD into SNAP/snap-desktop/snap-application:

   mvn nbm:cluster-app

4. Start the application via Maven:

   mvn nbm:run-platform

It is also possible to do step 3 and 4 at once:

   mvn nbm:cluster-app nbm:run-platform

### IDE Setup (IntelliJ IDEA)
1. Create an empty project with the _SNAP/_ directory as project directory

2. Import the pom.xml files of snap-engine and snap-desktop as modules. Ensure **not** to enable
the option *'Create module groups for multi-module Maven projects'*. It would create an odd project structure but would still work. Everything can be default values.

3. Set the used SDK for the main project. A JDK 1.8 or later is needed.

4. Use the following configuration to run SNAP in the IDE:
	* **Main class:** org.esa.snap.nbexec.Launcher
	* **VM parameters:** -Dsun.awt.nopixfmt=true -Dsun.java2d.noddraw=true -Dsun.java2d.dpiaware=false
	All VM parameters are optional
	* **Working directory:** SNAP/snap-desktop/snap-application/target/snap/
	* **Use classpath of module:** nbexec

### Run SNAP with a Toolbox
The Sentinel-3 Toolbox is configured to build all the modules into the cluster directory *´${project.basedir}/../target/nbmCluster´*
This cluster can be set as parameter to the above mentioned launcher.
Specify as program parameter e.g. *´--clusters "SNAP\s3tbx\target\nbmCluster\netbeans\s3tbx"´*


Enjoy!
