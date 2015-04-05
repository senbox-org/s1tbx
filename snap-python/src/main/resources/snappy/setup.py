#!/usr/bin/env python3

SNAPPY_NAME = 'snappy'
SNAPPY_VERSION = '2.0-SNAPSHOT'
SNAPPY_SUMMARY = 'SNAP Python API'
SNAPPY_AUTHOR = 'Norman Fomferra, Brockmann Consult GmbH'

# todo - fix this setup script so that the parent directory ('..' = 'snappy') including its
# content is installed into Python

from distutils.core import setup
setup(name=SNAPPY_NAME,
      version=SNAPPY_VERSION,
      description=SNAPPY_SUMMARY,
      author=SNAPPY_AUTHOR,
      py_modules=['../' + SNAPPY_NAME])
