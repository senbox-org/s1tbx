snap-python
===========

The `snap-python` module enables Python developers to

1. use the SNAP Java API from Python, and to
2. extend SNAP by *operator plug-ins* for EO data processing written in the Python programming language.

It is worth mentioning that the `snap-python` module works with the standard *CPython*, so that any native
Python extension modules such as `numpy` and `scipy` can be used. Before you read further you may have a look at the
example code in

1. `snappy-examples/*.py` for using the SNAP Java API from Python, and
2. `snappy-examples/snappy-ndvi-operator` for extending SNAP by an (NDVI) operator plugin.

The link from Python to the SNAP Java API is established via a Python module named *snappy*. The snappy module
depends on a *bi-directional* Java-Python bridge *jpy* that enables calls from Python into a Java virtual machine
and, at the same time, the other way round. This bridge is implemented by the [jpy Project](https://github.com/bcdev/jpy)
and is independent from the snappy module.

snappy has been tested with Python 2.7, 3.3 and 3.4 with Java 7 and 8 JDKs. 

Since `snap-python` version 5.0.5, SNAP configures itself for a given Python executable. If not already done
by the installer, set the configuration property `snap.pythonExecutable` in file `${snap.home}/etc/snap.properties`
to your desired Python executable.


////////////////////////////// To following text is outdated - MUST UPDATE SOON ////////////////////////////// 


Before you can start using the SNAP API or developing SNAP operator plugins with Python you need configure 
SNAP for the desired Python version. 

If there is no matching binary distribution, you will have to build `jpy` on your own and then copy your binary 
distribution to the `${snap.home}/modules/${snap-python}/lib` directory.

Unfortunately this has to be done manually, so be 
prepared to invest at least half an hour for setting up things correctly.

Installation
------------

The first step is to properly install `jpy` as described in the [jpy documentation](http://jpy.readthedocs.org/en/latest/install.html).
(Be sure use `jpy` version 0.7.3 or higher.) After successful installation of `jpy`, make sure that you have run VISAT at least once so that all modules are unpacked. Afterwards, you will be able to install `snappy` as follows:

On Darwin / Linux type:

    export SNAP_HOME=<path to your SNAP 5 installation>
    cd $SNAP_HOME/modules/snap-python-5.0/snappy
    python3 setup.py install --user

On Windows type:

    SET SNAP_HOME=<path to your SNAP 5 installation>
    cd %SNAP_HOME%\modules\snap-python-5.0\snappy
    python setup.py install

If you encounter any problems during the `jpy` or `snappy` setup please do not hesitate to contact the
[SNAP user forum](http://www.brockmann-consult.de/cms/web/snap/forum).

Testing
-------

When snappy is imported into your Python script or module, it will scan a SNAP installation for the available
SNAP API components. For this purpose, snappy needs to know where the SNAP installation is located. It can either be
configured via the environment variables `SNAP_HOME` or `SNAP2_HOME` or by using a dedicated *INI file* as described
below.

On Darwin / Linux type:

    set SNAP_HOME=<path to your SNAP 2.0 installation>
    python3
    >>> import snappy

On Windows type:

    export SNAP_HOME=<path to your SNAP 5 installation>
    python
    >>> import snappy

If the import is successful (no errors are raised) you can exit the Python interpreter and perform the test cases in the `snappy` directory.
They all require an EO data product file as input named `MER_RR__1P.N1`, which is an Envisat MERIS L1b product.
You can download an Envisat MERIS L1b test file from the
[SNAP home page](http://www.brockmann-consult.de/cms/web/snap/meris-products)
and rename it to `MER_RR__1P.N1` in order to run the tests. The tests expect [numpy](http://www.numpy.org/) to be installed. 

    python test_snappy_mem.py
    python test_snappy_perf.py
    python test_snappy_product.py

Please note that the SNAP API is actually independent of specific data formats, the MERIS file in this case
is only used as an example and for testing.

Configuration
-------------

`snappy` can be configured by an *INI file* `snappy.ini`. This file is read from the current working directory
or from the system-dependent location from which the installed Python `snappy` module is loaded from.

Given here is an example of its content (Windows):

    [DEFAULT]
    snap_home: C:\Program Files\snap-5.0
    extra_classpath: target/classes
    max_mem: 8G
    debug: True


Running snappy in an Apache webserver
-------------------------------------

Using the [mod_wsgi](https://code.google.com/p/modwsgi/)-module within an [Apache HTTP Server](http://httpd.apache.org/) environment allows to use `snappy` within web applications. However, there are a number of common pitfalls, which are listed in the sections below.

### Cannot open shared object

When doing `import jpy`, you might get an error similar to `ImportError: libjvm.so: cannot open shared object file: No such file or directory`. However, the file exists, and you don't get this error when you use the console. The problem is solved by performing the following steps:

* `locate libjvm.so`
* Output is, say, `/path/to/libjvm.so`
* Add `/path/to` to the `/etc/ld.so.conf` file
* Run `sudo ldconfig`
* Restart Apache
* 
(copied from [stackoverflow](http://stackoverflow.com/a/4527546/2043113))

### Environment variables not set

No matter if you have set all required environment variables in your shell, you might nonetheless receive the following error when doing `import snappy`: `RuntimeError: environment variable "SNAP_HOME" must be set to a valid SNAP installation directory`.
The reason for this is that the environment needs to be preserved for the Apache. A possible solution is to set the environment within the startup routine of the web application, such as

    os.environ['JAVA_HOME'] = settings.JAVA_HOME
    os.environ['JDK_HOME'] = settings.JDK_HOME
    os.environ['PATH'] = settings.PATH_extension + ':' + os.getenv('PATH', '')
    os.environ['LD_LIBRARY_PATH'] = settings.LD_LIBRARY_PATH_extension + ':' + os.getenv('LD_LIBRARY_PATH', '')
    os.environ['SNAP_HOME'] = settings.SNAP_HOME

, where `settings` contains the respective values.

### JAI 

When using JAI classes, you might get the following error: `RuntimeError: java.lang.NoClassDefFoundError: Could not initialize class javax.media.jai.JAI`. This can be resolved by calling

    SystemUtils = jpy.get_type('org.esa.snap.util.SystemUtils')
    SystemUtils.init3rdPartyLibs(None)

in the startup routine of the web application.

Examples
--------


### SNAP Java API Usage

The examples for the API usage are simple tools that compute an output product from an input product.
You can download an Envisat MERIS test files used as input from the
[SNAP home page](http://www.brockmann-consult.de/cms/web/snap/meris-products)
and rename it to `MER_RR__1P.N1` and `MER_RR__2P.N1` in order to run the example code.
All examples expect [numpy](http://www.numpy.org/) to be installed. 

Computing a Fluorescence Line Height (FLH) product from water-leaving reflectances:

    python snappy_flh.py MER_RR__2P.N1

Computing a Normalized Difference Vegetation Index (NDVI) product from top-of-atmosphere radiances:

    python snappy_ndvi.py MER_RR__1P.N1
    python snappy_ndvi_with_masks.py MER_RR__1P.N1

Performing arbitrary band maths:

    python snappy_bmaths.py MER_RR__1P.N1

Tailoring any input product to a spatial subset:

    python snappy_subset.py MER_RR__2P.N1 "POLYGON((15.786082 45.30223, 11.798364 46.118263, 10.878688 43.61961, 14.722727 42.85818, 15.786082 45.30223))"


There are many more possibilities using the SNAP API. Actually all Java classes of the SNAP API can be used.
As the SNAP API can be used from Python in a similar way as from Java, all of the Java API documentation applies as well.
Please check:

* [SNAP API Documentation](http://www.brockmann-consult.de/snap/doc/apidocs/index.html)

snappy imports the most frequently used Java API classes by default
 
* `org.esa.snap.core.dataio.ProductIO`
* `org.esa.snap.core.datamodel.Product`
* `org.esa.snap.core.datamodel.RasterDataNode`
* `org.esa.snap.core.datamodel.AbstractBand`
* `org.esa.snap.core.datamodel.Band`
* `org.esa.snap.core.datamodel.VirtualBand`
* `org.esa.snap.core.datamodel.GeoCoding`
* `org.esa.snap.core.datamodel.PixelPos`
* `org.esa.snap.core.datamodel.PixelPos`
* `org.esa.snap.util.ProductUtils`
* `org.esa.snap.core.gpf.GPF`
* `org.esa.snap.core.gpf.Operator`
* `org.esa.snap.core.gpf.Tile`

To import other Java API classes, get the fully qualified type name from the API reference and import it using jpy. 
For example:

    jpy = snappy.jpy
    Color = jpy.get_type('java.awt.Color')
    ColorPoint = jpy.get_type('org.esa.snap.core.datamodel.ColorPaletteDef$Point')
    ColorPaletteDef = jpy.get_type('org.esa.snap.core.datamodel.ColorPaletteDef')
    ImageInfo = jpy.get_type('org.esa.snap.core.datamodel.ImageInfo')
    ImageManager = jpy.get_type('org.esa.snap.core.image.ImageManager')
    JAI = jpy.get_type('javax.media.jai.JAI')
    
Due to the 1:1 translation of Java to Python, a lot of code in the Java programming tutorial applies to 
Python as well:

* [SNAP Programming Tutorial](http://www.brockmann-consult.de/snap-wiki/display/SNAP/SNAP+4+Programming+Tutorial)

### SNAP Operator Plugin

The directory `snappy-operator-example` represents an NDVI operator plugin for SNAP. In order to activate it in SNAP
copy it to the SNAP `modules` directory and start VISAT. You will find a new entry *Python NDVI Operator...*
in VISAT's Processing menu. It will also be available from SNAP's `gpt` command-line tool.

Note that running Python operator plugins requires the environment variable `SNAP_HOME` to be accessible by VISAT
and the `gpt` command-line tool.

You can use the directory `snappy-operator-example` as a template for new Python data processors. You can also
add new Python code directly to it. In this case

* create a new `snappy-operator-example/<your_operator>.py` (e.g. by copying the existing `ndvi_op.py`),
* create a new `snappy-operator-example/<your_operator>-info.xml` (e.g. by copying the existing `ndvi_op-info.py`) and
* register *<your_operator>* in `snappy-operator-example/META-INF/services/snappy-operators`.

Of course you will need to adapt the contents of the files accordingly.

Again, please don't hesitate to contact the
[SNAP user forum](http://forum.step.esa.int).

*Have fun!*
