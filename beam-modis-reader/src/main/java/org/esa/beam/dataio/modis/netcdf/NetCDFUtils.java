package org.esa.beam.dataio.modis.netcdf;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.ProductData;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

public class NetCDFUtils {

    public static String getNamedStringAttribute(String bandNameAttribName, NetCDFAttributes attributes) {
        String attributeValue = "";
        final Attribute attribute = attributes.get(bandNameAttribName);
        if (attribute != null) {
            attributeValue = attribute.getStringValue();
        }
        return attributeValue;
    }

    public static MetadataAttribute toMetadataAttribute(Attribute attribute) {
        MetadataAttribute attrib = null;
        final ProductData prodData;

        final DataType dataType = attribute.getDataType();

        switch (dataType) {
            case STRING:
                prodData = ProductData.createInstance(attribute.getStringValue());
                break;

            case BYTE:
            case INT:
                final int[] intValues = getIntValues(attribute);
                prodData = ProductData.createInstance(intValues);
                break;

            case FLOAT:
                final float[] floatValues = getFloatValues(attribute);
                prodData = ProductData.createInstance(floatValues);
                break;

            case DOUBLE:
                final double[] doubleValues = getDoubleValues(attribute);
                prodData = ProductData.createInstance(doubleValues);
                break;
            default:
                System.out.println("dataType = " + dataType);
                throw new NotImplementedException();
        }

        if (prodData != null) {
            attrib = new MetadataAttribute(attribute.getShortName(), prodData, true);
        }

        return attrib;
    }

    public static MetadataAttribute toMetadataAttribute(Variable variable) throws IOException {
        final String variableValue = variable.readScalarString();
        ProductData prodData = ProductData.createInstance(variableValue);

        return new MetadataAttribute(variable.getFullName(), prodData, true);
    }

    public static float[] getFloatValues(Attribute attribute) {
        final Array values = attribute.getValues();
        final long size = values.getSize();
        final float[] result = new float[(int) size];
        for (int i = 0; i < size; i++) {
            result[i] = values.getFloat(i);
        }
        return result;
    }

    public static double[] getDoubleValues(Attribute attribute) {
        final Array values = attribute.getValues();
        final long size = values.getSize();
        final double[] result = new double[(int) size];
        for (int i = 0; i < size; i++) {
            result[i] = values.getDouble(i);
        }
        return result;
    }

    public static int[] getIntValues(Attribute attribute) {
        final Array values = attribute.getValues();
        final long size = values.getSize();
        final int[] result = new int[(int) size];
        for (int i = 0; i < size; i++) {
            result[i] = values.getInt(i);
        }
        return result;
    }
}
