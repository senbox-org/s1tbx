package org.esa.beam.dataio.modis.bandreader;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;


public class ModisInt16BandReader extends ModisBandReader {

    private short[] _line;

    public ModisInt16BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link org.esa.beam.framework.datamodel.ProductData#TYPE_INT8}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_INT16;
    }

    /**
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     */
    @Override
    public void readBandDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                 int sourceStepX,
                                 int sourceStepY, int destOffsetX, int destOffsetY, int destWidth, int destHeight,
                                 ProductData destBuffer, ProgressMonitor pm) throws HDFException, ProductIOException {
        final short min;
        final short max;
        final short fill = (short) Math.floor(_fillValue + 0.5);
        if (_validRange == null) {
            min = Short.MIN_VALUE;
            max = Short.MAX_VALUE;
        } else {
            min = (short) Math.floor(_validRange.getMin() + 0.5);
            max = (short) Math.floor(_validRange.getMax() + 0.5);
        }

        final short[] targetData = (short[]) destBuffer.getElems();
        int targetIdx = 0;

        ensureLineWidth(sourceWidth);

        pm.beginTask("Reading band '" + getName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                HDF.getWrap().SDreaddata(_sdsId, _start, _stride, _count, _line);
                for (int x = 0; x < sourceWidth; x++) {
                    final short value = _line[x];
                    if (value < min || value > max) {
                        _line[x] = fill;
                    }
                    targetData[targetIdx] = _line[x];
                    ++targetIdx;
                }
                _start[_yCoord] += sourceStepY;
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    /**
     * Makes sure that the scan line buffer is present and of the correct size
     *
     * @param sourceWidth
     */
    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new short[sourceWidth];
        }
    }
}