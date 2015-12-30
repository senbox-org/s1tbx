import numpy
import sys

from snappy import Product
from snappy import ProductData
from snappy import ProductIO
from snappy import ProductUtils
from snappy import String


if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0]);
    sys.exit(1)

print("Reading...")
sourceProduct = ProductIO.readProduct(sys.argv[1])
b1 = sourceProduct.getBand('reflec_5')
b2 = sourceProduct.getBand('reflec_7')
b3 = sourceProduct.getBand('reflec_9')
w1 = b1.getSpectralWavelength()
w2 = b2.getSpectralWavelength()
w3 = b3.getSpectralWavelength()
a = (w2 - w1) / (w3 - w1)
k = 1.03

width = sourceProduct.getSceneRasterWidth()
height = sourceProduct.getSceneRasterHeight()
targetProduct = Product('FLH_Product', 'FLH_Type', width, height)
targetBand = targetProduct.addBand('FLH', ProductData.TYPE_FLOAT32)
ProductUtils.copyGeoCoding(sourceProduct, targetProduct)
targetProduct.setProductWriter(ProductIO.getProductWriter('GeoTIFF'))

targetProduct.writeHeader(String('snappy_flh_output.tif'))

r1 = numpy.zeros(width, dtype=numpy.float32)
r2 = numpy.zeros(width, dtype=numpy.float32)
r3 = numpy.zeros(width, dtype=numpy.float32)

print("Writing...")

for y in range(height):
    b1.readPixels(0, y, width, 1, r1)
    b2.readPixels(0, y, width, 1, r2)
    b2.readPixels(0, y, width, 1, r3)
    print("processing line ", y, " of ", height)
    FLH = r2 - k * (r1 + a * (r3 - r1))
    targetBand.writePixels(0, y, width, 1, FLH)

targetProduct.closeIO()

print("Done.")