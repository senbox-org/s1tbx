#!/usr/bin/env python3

BEAMPY_NAME = 'beampy'
BEAMPY_VERSION = '2.0-SNAPSHOT'
BEAMPY_SUMMARY = 'SNAP Python API'
BEAMPY_AUTHOR = 'Norman Fomferra, Brockmann Consult GmbH'

# todo - fix this setup script so that the parent directory ('..' = 'beampy') including its
# content is installed into Python

from distutils.core import setup
setup(name=BEAMPY_NAME,
      version=BEAMPY_VERSION,
      description=BEAMPY_SUMMARY,
      author=BEAMPY_AUTHOR,
      py_modules=['../' + BEAMPY_NAME])
