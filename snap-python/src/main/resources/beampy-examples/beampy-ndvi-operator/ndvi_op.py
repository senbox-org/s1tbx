import beampy
import numpy

jpy = beampy.jpy


class NdviComputer:
    def initialize(self, operator):
        sourceProduct = operator.getSourceProduct('source')
        print('initialize: source product is', sourceProduct.getFileLocation())

        width = sourceProduct.getSceneRasterWidth()
        height = sourceProduct.getSceneRasterHeight()

        self.lowerFactor = operator.getParameter('lowerFactor')
        self.lowerBandName = operator.getParameter('lowerName')
        self.lowerBand = self.getBand(sourceProduct, self.lowerBandName)

        self.upperFactor = operator.getParameter('upperFactor')
        self.upperBandName = operator.getParameter('upperName')
        self.upperBand = self.getBand(sourceProduct, self.upperBandName)

        ndviProduct = beampy.Product('pyNDVI', 'pyNDVI', width, height)
        self.ndviBand = ndviProduct.addBand('ndvi', beampy.ProductData.TYPE_FLOAT32)
        self.ndviFlagsBand = ndviProduct.addBand('ndvi_flags', beampy.ProductData.TYPE_UINT8)

        operator.setTargetProduct(ndviProduct)

    def compute(self, operator, targetTiles, targetRectangle):
        lowerTile = operator.getSourceTile(self.lowerBand, targetRectangle)
        upperTile = operator.getSourceTile(self.upperBand, targetRectangle)

        ndviTile = targetTiles.get(self.ndviBand)
        ndviFlagsTile = targetTiles.get(self.ndviFlagsBand)

        lowerSamples = lowerTile.getSamplesFloat()
        upperSamples = upperTile.getSamplesFloat()

        lowerData = numpy.array(lowerSamples, dtype=numpy.float32) * self.lowerFactor
        upperData = numpy.array(upperSamples, dtype=numpy.float32) * self.upperFactor

        ndvi = (upperData - lowerData ) / (upperData + lowerData )

        ndviLow = ndvi < 0.0
        ndviHigh = ndvi > 0.1
        ndviFlags = (ndviLow + 2 * ndviHigh).astype(numpy.uint8)

        ndviTile.setSamples(ndvi)
        ndviFlagsTile.setSamples(ndviFlags)


    def getBand(self, merisProduct, bandName):
        band = merisProduct.getBand(bandName)
        if not band:
            raise RuntimeError('Product has not a band with name', bandName)
        return band

    def dispose(self, operator):
        pass
