Project name: SNAP ICEYE reader
Type: Plugin  
Main contact: ahmad.hamouda (at) iceye.fi  

* Introduction

This plugin is used inside snap desktop application to map our image format to snap format.  
This plugin build as per snap development extension module documentation "https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879037/How+to+develop+an+extension+module".

* Compilation & dependencies

In order to compile and execute the current code for development  
purposes [debug], execute:

    - Change TESTING_IMAGE_PATH variable at TestIceyeReader
    - Set application enviroments as per "https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879037/How+to+develop+an+extension+module"
    - Run test case in debug mode

In order to attach code for debugger mode:

    - Create Jar configuration with the following parameters: 
        - Jar path:	<your snap desktop installation directory>/snap/modules/ext/org.esa.snap.snap-rcp/org-esa-snap/snap-main.jar
        - VM options:	-Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false -Dorg.netbeans.level=INFO -Xmx8G
        - Program arguments:	--userdir "<your .snap path usualy at home>/.snap/system"
        - Working directory:	<your snap desktop installation directory>
        - JRE:	1.8

* Unit Testing

To run unit tests, execute:

    - change TESTING_IMAGE_PATH variable at TestIceyeReader

    mvn clean install

To build without testing

    mvn clean install -DskipTests=true;
    
* Integration Testing

    - Download SNAP desktop version 6.0.0 "http://step.esa.int/main/download/"
    - You can update the application if you want
    - Add plugin:
        - Tools -> Plugins -> Downloaded -> add plugin -> slect pulgin from your target file [.pem]
    - Open image:
        - File -> Import -> SAR sensors -> Iceye-Product
        
        
* Useful urls
    - Snap git hub:https://github.com/senbox-org/s1tbx

