package org.esa.beam.dataio.obpg.bandreader;

import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.obpg.hdf.lib.HDF;
import org.esa.beam.framework.datamodel.ProductData;

public class ObpgInt32BandReader extends ObpgBandReader {

    private int[] _line;
    private int min;
    private int max;
    private int fill;
    private int[] targetData;
    private int targetIdx;

    public ObpgInt32BandReader(final int sdsId, final int layer, final boolean is3d) {
        super(sdsId, layer, is3d);
    }

    /**
     * Retrieves the data type of the band
     *
     * @return always {@link org.esa.beam.framework.datamodel.ProductData#TYPE_INT8}
     */
    @Override
    public int getDataType() {
        return ProductData.TYPE_INT32;
    }

    @Override
    protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY, final int sourceWidth,
                                     final int sourceHeight, final int sourceStepX, final int sourceStepY,
                                     final ProductData destBuffer) {
        fill = (int) Math.round(_fillValue);
        if (_validRange == null) {
            min = Integer.MIN_VALUE;
            max = Integer.MAX_VALUE;
        } else {
            min = (int) Math.round(_validRange.getMin());
            max = (int) Math.round(_validRange.getMax());
        }
        targetData = (int[]) destBuffer.getElems();
        targetIdx = 0;
        ensureLineWidth(sourceWidth);
    }

    @Override
    protected void readLine() throws HDFException {
        HDF.getInstance().SDreaddata(_sdsId, _start, _stride, _count, _line);
    }

    @Override
    protected void validate(final int x) {
        final int value = _line[x];
        if (value < min || value > max) {
            _line[x] = fill;
        }
    }

    @Override
    protected void assign(final int x) {
        targetData[targetIdx++] = _line[x];
    }

    private void ensureLineWidth(final int sourceWidth) {
        if ((_line == null) || (_line.length != sourceWidth)) {
            _line = new int[sourceWidth];
        }
    }
}