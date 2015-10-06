package org.esa.snap.core.image;

import java.awt.image.DataBuffer;

/**
 * Only used by the classes {@link ReplaceValueOpImage} and {@link ReplaceValueOpImage}
 *
 * @author Marco Peters
 */
class DataBufferUtils {

    static int getDataBufferType(Number value) {
        if (value instanceof Byte) {
            return DataBuffer.TYPE_BYTE;
        }
        if (value instanceof Short) {
            return DataBuffer.TYPE_SHORT;
        }
        if (value instanceof Integer) {
            return DataBuffer.TYPE_INT;
        }
        if (value instanceof Float) {
            return DataBuffer.TYPE_FLOAT;
        }
        // for double, long and all others
        return DataBuffer.TYPE_DOUBLE;
    }

}
