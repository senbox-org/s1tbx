import sys

import snappy
from snappy import (ProductIO, ProductUtils, ProgressMonitor)

if len(sys.argv) != 2:
    print("usage: %s <file>" % sys.argv[0])
    sys.exit(1)

file = sys.argv[1]

jpy = snappy.jpy

# More Java type definitions required for image generation
Color = jpy.get_type('java.awt.Color')
ColorPoint = jpy.get_type('org.esa.snap.core.datamodel.ColorPaletteDef$Point')
ColorPaletteDef = jpy.get_type('org.esa.snap.core.datamodel.ColorPaletteDef')
ImageInfo = jpy.get_type('org.esa.snap.core.datamodel.ImageInfo')
ImageLegend = jpy.get_type('org.esa.snap.core.datamodel.ImageLegend')
ImageManager = jpy.get_type('org.esa.snap.core.image.ImageManager')
JAI = jpy.get_type('javax.media.jai.JAI')
RenderedImage = jpy.get_type('java.awt.image.RenderedImage')


# Disable JAI native MediaLib extensions 
System = jpy.get_type('java.lang.System')
System.setProperty('com.sun.media.jai.disableMediaLib', 'true')


def write_image(band, filename, format):
    im = ImageManager.getInstance().createColoredBandImage([band], band.getImageInfo(), 0)
    JAI.create("filestore", im, filename, format)


def write_rgb_image(bands, filename, format):
    image_info = ProductUtils.createImageInfo(bands, True, ProgressMonitor.NULL)
    im = ImageManager.getInstance().createColoredBandImage(bands, image_info, 0)
    JAI.create("filestore", im, filename, format)


def resize(product, targetWidth, targetHeight):
    from snappy import GPF
    from snappy import HashMap

    parameters = HashMap()
    parameters.put('targetWidth', targetWidth)
    parameters.put('targetHeight', targetHeight)
    return GPF.createProduct('Resample', parameters, product)

product = ProductIO.readProduct(file)

# This scales the product to the specified size
# remove if you don't want scaling.
product = resize(product, 1000, 1000)

band = product.getBand('radiance_13')

# The colour palette assigned to pixel values 0, 50, 100 in the band's geophysical units
points = [ColorPoint(0.0, Color.YELLOW), 
          ColorPoint(50.0, Color.RED), 
          ColorPoint(100.0, Color.BLUE)]
cpd = ColorPaletteDef(points)
ii = ImageInfo(cpd)
band.setImageInfo(ii)

image_format = 'PNG'
write_image(band, 'snappy_write_image.png', image_format)

legend = ImageLegend(band.getImageInfo(), band)
legend.setHeaderText(band.getName())

#legend.setOrientation(ImageLegend.HORIZONTAL) # or ImageLegend.VERTICAL
#legend.setFont(legend.getFont().deriveFont(14))
#legend.setBackgroundColor(Color.CYAN)
#legend.setForegroundColor(Color.ORANGE);
#legend.setBackgroundTransparency(0.7);
#legend.setBackgroundTransparencyEnabled(True);
#legend.setAntialiasing(True);

legend_image = legend.createImage()

# This cast is need because otherwise jpy can't evaluate which method to call
# This is considered as an issue of jpy (https://github.com/bcdev/jpy/issues/89)
rendered_legend_image = jpy.cast(legend_image, RenderedImage)
JAI.create("filestore", rendered_legend_image, 'snappy_write_image_legend.png', image_format)

red = product.getBand('radiance_13')
green = product.getBand('radiance_5')
blue = product.getBand('radiance_1')
write_rgb_image([red, green, blue], 'snappy_write_image_rgb.png', image_format)




