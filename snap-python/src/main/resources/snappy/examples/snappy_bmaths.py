import sys

from snappy import GPF
from snappy import HashMap
from snappy import ProductIO
from snappy import jpy

if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]

print("Reading...")
product = ProductIO.readProduct(file)
width = product.getSceneRasterWidth()
height = product.getSceneRasterHeight()
name = product.getName()
description = product.getDescription()
band_names = product.getBandNames()

print("Product: %s, %d x %d pixels, %s" % (name, width, height, description))
print("Bands:   %s" % (list(band_names)))

GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis()

BandDescriptor = jpy.get_type('org.esa.snap.core.gpf.common.BandMathsOp$BandDescriptor')

targetBand1 = BandDescriptor()
targetBand1.name = 'band_1'
targetBand1.type = 'float32'
targetBand1.expression = '(radiance_10 - radiance_7) / (radiance_10 + radiance_7)'

targetBand2 = BandDescriptor()
targetBand2.name = 'band_2'
targetBand2.type = 'float32'
targetBand2.expression = '(radiance_9 - radiance_6) / (radiance_9 + radiance_6)'

targetBands = jpy.array('org.esa.snap.core.gpf.common.BandMathsOp$BandDescriptor', 2)
targetBands[0] = targetBand1
targetBands[1] = targetBand2

parameters = HashMap()
parameters.put('targetBands', targetBands)

result = GPF.createProduct('BandMaths', parameters, product)

print("Writing...")

ProductIO.writeProduct(result, 'snappy_bmaths_output.dim', 'BEAM-DIMAP')

print("Done.")


"""
   Please note: the next major version of snappy/jpy will be more pythonic in the sense that implicit data type
   conversions are performed. The 'parameters' from above variable could then be given as a Python dict object:

    parameters = {
        'targetBands': [
            {
                'name': 'band_1',
                'type': 'float32',
                'expression': '(radiance_10 - radiance_7) / (radiance_10 + radiance_7)'
            },
            {
                'name': 'band_2',
                'type': 'float32',
                'expression': '(radiance_9 - radiance_6) / (radiance_9 + radiance_6)'
            }
        ]
    }
"""
