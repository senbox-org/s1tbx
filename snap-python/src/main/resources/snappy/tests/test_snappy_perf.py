import unittest

import numpy as np

import snappy


#JAI = snappy.jpy.get_type('javax.media.jai.JAI')
#JAI.getDefaultInstance().getTileCache().setMemoryCapacity(128 * 1000 * 1000)

test_product_file = './MER_RR__1P.N1'


class TestBeamIO(unittest.TestCase):

    def setUp(self):
        self.product = snappy.ProductIO.readProduct(test_product_file)
        self.assertIsNotNone(self.product)


    def tearDown(self):
        self.product.dispose()


    def test_readPixels_performance(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.float32)

        import time

        t0 = time.time()
        for y in range(h):
            b.readPixels(0, 0, w, 1, a)
        t1 = time.time()

        dt = t1 - t0
        print('Band.readPixels(): w =', w, ', dtype=np.float32:', h, 'calls in', dt*1000, 'ms, that is ', dt*1000/y, 'ms per call')


    def test_readValidMask_performance(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.bool)

        import time

        t0 = time.time()
        for y in range(h):
            b.readValidMask(0, 0, w, 1, a)
        t1 = time.time()

        dt = t1 - t0
        print('Band.readValidMask(): w =', w, ', dtype=np.bool:', h, 'calls in', dt*1000, 'ms, that is ', dt*1000/y, 'ms per call')


if __name__ == '__main__':
    unittest.main()
