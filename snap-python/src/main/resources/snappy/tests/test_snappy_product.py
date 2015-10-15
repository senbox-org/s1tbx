import unittest
import array
import sys

import numpy as np

import snappy

JAI = snappy.jpy.get_type('javax.media.jai.JAI')
JAI.getDefaultInstance().getTileCache().setMemoryCapacity(128 * 1000 * 1000)

test_product_file = './MER_RR__1P.N1'

class TestBeamIO(unittest.TestCase):

    def setUp(self):
        self.product = snappy.ProductIO.readProduct(test_product_file)
        self.assertIsNotNone(self.product)


    def tearDown(self):
        self.product.dispose()


    def test_getProductReader(self):
        #print('Band.mro =', snappy.Band.mro())
        reader = self.product.getProductReader()
        self.assertIsNotNone(reader)
        # TODO: fix me: AttributeError: 'org.esa.snap.core.dataio.ProductReader' object has no attribute 'getClass'
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
        self.assertTrue(h > 0)


    def test_readPixels_with_java_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = snappy.jpy.array('float', w)
        b.readPixels(0, 0, w, 1, a)
        self.assertTrue(a[0] == 0.0)
        self.assertTrue(0 < a[100] < 200)


    def test_readPixels_with_python_array(self):
        if sys.version_info < (3, 0,):
            # Test only on Python 3.x, as the 2.x array type does not support the new buffer interface
            return
            
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = array.array('f', w * [0])
        b.readPixels(0, 0, w, 1, a)
        self.assertTrue(a[0] == 0.0)
        self.assertTrue(0 < a[100] < 200)


    def test_readPixels_with_numpy_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.float32)
        b.readPixels(0, 0, w, 1, a)
        self.assertTrue(a[0] == 0.0)
        self.assertTrue(0 < a[100] < 200)


    def test_readValidMask_with_numpy_array(self):
        w = self.product.getSceneRasterWidth()
        h = self.product.getSceneRasterHeight()
        b = self.product.getBand('radiance_13')
        a = np.zeros(w, dtype=np.int8)
        #snappy.jpy.diag.flags = snappy.jpy.diag.F_ALL
        b.readValidMask(0, 0, w, 1, a)
        #snappy.jpy.diag.flags = snappy.jpy.diag.F_OFF
        self.assertEqual(a[0], 0)
        self.assertEqual(a[100], 1)


if __name__ == '__main__':
    unittest.main()
