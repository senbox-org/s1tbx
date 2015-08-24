SNAPPY_NAME = 'snappy'
SNAPPY_VERSION = '2.0.0'
SNAPPY_SUMMARY = 'snappy - The SNAP Python API'
SNAPPY_AUTHOR = 'SNAP Development Team, Brockmann Consult GmbH'

from distutils.core import setup
import os

old_cwd = os.getcwd()
new_cwd = os.path.normpath(os.path.join(os.path.dirname(__file__), '..'))
os.chdir(new_cwd)

try:
    setup(
        name=SNAPPY_NAME,
        version=SNAPPY_VERSION,
        description=SNAPPY_SUMMARY,
        author=SNAPPY_AUTHOR,
        packages=[SNAPPY_NAME],
        package_data={SNAPPY_NAME: ['*.so', '*.pyd', '*.dll', '*.dll', '*.properties', '*.ini', '*.info']}
    )
finally:
    os.chdir(old_cwd)
