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

1. Clone the source code and related repositories into a folder SNAP/

	git clone https://github.com/senbox-org/snap.git
	
	git clone https://github.com/senbox-org/beam.git
	
	git clone https://github.com/senbox-org/ceres.git
	
	git clone https://github.com/arraydev/s1tbx.git
	
	git clone https://github.com/CS-SI/s2tbx.git
	
	git clone https://github.com/bcdev/s3tbx.git
	
	Note: the snap, beam and ceres repositories are mandetory. The toolboxes are each optional.
2. CD into SNAP/snap and build the source: 

	mvn compile or mvn package
	
	Use maven profiles to build the desired toolbox components
	
	mvn compile -P s2tbx,s3tbx
	
	or all components
	
	mvn compile -P all
	
3. Open the pom.xml file from within IntelliJ IDEA to import.
4. Use the following configuration to run DAT:

    * Main class: com.bc.ceres.launcher.Launcher
    * VM parameters: -Xmx4G -Dceres.context=snap
    * Program parameters: none
    * Working directory: SNAP/output
    * Use classpath of module: snap-bootstrap


Enjoy!
