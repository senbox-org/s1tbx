
import beampy

Operator = beampy.jpy.get_class('org.esa.beam.framework.gpf.Operator')

class BiboTileComputer:

    def __init__(self):
        print('__init__')
        pass

    def initialize(self, operator):
        print('initialize: operator =', operator)

        sp = operator.getSourceProduct()
        print('sp =', sp.getName())

        p = beampy.Product('bibo', 'bibo_type', 600, 200)
        p.addBand('b1', beampy.ProductData.TYPE_FLOAT32)
        p.addBand('b2', beampy.ProductData.TYPE_FLOAT32)
        operator.setTargetProduct(p)

    def computeTile(self, operator, targetBand, targetTile):
        print('computeTile: operator =', operator, 'targetBand =', targetBand.getName())

    def dispose(self, operator):
        print('dispose: operator =', operator)
