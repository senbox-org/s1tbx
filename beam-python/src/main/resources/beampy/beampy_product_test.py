import unittest
import array

import numpy as np

import beampy


JAI = beampy.jpy.get_type('javax.media.jai.JAI')

JAI.getDefaultInstance().getTileCache().setMemoryCapacity(128 * 1000 * 1000)


test_product_file = './MER_RR__1P.N1'


expected_a100 = 89.462776


class TestBeamIO(unittest.TestCase):

    def setUp(self):
        self.product = beampy.ProductIO.readProduct(test_product_file)
        self.assertIsNotNone(self.product)


    def tearDown(self):
        self.product.dispose()


    def test_getProductReader(self):
        #print('Band.mro =', beampy.Band.mro())
        reader = self.product.getProductReader()
        self.assertIsNotNone(reader)
        # TODO: fix me!
        #print('ProductReader.mro =', type(reader).mro())
        #readerClass = reader.getClass()
        #self.assertEqual(readerClass.getName(), '??')


    def test_getBandNames(self):
        names = self.product.getBandNames()
        self.assertEqual(len(names), 17)
        self.assertEqual(names[0], 'radiance_1')
        self.assertEqual(names[14], 'radiance_15')
        self.assertEqual(names[15], 'l1_flags')
        self.assertEqual(names[16], 'detector_index')


    def test_getSceneRasterWidthAndHeight(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        self.assertEqual(w, 1121)
        self.assertEqual(h,  705)


    def test_readPixels_with_java_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = beampy.jpy.array('float', w)
        b.readPixels(0, 0, w, 1, a)
        self.assertAlmostEqual(a[0], 0.0, places=5)
        self.assertAlmostEqual(a[100], expected_a100, places=5)


    def test_readPixels_with_python_array(self):
        if sys.version >= (3, 0,):
            w = self.product.getSceneRasterWidth()
            h = self.product.getSceneRasterHeight()
            b = self.product.getBand('radiance_13')
            a = array.array('f', w * [0])
            b.readPixels(0, 0, w, 1, a)
            self.assertAlmostEqual(a[0], 0.0, places=5)
            self.assertAlmostEqual(a[100], expected_a100, places=5)
        else:
            print("Test skipped as the Python 2.7 array type does not support the new buffer interface")


    def test_readPixels_with_numpy_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.float32)
        b.readPixels(0, 0, w, 1, a)
        self.assertAlmostEqual(a[0], 0.0, places=5)
        self.assertAlmostEqual(a[100], expected_a100, places=5)


    def test_readValidMask_with_numpy_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.bool)
        b.readValidMask(0, 0, w, 1, a)
        self.assertEqual(a[0], 0)
        self.assertEqual(a[100], 1)



if __name__ == '__main__':
    unittest.main()
