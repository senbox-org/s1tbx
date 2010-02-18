File name:      Readme.txt

Authors:        Norman Fomferra
                Sabine Embacher

Affiliation:    BEAM Development Team
                Brockmann Consult GmbH
                Max-Planck-Strasse 1
                D-21502 Geesthacht, Germany
                www.brockmann-consult.de

E-mails:        norman.fomferra@brockmann-consult.de
                sabine.embacher@brockmann-consult.de

Document title: Brief Documentation of the NDVI Processor
                A plug-in for MERIS/(A)ATSR Toolbox (BEAM)

Release date:	March 24, 2005


Introduction
------------

This file briefly describes the installation and use of the
accompanying software package.

Also this plugin demonstrates how to attach help by a plugin.
Take a look at the section 'Help For Plugins'

This plugin uses MERIS Level-1b TOA radiances of
bands 6 and 10 to retrieve the NDVI:

NDVI = Normalized Difference Vegetation Index

The NDVI algorithms exploit the strength and the vitality of the
vegetation on the earth's surface.

The spectral signatur of wholesome vegetation shows an abrupt rise of
the reflection level at 0,7 µm, whereas land without vegetation,
according to the type of surface, has a continuous linear course.
So much more active the chlorophyll of the plants, so much bigger is
the boost of the reflection level at the near infrared (0,78 - 1 µm).
In addition to the discrimination of vegetation between other surface,
it allows to detect the vitality of the vegetation.

The NDVI results from the following equation:

NDVI = (near IR - red) / (near IR + red)

In the area of red the incoming solar radiation won't be extensively
absorbed by the pigments of the mesophyll inside the folios, primarily
by the chlorophyll. In the area of the near infrared in contrast the
bigger part of the incoming radiation is reflected. The NDVI composes
a measurement for the photosynthetic activity and is strongly in
correlation with density and vitality of the vegetation. The normalizing
reduces topographicaly and atmospharic effects and enables the
simultaneous examination of a wide area.

Applied to the MERIS Level 1b TOA radiances, the NDVI processor
uses the following bands:

The near IR band: radiance_10 (753.75nm)
The red band:     radiance_6  (620nm)

So the result is:

NDVI = (radiance_10 - radiance_6) / (radiance_10 + radiance_6)


Also the processor computes an additional flags band called 'ndvi_flags'
with the following bit coding:

Bit Position  |  Description
--------------+--------------------------------------------------
   Bit 0      | The computed value for NDVI is NAN or is Infinite
   Bit 1      | The computed value for NDVI is less than 0 (zero)
   Bit 2      | The computed value for NDVI is greater than 1 (one)


Requirements
------------

The NDVI processor is a plug-in module for the BEAM software developed
by Brockmann Consult for ESA. This latter software must of course be
installed prior to the NDVI processor.

The BEAM software includes an application programming interface (API) and
a set of executable tools to facilitate the use of MERIS, AATSR and further
ASAR data products of the ESA ENVISAT satellite.

It can be freely downloaded from:

http://www.brockmann-consult.de/services/beam2/software/


Installation
------------

The BEAM package (here : BEAM VISAT Version 3.2) is installed within a
particular, user-selectable, directory. For the purpose of this documentation
file, that directory will be denoted $BEAM_HOME$. After installation of the
BEAM package, this directory should contain the following subdirectories
and files:

    $BEAM_HOME$
        |- auxdata/
        |- bin/
        |- doc/
        |- extensions/
        |- jre/
        |- lib/
        |- license/
        |- log/
        |- api-doc.zip
        |- changelog.txt
        |- epr_api-2.0.5.zip
        |- examples.zip
        |- known-issues.txt
        |- readme.txt
        |- src.zip
        |- version.txt

The NDVI processor plug-in, named 'ndviprocessor.jar', must simply be added
to the $BEAM_HOME$/extensions/ subdirectory. The BEAM application (VISAT)
will automatically integrate the NDVI processor within its interface.


Operation
---------

Once the BEAM software and the NDVI processor have been installed, the
package can be operated in two different modes: interactive and automatic.

Interactive processing
----------------------
To launch an interactive session, start the main BEAM software application
(VISAT) and select the NDVI processor using the following menu selections:

	Tools -> NDVI Processor.

A dialog window will appear:

	- Select the input file containing the MERIS Level-1b data to be
	  processed.
	- Specify the output file where you want the results to be written.
	- Select the output format from the pull-down menu, if different from
	  the BEAM default.
	- Optionally, save this configuration in a separate XML file, known to
	  BEAM software as a (reusable) 'processing request'.
	- Initiate the NDVI processor itself by clicking on the 'Run' button.

Automatic (batch-) processing
-----------------------------
To process one or more data sets automatically, i.e., without requiring
manual input, it is also possible to launch the application from the
command line (or an executable script, for that matter).

For convenience we supplied a UNIX script (ndvi.sh) and a script for Windows
computers (ndvi.bat) to run the NDVI processor in its interactive or
non-interactive (= batch) mode. Also we provide an example processing request
file (ndvi_temp.xml) as a template to use as a starting point from the command line.

Customisation
-------------
First: adapt the variable BEAM_HOME in ndvi.sh or ndvi.bat.

Windows example (in ndvi.bat):
SET BEAM_HOME=C:\beam-3.2

UNIX example (in ndvi.sh):
BEAM_HOME=/usr/local/bin/beam-3.2
 
Second: copy the template xml request file and rename it to a processing
request file you need.
Last: adapt the InputProduct and OutputProduct in the processing request
you want to use.
Then this script may be launched from anywhere in the following modes :

Interactive mode :
    unix:    ndvi.sh -i
    windows: ndvi.bat -i

Interactive mode with debug info :
    unix:    ndvi.sh -i -d
    windows: ndvi.bat -i -d

Interactive mode with predefined request :
    unix:    ndvi.sh -i <userdefined name>.xml
    windows: ndvi.bat -i <userdefined name>.xml

Batch mode with predefined request :
    unix:    ndvi.sh <userdefined name>.xml
    windows: ndvi.bat <userdefined name>.xml


Help For Plugins
----------------
You can attach help to your plugin by creating a help folder in the source tree
of your plugin. In the class which extends the AbstractProcessorPlugin you have
to overwrite the method getHelpsetPath() to attach the help from your plugin to
VISAT's help. Take a look at 'NdviProcessorVPI.java' (package com.bc.beam.visat).

If you write a plugin by implementing the VisatPlugIn directly, you have to use
the method 'addHelp(...)' from the given 'visatApp'

Example:

    ...
    public void initPlugIn(final VisatApp visatApp) {
        ...
        visatApp.addHelp(this, <the helpset path as string>);
        ...
    }
    ...


If you want the processors help button or help menu entry to show a processor
specific help theme, the method 'getHelpId()' must return a help theme that is
equal to a help target inside the 'map.xml'.
Take a look at 'NdviProcessorVPI.java' (package com.bc.beam.visat).
Also take a look at 'map.xml' (package com.bc.beam.processor.ndvi.help).

In the source directory (src) you can find the complete help structure for this
plugin. Take a look at the directory 'src/com/bc/beam/processor/ndvi/help'


Warranties and copyright information
------------------------------------

The NDVI processor package described in this document is provided
'as is', with no warranty of merchantability or fitness to any particular
purpose. Although every effort has been made to ensure accuracy of computations
and conformity to the algorithms as published in the references below, the
authors assume no responsibility whatsoever for any direct, indirect or
consequential damage resulting from the use of this software. The NDVI
processor is distributed free of charge and cannot be sold or re-sold. It
can be copied and distributed further, provided all documentation is attached
and provided the original source of the software is explicitly and
prominently described.

Questions, concerns and problems should be referred to the authors of the
software package at the address indicated at the start of this file.

The copyright on this file and the associated software remains with the
Brockmann Consult GmbH.
