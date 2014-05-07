beam-python
===========

The `beam-python` module enables Python developers to

1. use the BEAM Java API from Python, and to
2. extend BEAM by *operator plug-ins* for EO data processing written in the Python programming language.

It is worth mentioning that the `beam-python` module works with the standard *CPython*, so that any native
Python extension modules such as `numpy` and `scipy` can be used. Before you read further you may have a look at the
example code in

1. `beampy-examples` for using the BEAM Java API from Python, and
2. `beampy-operator-example` for extending BEAM by an operator plugin.

The link from Python to the BEAM Java API is established via a Python module named `beampy`. The `beampy` module
depends on a *bi-directional* Java-Python bridge that enables calls from Python into a Java virtual machine
and, at the same time, the other way round. This bridge is implemented by the [jpy Project](https://github.com/bcdev/jpy)
and is independent from `beampy` module.

Before you can start using the BEAM API or developing BEAM operator plugins with Python you need
to install `jpy` and `beampy`. Unfortunately this has to be done manually, so be prepared to invest half an hour or so
for setting things up correctly.


Set-up
------

You will need the following software development tools:
* [Python 3.3](http://www.python.org/) or higher
* [numpy](http://www.numpy.org/), for Python 3.3 (required for the examples)
* [git](http://git-scm.com/)
* [Oracle JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/) or higher
* [Maven 3](http://maven.apache.org/), or higher
* For Windows: [Microsoft Windows SDK 7.1](http://www.microsoft.com/en-us/download/details.aspx?id=8279)
* For Darwin: [Xcode 5](https://itunes.apple.com/de/app/xcode/id497799835?mt=12), or higher
* For Linux: gcc

Clone or download the `jpy` repository from it's GitHub home

    > git clone https://github.com/bcdev/jpy.git

and follow the steps described in the `jpy`'s [`README.md`](https://github.com/bcdev/jpy/blob/master/README.md).

If you encounter any problems during set-up please do not hesitate to contact the
[BEAM user forum](http://www.brockmann-consult.de/cms/web/beam/forum).

After successful installation of `jpy`, you will need to install `beampy`.

### Darwin / Linux:

    > export BEAM_HOME=<path to your BEAM 5 installation>
    > cd $BEAM_HOME/modules/beam-python/beampy
    > python3 setup.py install --user

### Windows:

    > SET BEAM_HOME=<path to your BEAM 5 installation>
    > cd %BEAM_HOME%\modules\beam-python\beampy
    > python setup.py install

Testing the `beampy` installation
---------------------------------

When `beampy` is imported into your Python script or module, it will scan a BEAM installation for the available
BEAM API components. For this purpose, `beampy` needs to know where the BEAM installation is located. It can either be
configured via the environment variables `BEAM_HOME` or `BEAM5_HOME` or by using a dedicated *INI file* as described below.

### Darwin / Linux:

    > set BEAM_HOME=<path to your BEAM 5 installation>
    > python3
    >>> import beampy

### Windows:

    > export BEAM_HOME=<path to your BEAM 5 installation>
    > python
    >>> import beampy


`beampy` Configuration
----------------------

`beampy` can be configured by an *INI file* `beampy.ini`. This file is read from the current working directory
or from the system-dependent location from which the installed Python `beampy` module is loaded from.

Given here is an example of its content (Windows):

    [DEFAULT]
    beam_home: C:\Program Files\beam-5.0
    extra_classpath: target/classes
    max_mem: 8G
    debug: True

