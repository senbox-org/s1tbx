package org.esa.snap.core.gpf.common.resample;

import javax.media.jai.RasterAccessor;
import java.awt.image.DataBuffer;

/**
 * @author Tonio Fincke
 */
public class DataAccessorFactory {

    public static LongDataAccessor createLongDataAccessor(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                return new LongDataAccessor.Byte(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_SHORT:
                return new LongDataAccessor.Short(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_USHORT:
                return new LongDataAccessor.UShort(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_INT:
                return new LongDataAccessor.Int(srcAccessor, dstAccessor, noDataValue);
        }
        throw new IllegalArgumentException("Datatype not supported");
    }

    public static DoubleDataAccessor createDoubleDataAccessor(RasterAccessor srcAccessor, RasterAccessor dstAccessor, double noDataValue) {
        switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_FLOAT:
                return new DoubleDataAccessor.Float(srcAccessor, dstAccessor, noDataValue);
            case DataBuffer.TYPE_DOUBLE:
                return new DoubleDataAccessor.Double(srcAccessor, dstAccessor, noDataValue);
        }
        throw new IllegalArgumentException("Datatype not supported");
    }

}
