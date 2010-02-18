package org.esa.beam.dataio.modis.bandreader;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.IHDFAdapterForMocking;
import org.esa.beam.dataio.modis.hdf.lib.HDFTestCase;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.Range;

public class ModisUint16BandReaderTest extends HDFTestCase {

    public void testRead() throws ProductIOException, HDFException {
        setHdfMock(new IHDFAdapterForMocking() {
            short value = -7;

            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
                    throws HDFException {
                final short[] shorts = (short[]) buffer;
                for (int i = 0; i < shorts.length; i++) {
                    shorts[i] = value;
                    value += 2;
                }
            }
        });

        final ProductData buffer = new ProductData.UShort(12);
        final ModisUint16BandReader reader = new ModisUint16BandReader(3, 2, false);
        final int fill = 999;
        reader.setFillValue(fill);
        reader.setValidRange(new Range(4, Short.MAX_VALUE * 2 - 3));

        // Method under test
        reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);

        final int[] expected = {65529, 65531, fill, fill, fill, fill, 5, 7, 9, 11, 13, 15};
        for (int i = 0; i < expected.length; i++) {
            assertEquals("false at index: " + i + "  ", expected[i], buffer.getElemIntAt(i));
        }
    }

    public void testHDFException() throws ProductIOException, HDFException {
        setHdfMock(new IHDFAdapterForMocking() {

            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
                    throws HDFException {
                throw new HDFException("TestMessage");
            }
        });

        final int sdsId = 3;
        final ProductData buffer = new ProductData.UShort(12);
        final ModisUint16BandReader reader = new ModisUint16BandReader(sdsId, 2, false);

        try {
            // Method under test
            reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);
            fail();
        } catch (HDFException e) {
            assertEquals("TestMessage", e.getMessage());
        } catch (Exception e) {
            fail();
        }
    }
}
