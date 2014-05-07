"""
This module represents the BEAM Python API.

In order to use beampy the module jpy must be installed first.

You can configure beampy by using a file named beampy.ini as follows:

    [DEFAULT]
    beam_home: C:\Program Files\beam-4.11
    extra_classpath: target/classes
    extra_options: -Djava.awt.headless=false
    max_mem: 4G
    debug: False

You can place beampy.ini next to <python3>/site-packages/beampy.py or put it in your current working directory.

"""

__author__ = "Norman Fomferra, Brockmann Consult GmbH"

import os
import configparser

module_dir = os.path.dirname(os.path.realpath(__file__))
config = configparser.ConfigParser()
config.read(['./beampy.ini', os.path.join(module_dir, 'beampy.ini')])

debug = config.getboolean('DEFAULT', 'debug', fallback=False)

# todo - determine path to JVM shared library and extend PATH / LD_LIBRARY_PATH env vars so that jpy can load it

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
    beam_home = config.get('DEFAULT', 'beam_home',
                           fallback=os.getenv('BEAM_HOME',
                                              os.getenv('BEAM4_HOME',
                                                        os.getenv('BEAM5_HOME'))))
    if beam_home is None or not os.path.exists(beam_home):
        raise RuntimeError('environment variable "BEAM_HOME" must be set to a valid BEAM installation directory')


    #import pprint
    searchpath = _get_beam_jar_locations()
    #pprint.pprint(searchpath)
    classpath = _create_classpath(searchpath)
    extra_classpath = config.get('DEFAULT', 'extra_classpath', fallback=None)
    if extra_classpath:
        classpath += extra_classpath.split(sep=os.pathsep)

    #pprint.pprint(classpath)
    max_mem = config.get('DEFAULT', 'max_mem', fallback='512M')
    options = ['-Djava.awt.headless=true',
               '-Djava.class.path=' + os.pathsep.join(classpath), 
               '-Xmx' + max_mem]
    extra_options = config.get('DEFAULT', 'extra_options', fallback=None)
    if extra_options:
        options += extra_options.split(sep='|')

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

    if method.name == 'readPixels' and method.param_count >= 5:
        index = 4
        param_type_str = str(method.get_param_type(index))
        if param_type_str == "<class '[I'>"\
            or param_type_str == "<class '[F'>" \
            or param_type_str == "<class '[D'>":
            method.set_param_mutable(index, True)
            method.set_param_return(index, True)

    if method.name == 'readValidMask' and method.param_count == 5:
        index = 4
        param_type_str = str(method.get_param_type(index))
        if param_type_str == "<class '[Z'>":
            method.set_param_mutable(index, True)
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

    #SubsetOp = jpy.get_type('org.esa.beam.gpf.operators.standard.SubsetOp')
    #JtsGeometryConverter = jpy.get_type('org.esa.beam.util.converters.JtsGeometryConverter')



except Exception:
    jpy.destroy_jvm()
    raise

