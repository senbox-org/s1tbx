package org.esa.beam.dataio.envi;

import org.esa.beam.framework.datamodel.ProductData;

class DataTypeUtils {

    private DataTypeUtils() {
    }

    static int toBeam(int enviTypeId) {
        if (EnviConstants.TYPE_ID_BYTE == enviTypeId) {
            return ProductData.TYPE_UINT8;
        } else if (EnviConstants.TYPE_ID_INT16 == enviTypeId) {
            return ProductData.TYPE_INT16;
        } else if (EnviConstants.TYPE_ID_INT32 == enviTypeId) {
            return ProductData.TYPE_INT32;
        } else if (EnviConstants.TYPE_ID_FLOAT32 == enviTypeId) {
            return ProductData.TYPE_FLOAT32;
        } else if (EnviConstants.TYPE_ID_FLOAT64 == enviTypeId) {
            return ProductData.TYPE_FLOAT64;
        } else if (EnviConstants.TYPE_ID_UINT16 == enviTypeId) {
            return ProductData.TYPE_UINT16;
        } else if (EnviConstants.TYPE_ID_UINT32 == enviTypeId) {
            return ProductData.TYPE_UINT32;
        }
        return ProductData.TYPE_UNDEFINED;
    }

    static int getSizeInBytes(int enviTypeId) {
        if (EnviConstants.TYPE_ID_BYTE == enviTypeId) {
            return 1;
        } else if (EnviConstants.TYPE_ID_INT16 == enviTypeId ||
                   EnviConstants.TYPE_ID_UINT16 == enviTypeId) {
            return 2;
        } else if (EnviConstants.TYPE_ID_INT32 == enviTypeId ||
                   EnviConstants.TYPE_ID_FLOAT32 == enviTypeId ||
                   EnviConstants.TYPE_ID_UINT32 == enviTypeId) {
            return 4;
        } else if (EnviConstants.TYPE_ID_FLOAT64 == enviTypeId) {
            return 8;
        }
        return -1;
    }
}
