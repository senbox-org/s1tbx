#!/usr/bin/env python3

BEAMPY_NAME = 'beampy'
BEAMPY_VERSION = '5.0.2'
BEAMPY_SUMMARY = 'BEAM Python API'
BEAMPY_AUTHOR = 'Norman Fomferra, Brockmann Consult GmbH'
BEAMPY_JPY = 'jpy>=0.7.2'

try:
    import setuptools
    use_distutils = False
except ImportError:
    use_distutils = True

if use_distutils:
    from distutils.core import setup
    setup(name=BEAMPY_NAME,
          version=BEAMPY_VERSION,
          description=BEAMPY_SUMMARY,
          author=BEAMPY_AUTHOR,
          py_modules=[BEAMPY_NAME])
else:
    from setuptools import setup
    setup(name=BEAMPY_NAME,
          version=BEAMPY_VERSION,
          description=BEAMPY_SUMMARY,
          author=BEAMPY_AUTHOR,
          py_modules=[BEAMPY_NAME],
          install_requires=[BEAMPY_JPY])
