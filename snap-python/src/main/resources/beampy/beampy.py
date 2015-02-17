"""
The beampy module provides access to the BEAM Java APIs.

In order to use beampy the module 'jpy' must be installed first.

You can configure beampy by using a file named beampy.ini as follows:

    [DEFAULT]
    beam_home: C:\Program Files\beam-5
    extra_classpath: target/classes
    extra_options: -Djava.awt.headless=false
    max_mem: 4G
    debug: False

You can place beampy.ini next to <python3>/site-packages/beampy.py or put it in your current working directory.

"""

import os
import sys

if sys.version_info >= (3, 0, 0,):
    import configparser as cp
else:
    import ConfigParser as cp

module_dir = os.path.dirname(os.path.realpath(__file__))
config = cp.ConfigParser()
config.read(['beampy.ini', os.path.join(module_dir, 'beampy.ini')])

debug = False
if config.has_option('DEFAULT', 'debug'):
    debug = config.getboolean('DEFAULT', 'debug')

import jpyutil
jpyutil.preload_jvm_dll()
import jpy

if debug:
    jpy.diag.flags = jpy.diag.F_ALL


def _get_beam_jar_locations():

    beam_bin = os.path.join(beam_home, 'bin')
    beam_lib = os.path.join(beam_home, 'lib')
    beam_mod = os.path.join(beam_home, 'modules')

    #print('beam_bin =', beam_bin, os.path.exists(beam_bin))
    #print('beam_lib =', beam_lib, os.path.exists(beam_lib))
    #print('beam_mod =', beam_mod, os.path.exists(beam_mod))

    if not (os.path.exists(beam_bin)
            and os.path.exists(beam_lib)
            and os.path.exists(beam_mod)):
        raise RuntimeError('does not seem to be a valid BEAM installation path: ' + beam_home)

    return [beam_bin, beam_lib, beam_mod]


def _collect_classpath(path, classpath):
    for name in os.listdir(path):
        file = os.path.join(path, name)
        if name.endswith('.jar') or name.endswith('.zip') or os.path.isdir(file):
            classpath.append(file)


def _create_classpath(searchpath):

    classpath = []
    for path in searchpath:
        _collect_classpath(path, classpath)
    return classpath


def _get_jvm_options():
    global beam_home, searchpath, classpath, extra_classpath, max_mem, options, extra_options

    if config.has_option('DEFAULT', 'beam_home'):
        beam_home = config.get('DEFAULT', 'beam_home')
    else:
        beam_home = os.getenv('BEAM_HOME', os.getenv('BEAM4_HOME', os.getenv('BEAM5_HOME')))

    if beam_home is None or not os.path.isdir(beam_home):
        raise IOError('environment variable "BEAM_HOME" must be set to a valid BEAM installation directory')

    module_prefix = 'beam-python-'
    module_glob_path = os.path.join(beam_home, 'modules', module_prefix + "*")

    import glob

    module_dirs = glob.glob(module_glob_path)

    if len(module_dirs) == 0:
        raise IOError("got no results for '%s'" % module_glob_path)

    if len(module_dirs) == 1:
        module_dir = module_dirs[0]
    else:
        versions = [(os.path.basename(module_dir)[len(module_prefix):].upper().split('-'), module_dir) for module_dir in module_dirs]
        max_parts = max([len(v) for (v,p) in versions])
        versions = [(v + (max_parts - len(v)) * ['~'], p) for (v,p) in versions]
        module_dir = max(versions, key=lambda x: x[0])[1]

    beampy_module_dir = os.path.join(module_dir, 'beampy')
    #pprint("beampy_module_dir = '%s'" % beampy_module_dir)

    sys.path = [beampy_module_dir] + sys.path

    #import pprint
    searchpath = _get_beam_jar_locations()
    #pprint.pprint(searchpath)

    classpath = _create_classpath(searchpath)

    if config.has_option('DEFAULT', 'extra_classpath'):
        extra_classpath = config.get('DEFAULT', 'extra_classpath')
        classpath += extra_classpath.split(os.pathsep)

    #pprint.pprint(classpath)

    max_mem = '512M'
    if config.has_option('DEFAULT', 'extra_options'):
        max_mem = config.get('DEFAULT', 'max_mem')

    options = ['-Djava.awt.headless=true',
               '-Djava.class.path=' + os.pathsep.join(classpath), 
               '-Xmx' + max_mem]

    if config.has_option('DEFAULT', 'extra_options'):
        extra_options = config.get('DEFAULT', 'extra_options')
        options += extra_options.split('|')

    return options


if not jpy.has_jvm():
    jpy.create_jvm(options=_get_jvm_options())


# Don't need these functions anymore
del _get_jvm_options
del _get_beam_jar_locations
del _create_classpath
del _collect_classpath


def annotate_RasterDataNode_methods(type, method):
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
        print('annotate_RasterDataNode_methods: Method "{0}": modified parameter {1:d}: mutable = {2}, return = {3}'.format(
              method.name, index, method.is_param_mutable(index), method.is_param_return(index)))

    return True


jpy.type_callbacks['org.esa.beam.framework.datamodel.RasterDataNode'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.AbstractBand'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.Band'] = annotate_RasterDataNode_methods
jpy.type_callbacks['org.esa.beam.framework.datamodel.VirtualBand'] = annotate_RasterDataNode_methods


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
# This is only needed, if you use the BEAM API from Python.
# Don use it, if you are extending BEAM by writing your own GPF Operator.
#  SystemUtils.init3rdPartyLibs(None)
