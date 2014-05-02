import beampy
import numpy

jpy = beampy.jpy

Rectangle = jpy.get_type('java.awt.Rectangle')
Tile = jpy.get_type('org.esa.beam.framework.gpf.Tile')

class MerisNdviTileComputer:

    def initialize(self, operator):

        merisProduct = operator.getSourceProduct('source')
        print('initialize: source product is', merisProduct.getFileLocation())

        width = merisProduct.getSceneRasterWidth()
        height = merisProduct.getSceneRasterHeight()

        self.lowFactor = operator.getParameter('lowerFactor')
        self.lowerBandName = operator.getParameter('lowerName')

        self.upperFactor = operator.getParameter('upperFactor')
        self.upperBandName = operator.getParameter('upperName')

        self.lowerBand = self.getBand(merisProduct, self.lowerBandName)
        self.upperBand = self.getBand(merisProduct, self.upperBandName)

        ndviProduct = beampy.Product('pyNDVI', 'pyNDVI', width, height)
        self.ndviBand = ndviProduct.addBand('ndvi', beampy.ProductData.TYPE_FLOAT32)
        self.ndviFlagsBand = ndviProduct.addBand('ndvi_flags', beampy.ProductData.TYPE_UINT8)

        operator.setTargetProduct(ndviProduct)

    def compute(self, operator, targetTiles, targetRectangle):

        lowerTile = operator.getSourceTile(self.lowerBand, targetRectangle)
        upperTile = operator.getSourceTile(self.upperBand, targetRectangle)

        ndviTile = targetTiles.get(self.ndviBand)
        ndviFlagsTile = targetTiles.get(self.ndviFlagsBand)

        b7Data = lowerTile.getSamplesFloat()
        b10Data = upperTile.getSamplesFloat()

        r7 = numpy.array(b7Data, dtype=numpy.float32)
        r10 = numpy.array(b10Data, dtype=numpy.float32)

        ndvi = (r10 - r7) / (r10 + r7)

        ndviLow = ndvi < 0.0
        ndviHigh = ndvi > 0.1
        ndviFlags = ndviLow + 2 * ndviHigh

        ndviTile.setSamples(ndvi)
        ndviFlagsTile.setSamples(ndviFlags)


    def getBand(self, merisProduct, bandName):
        band = merisProduct.getBand(bandName)
        if not band:
            raise RuntimeError('Product has not a band with name', bandName)
        return band

    def dispose(self, operator):
        pass
