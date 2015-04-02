"""
The beampy module provides access to the Java SE APIs and SNAP Java APIs.

You can configure beampy by using a file named beampy.ini as follows:

    [DEFAULT]
    snap_home: C:/Program Files/snap-2.0
    java_classpath: target/classes
    java_options: -Djava.awt.headless=false
    java_max_mem: 4G
    debug: False

You can place beampy.ini next to <python3>/site-packages/beampy.py or put it in your current working directory.
The 'snap_home' options and all options starting with 'java_' are only used if you use the SNAP API from Python
and a new Java Virtual Machine is created. They are ignored if the SNAP API is called from SNAP itself
(e.g. SNAP command-line or GUI).

'beampy' uses a bundled 'jpy' module, see documentation at http://jpy.readthedocs.org/en/latest
and source code at https://github.com/bcdev/jpy
"""

import os
import sys

if sys.version_info >= (3, 0, 0,):
    # noinspection PyUnresolvedReferences
    import configparser as cp
else:
    # noinspection PyUnresolvedReferences
    import ConfigParser as cp

module_dir = os.path.dirname(os.path.realpath(__file__))
module_ini = os.path.basename(module_dir) + '.ini'

# Read configuration *.ini file from either '.', '<module_dir>/..', '<module_dir>' in this order
config = cp.ConfigParser()
config.read([module_ini,
             os.path.join(module_dir, os.path.join('..', module_ini)),
             os.path.join(module_dir, module_ini)
             ])

debug = False
if config.has_option('DEFAULT', 'debug'):
    debug = config.getboolean('DEFAULT', 'debug')

# Pre-load Java VM shared library and import 'jpy' the Java-Python bridge
if module_dir not in sys.path:
    sys.path.append(module_dir)
import jpyutil
jpyutil.preload_jvm_dll()
import jpy

if debug:
    # jpy.diag.F_OFF    0x00
    # jpy.diag.F_TYPE   0x01
    # jpy.diag.F_METH   0x02
    # jpy.diag.F_EXEC   0x04
    # jpy.diag.F_MEM    0x08
    # jpy.diag.F_JVM    0x10
    # jpy.diag.F_ERR    0x20
    # jpy.diag.F_ALL    0xff
    jpy.diag.flags = jpy.diag.F_EXEC + jpy.diag.F_ERR


# Recursively searches for JAR files in 'dir_path' and appends them to 'classpath'.
# Note: Sub-directories named 'locale' or 'docs' are not searched and
#       JAR files ending with '-ui.jar' or '-examples.jar' are excluded.
# Only used if this module is not imported from Java.
#
def _collect_snap_jvm_classpath(dir_path, classpath):
    for name in os.listdir(dir_path):
        path = os.path.join(dir_path, name)
        if os.path.isfile(path) and name.endswith('.jar') and \
                not (name.endswith('-ui.jar') or name.endswith('-examples.jar')):
            classpath.append(path)
        elif os.path.isdir(path) and not (name == 'locale' or name == 'docs'):
            _collect_snap_jvm_classpath(path, classpath)


# Searches for *.jar files in directory given by global 'snap_home' variable and returns them as a list.
# Only used if this module is not imported from Java.
#
def _get_snap_jvm_classpath():

    dir_names = []
    for name in os.listdir(snap_home):
        if os.path.isdir(os.path.join(snap_home, name)):
            dir_names.append(name)

    module_dirs = []

    if 'bin' in dir_names and 'etc' in dir_names and 'snap' in dir_names:
        # SNAP Desktop Distribution Directory
        for dir_name in dir_names:
            if not (dir_name == 'platform' or dir_name == 'ide'):
                dir_path = os.path.join(snap_home, dir_name, 'modules')
                if os.path.isdir(dir_path):
                    module_dirs.append(dir_path)
    elif 'lib' in dir_names and 'modules' in dir_names:
        # SNAP Engine Distribution Directory
        module_dirs = [os.path.join(snap_home, 'modules'), os.path.join(snap_home, 'lib')]
    else:
        raise RuntimeError('does not seem to be a valid SNAP distribution directory: ' + snap_home)

    if debug:
        import pprint
        print(module_dir + ': module_dirs = ')
        pprint.pprint(module_dirs)

    classpath = []
    for path in module_dirs:
        _collect_snap_jvm_classpath(path, classpath)

    if debug:
        import pprint
        print(module_dir + ': classpath =')
        pprint.pprint(classpath)

    return classpath


# Creates a list of Java JVM options, including the Java classpath derived from the global 'snap_home' variable.
# Only used if this module is not imported from Java.
#
def _get_snap_jvm_options():
    global snap_home, extra_classpath, max_mem, options, extra_options

    if config.has_option('DEFAULT', 'snap_home'):
        snap_home = config.get('DEFAULT', 'snap_home')
    else:
        snap_home = os.getenv('SNAP_HOME')

    if snap_home is None or not os.path.isdir(snap_home):
        raise IOError("Can't find SNAP distribution directory. Either configure variable 'snap_home' " +
                      "in file './beampy.ini' or set environment variable 'SNAP_HOME' to an " +
                      "existing SNAP distribution directory.")

    classpath = _get_snap_jvm_classpath()

    if config.has_option('DEFAULT', 'java_classpath'):
        extra_classpath = config.get('DEFAULT', 'java_classpath')
        classpath += extra_classpath.split(os.pathsep)

    max_mem = '512M'
    if config.has_option('DEFAULT', 'java_max_mem'):
        max_mem = config.get('DEFAULT', 'java_max_mem')

    options = ['-Djava.awt.headless=true',
               '-Djava.class.path=' + os.pathsep.join(classpath),
               '-Xmx' + max_mem]

    if config.has_option('DEFAULT', 'java_options'):
        extra_options = config.get('DEFAULT', 'java_options')
        options += extra_options.split('|')

    return options


# Figure out if this module is called from a Java VM. If not, derive a list of Java VM options and create the Java VM.
called_from_java = jpy.has_jvm()
if not called_from_java:
    jpy.create_jvm(options=_get_snap_jvm_options())


# Don't need these functions anymore
del _get_snap_jvm_options
del _get_snap_jvm_classpath
del _collect_snap_jvm_classpath


# noinspection PyUnusedLocal
def annotate_RasterDataNode_methods(type_name, method):
    index = -1

    if sys.version_info >= (3, 0, 0,):
        arr_z_type_str = "<class '[Z'>"
        arr_i_type_str = "<class '[I'>"
        arr_f_type_str = "<class '[F'>"
        arr_d_type_str = "<class '[D'>"
    else:
        arr_z_type_str = "<type '[Z'>"
        arr_i_type_str = "<type '[I'>"
        arr_f_type_str = "<type '[F'>"
        arr_d_type_str = "<type '[D'>"

    if method.name == 'readPixels' and method.param_count >= 5:
        index = 4
        param_type_str = str(method.get_param_type(index))
        if param_type_str == arr_i_type_str \
                or param_type_str == arr_f_type_str \
                or param_type_str == arr_d_type_str:
            method.set_param_mutable(index, True)
            method.set_param_output(index, True)
            method.set_param_return(index, True)

    if method.name == 'readValidMask' and method.param_count == 5:
        index = 4
        param_type_str = str(method.get_param_type(index))
        if param_type_str == arr_z_type_str:
            method.set_param_mutable(index, True)
            method.set_param_output(index, True)
            method.set_param_return(index, True)

    if index >= 0 and debug:
        print(
            'annotate_RasterDataNode_methods: Method "{0}": modified parameter {1:d}: mutable = {2}, return = {3}'
                .format(method.name, index, method.is_param_mutable(index), method.is_param_return(index)))

    return True


jpy.type_callbacks['org.esa.beam.framework.datamodel.RasterDataNode'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.AbstractBand'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.Band'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.VirtualBand'] = annotate_RasterDataNode_methods

#
# Preload and assign frequently used Java classes from the Java SE and SNAP Java API.
#

try:
    # Note we may later want to read pre-defined types from a configuration file (beampy.ini)

    String = jpy.get_type('java.lang.String')
    File = jpy.get_type('java.io.File')
    Rectangle = jpy.get_type('java.awt.Rectangle')

    SystemUtils = jpy.get_type('org.esa.beam.util.SystemUtils')
    ProductIO = jpy.get_type('org.esa.beam.framework.dataio.ProductIO')

    Product = jpy.get_type('org.esa.beam.framework.datamodel.Product')
    ProductData = jpy.get_type('org.esa.beam.framework.datamodel.ProductData')
    RasterDataNode = jpy.get_type('org.esa.beam.framework.datamodel.RasterDataNode')
    AbstractBand = jpy.get_type('org.esa.beam.framework.datamodel.AbstractBand')
    Band = jpy.get_type('org.esa.beam.framework.datamodel.Band')
    VirtualBand = jpy.get_type('org.esa.beam.framework.datamodel.VirtualBand')
    GeoCoding = jpy.get_type('org.esa.beam.framework.datamodel.GeoCoding')
    GeoPos = jpy.get_type('org.esa.beam.framework.datamodel.GeoPos')
    PixelPos = jpy.get_type('org.esa.beam.framework.datamodel.PixelPos')
    FlagCoding = jpy.get_type('org.esa.beam.framework.datamodel.FlagCoding')
    ProductNodeGroup = jpy.get_type('org.esa.beam.framework.datamodel.ProductNodeGroup')

    ProductUtils = jpy.get_type('org.esa.beam.util.ProductUtils')

    GPF = jpy.get_type('org.esa.beam.framework.gpf.GPF')
    Operator = jpy.get_type('org.esa.beam.framework.gpf.Operator')
    Tile = jpy.get_type('org.esa.beam.framework.gpf.Tile')

except Exception:
    jpy.destroy_jvm()
    raise


# Note: use the following code to initialise BEAM's 3rd party libraries, JAI and GeoTools.
# Only needed, if SNAP Python API is not called from Java (e.g. from SNAP gpt or SNAP desktop).
if not called_from_java:
    SystemUtils.init3rdPartyLibs(None)
