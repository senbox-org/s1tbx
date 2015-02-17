#!/usr/bin/env python3

BEAMPY_NAME = 'beampy'
BEAMPY_VERSION = '5.0.5-SNAPSHOT'
BEAMPY_SUMMARY = 'BEAM Python API'
BEAMPY_AUTHOR = 'Norman Fomferra, Brockmann Consult GmbH'
BEAMPY_JPY = 'jpy>=0.8-SNAPSHOT'

try:
    import setuptools
    has_setuptools = True
except ImportError:
    has_setuptools = False

if has_setuptools:
    from setuptools import setup
    setup(name=BEAMPY_NAME,
          version=BEAMPY_VERSION,
          description=BEAMPY_SUMMARY,
          author=BEAMPY_AUTHOR,
          py_modules=[BEAMPY_NAME],
          install_requires=[BEAMPY_JPY])
else:
    from distutils.core import setup
    setup(name=BEAMPY_NAME,
          version=BEAMPY_VERSION,
          description=BEAMPY_SUMMARY,
          author=BEAMPY_AUTHOR,
          py_modules=[BEAMPY_NAME])
