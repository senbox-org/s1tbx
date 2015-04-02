import beampy
import numpy


class NdviOp:

    def __init__(self):
        #jpy = beampy.jpy
        #jpy.diag.flags = jpy.diag.F_ALL
        self.lower_band = None
        self.upper_band = None
        self.ndvi_band = None
        self.ndvi_flags_band = None
        self.lower_factor = 0.0
        self.upper_factor = 0.0

    def initialize(self, operator):
        source_product = operator.getSourceProduct('source')
        print('initialize: source product location is', source_product.getFileLocation())

        width = source_product.getSceneRasterWidth()
        height = source_product.getSceneRasterHeight()

        lower_band_name = operator.getParameter('lowerName')
        if not lower_band_name:
            raise RuntimeError('Missing parameter "lowerName"')
        self.lower_band = self._get_band(source_product, lower_band_name)
        self.lower_factor = operator.getParameter('lowerFactor')

        upper_band_name = operator.getParameter('upperName')
        if not upper_band_name:
            raise RuntimeError('Missing parameter "upperName"')
        self.upper_band = self._get_band(source_product, upper_band_name)
        self.upper_factor = operator.getParameter('upperFactor')

        print('initialize: lower_band =', self.lower_band, ', upper_band =', self.upper_band)
        print('initialize: lower_factor =', self.lower_factor, ', upper_factor =', self.upper_factor)

        ndvi_product = beampy.Product('py_NDVI', 'py_NDVI', width, height)
        self.ndvi_band = ndvi_product.addBand('ndvi', beampy.ProductData.TYPE_FLOAT32)
        self.ndvi_flags_band = ndvi_product.addBand('ndvi_flags', beampy.ProductData.TYPE_UINT8)

        operator.setTargetProduct(ndvi_product)

    def compute(self, operator, target_tiles, target_rectangle):
        lower_tile = operator.getSourceTile(self.lower_band, target_rectangle)
        upper_tile = operator.getSourceTile(self.upper_band, target_rectangle)

        ndvi_tile = target_tiles.get(self.ndvi_band)
        ndvi_flags_tile = target_tiles.get(self.ndvi_flags_band)

        lower_samples = lower_tile.getSamplesFloat()
        upper_samples = upper_tile.getSamplesFloat()

        print('compute: lower_samples =', lower_samples[0], ', upper_samples =', upper_samples[0])

        lower_data = numpy.array(lower_samples, dtype=numpy.float32) * self.lower_factor
        upper_data = numpy.array(upper_samples, dtype=numpy.float32) * self.upper_factor

        print('compute: lower_data =', lower_data[0], ', upper_data =', upper_data[0])

        ndvi = (upper_data - lower_data ) / (upper_data + lower_data )

        ndvi_low = ndvi < 0.0
        ndvi_high = ndvi > 0.1
        ndvi_flags = (ndvi_low + 2 * ndvi_high).astype(numpy.uint8)

        ndvi_tile.setSamples(ndvi)
        ndvi_flags_tile.setSamples(ndvi_flags)

    def dispose(self, operator):
        pass

    def _get_band(self, product, name):
        band = product.getBand(name)
        if not band:
            raise RuntimeError('Product does not contain a band named', name)
        return band
