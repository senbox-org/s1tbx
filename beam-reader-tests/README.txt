This module contains a set or BEAM ProductReader acceptance tests.

To be able to run the tests, two VM properties need to be present:

-Dbeam.reader.tests.execute=true
    This property enables the acceptance test runner. If not set, all tests are skipped and a message is
    printed to the console window.

-Dbeam.reader.tests.data.dir=<Path_To_Data>
    This property defines the root directory for the test dataset. All test-product definitions are referenced
    relative to this root directory. If the property is not set or does not denote a valid directory, the test
    setup fails.

-Dbeam.reader.tests.failOnMissingData=false
    By default the reader tests fail if test data is missing. This property can be set to false to avoid this.
    It is helpful for developer if they don't have the complete test data set on their developer machine.

-Dbeam.reader.tests.log.file=<Path_To_LogFile>
    Specifies the path the log file shall be written to. If not given no log file will be created.

-Dbeam.reader.tests.class.name=<ProductReaderPlugIn_ClassName>
    If the ProductReaderPlugIn class name is given only test for this reader plugin are executed.


Creating a reader test
~~~~~~~~~~~~~~~~~~~~~~

A number of files must be provided in the resource directory of the reader module. They must be placed in the same package
as the implemented plugin. The files must be named as follows
    <READER_PLUGIN_NAME>-data.json
    <PRODUCT-ID>.json
where <PRODUCT-ID> must macth the product identifier that is defined in the *-data.json file.
Example for Landsat:
Plugin is org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin.

The files must be located in
    \src\main\resources\org\esa\beam\dataio\landsat\geotiff\LandsatGeotiffReaderPlugin-data.json
    \src\main\resources\org\esa\beam\dataio\landsat\geotiff\L71191027_02720070313.json

Within the data file the test products are defined. Each definition consists of an id, a relative Path and a
description (see class org.esa.beam.dataio.TestProduct). Within the product-id file the expected content of each product
is defined (see class org.esa.beam.dataio.ExpectedContent).

The dependency to the reader must be added to the dependency list of the beam-reader-tests module.
ATTENTION:  Remember to install the reader module to your local maven repository after you have made changes.
            The dependencies used by the beam-reader-tests module link to installed jars.

For external reader acceptance tests the test class 'org.esa.beam.dataio.ProductReaderAcceptanceTest' must
be implemented empty. Just to have something to start.
In the dependency list of the external module additionally to the 'normal' dependency of beam-reader-tests module
the 'test-jar' type dependency must be added.

    <dependency>
        <groupId>org.esa.beam</groupId>
        <artifactId>beam-reader-tests</artifactId>
        <version>${beam.version}</version>
    </dependency>
    <dependency>
        <groupId>org.esa.beam</groupId>
        <artifactId>beam-reader-tests</artifactId>
        <type>test-jar</type>
        <scope>test</scope>
        <version>${beam.version}</version>
    </dependency>




