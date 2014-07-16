import sys

import numpy
from beampy import String
from beampy import Product
from beampy import ProductData
from beampy import ProductIO
from beampy import ProductUtils


if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0]);
    sys.exit(1)

# Uncomment if you receive errors of type com.sun.media.jai.util.ServiceConfigurationError, see
# http://www.brockmann-consult.de/beam-jira/browse/BEAM-1699
#beampy.SystemUtils.init3rdPartyLibs(None)  # Initialise BEAM's third party Java libraries JAI and GeoTools.


print("Reading...")
product = ProductIO.readProduct(sys.argv[1])
width = product.getSceneRasterWidth()
height = product.getSceneRasterHeight()
name = product.getName()
desc = product.getDescription()
band_names = product.getBandNames()

print("Product: %s, %d x %d pixels, %s" % (name, width, height, desc))
print("Bands:   %s" % (band_names))

b7 = product.getBand('radiance_7')
b10 = product.getBand('radiance_10')
ndviProduct = Product('NDVI', 'NDVI', width, height)
ndviBand = ndviProduct.addBand('ndvi', ProductData.TYPE_FLOAT32)
ndviBand.setNoDataValue(numpy.nan)
ndviBand.setNoDataValueUsed(True)

writer = ProductIO.getProductWriter('BEAM-DIMAP')

ProductUtils.copyGeoCoding(product, ndviProduct)

ndviProduct.setProductWriter(writer)
ndviProduct.writeHeader(String('beampy_ndvi_with_masks_output.dim'))

r7  = numpy.zeros(width, dtype=numpy.float32)
r10 = numpy.zeros(width, dtype=numpy.float32)

v7  = numpy.zeros(width, dtype=numpy.uint8)
v10 = numpy.zeros(width, dtype=numpy.uint8)

print("Writing...")

for y in range(height):
    b7.readPixels(0, y, width, 1, r7)
    b10.readPixels(0, y, width, 1, r10)

    b7.readValidMask(0, y, width, 1, v7)
    b10.readValidMask(0, y, width, 1, v10)

    invalidMask7 = numpy.where(v7 == 0, 1, 0)
    invalidMask10 = numpy.where(v10 == 0, 1, 0)

    ma7 = numpy.ma.array(r7, mask=invalidMask7, fill_value=numpy.nan)
    ma10 = numpy.ma.array(r10, mask=invalidMask10, fill_value=numpy.nan)

    print("processing line ", y, " of ", height)
    ndvi = (ma10 - ma7) / (ma10 + ma7)
    ndviBand.writePixels(0, y, width, 1, ndvi.filled(numpy.nan))

ndviProduct.closeIO()

print("Done.")