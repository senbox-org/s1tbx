import sys
import beampy
from beampy import ProductIO
from beampy import Product
from beampy import Band

if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]

jpy = beampy.jpy

# Uncomment if you receive errors of type com.sun.media.jai.util.ServiceConfigurationError, see
# http://www.brockmann-consult.de/beam-jira/browse/BEAM-1699
#beampy.SystemUtils.init3rdPartyLibs(None)  # Initialise BEAM's third party Java libraries JAI and GeoTools.

# More Java type definitions required for image generation
Color = jpy.get_type('java.awt.Color')
ColorPoint = jpy.get_type('org.esa.beam.framework.datamodel.ColorPaletteDef$Point')
ColorPaletteDef = jpy.get_type('org.esa.beam.framework.datamodel.ColorPaletteDef')
ImageInfo = jpy.get_type('org.esa.beam.framework.datamodel.ImageInfo')
ImageManager = jpy.get_type('org.esa.beam.jai.ImageManager')
JAI = jpy.get_type('javax.media.jai.JAI')

# Disable JAI native MediaLib extensions 
System = jpy.get_type('java.lang.System')
System.setProperty('com.sun.media.jai.disableMediaLib', 'true')

def write_image(band, points, filename, format):
    cpd = ColorPaletteDef(points)
    ii = ImageInfo(cpd)
    # this following line is actually superfluous, but due to a bug in BEAM is must be like that
    band.setImageInfo(ii) 
    im = ImageManager.getInstance().createColoredBandImage([band], band.getImageInfo(), 0)
    JAI.create("filestore", im, filename, format)

product = ProductIO.readProduct(file)
band = product.getBand('radiance_13')

# The colour palette assigned to pixel values 0, 50, 100 in the band's geophysical units
points = [ColorPoint(0.0, Color.YELLOW), 
          ColorPoint(50.0, Color.RED), 
          ColorPoint(100.0, Color.BLUE)]

write_image(band, points, 'beampy_write_image.png', 'PNG')