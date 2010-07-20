package org.esa.beam.dataio.netcdf.util;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.logging.BeamLogManager;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides some NetCDF related utility methods.
 */
public class ReaderUtils {

    public static boolean isValidRasterDataType(final DataType dataType) {
        final boolean unsigned = false;
        return getRasterDataType(dataType, unsigned) != -1;
    }

    public static int getRasterDataType(final DataType dataType, boolean unsigned) {
        final boolean rasterDataOnly = true;
        return getEquivalentProductDataType(dataType, unsigned, rasterDataOnly);
    }

    public static int getEquivalentProductDataType(DataType dataType, boolean unsigned, boolean rasterDataOnly) {
        if (dataType == DataType.BYTE) {
            return unsigned ? ProductData.TYPE_UINT8 : ProductData.TYPE_INT8;
        } else if (dataType == DataType.SHORT) {
            return unsigned ? ProductData.TYPE_UINT16 : ProductData.TYPE_INT16;
        } else if (dataType == DataType.INT) {
            return unsigned ? ProductData.TYPE_UINT32 : ProductData.TYPE_INT32;
        } else if (dataType == DataType.FLOAT) {
            return ProductData.TYPE_FLOAT32;
        } else if (dataType == DataType.DOUBLE) {
            return ProductData.TYPE_FLOAT64;
        } else if (!rasterDataOnly) {
            if (dataType == DataType.CHAR) {
                return ProductData.TYPE_ASCII;
            } else if (dataType == DataType.STRING) {
                return ProductData.TYPE_ASCII;
            }
        }
        return -1;
    }

    public static ProductData createProductData(int productDataType, Array values) {
        Object data = values.getStorage();
        if (data instanceof char[]) {
            data = new String((char[]) data);
        }
        return ProductData.createInstance(productDataType, data);
    }

    public static ProductData.UTC getSceneRasterTime(NetcdfFile ncFile,
                                                     final String dateAttrName,
                                                     final String timeAttrName) {
        final Attribute dateAttr = ncFile.findGlobalAttribute(dateAttrName);
        final Attribute timeAttr = ncFile.findGlobalAttribute(timeAttrName);
        final String dateTimeStr = getDateTimeString(dateAttr, timeAttr);

        if (dateTimeStr != null) {
            return parseDateTime(dateTimeStr);
        }

        return null;
    }

    public static String getDateTimeString(Attribute dateAttr, Attribute timeAttr) {
        String date = dateAttr != null ? dateAttr.getStringValue() : null;
        String time = timeAttr != null ? timeAttr.getStringValue() : null;
        if (date != null && date.endsWith("UTC")) {
            date = date.substring(0, date.length() - 3).trim();
        }
        if (time != null && time.endsWith("UTC")) {
            time = time.substring(0, time.length() - 3).trim();
        }
        if (date != null && time != null) {
            return date + " " + time;
        }
        if (date != null) {
            return date + (date.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        if (time != null) {
            return time + (time.indexOf(':') == -1 ? " 00:00:00" : "");
        }
        return null;
    }

    public static ProductData.UTC parseDateTime(String dateTimeStr)  {
        try {
            return ProductData.UTC.parse(dateTimeStr);
        } catch (ParseException ignore) {
            try {
                return ProductData.UTC.parse(dateTimeStr, Constants.DATE_TIME_PATTERN);
            } catch (ParseException ignore2) {
                BeamLogManager.getSystemLogger().warning(
                        "Failed to parse time string '" + dateTimeStr + "'");
                return null;
            }
        }
    }

    public static boolean hasValidExtension(String pathname) {
        final String lowerPath = pathname.toLowerCase();
        final String[] validExtensions = Constants.FILE_EXTENSIONS;
        for (String validExtension : validExtensions) {
            validExtension = validExtension.toLowerCase();
            if (lowerPath.endsWith(validExtension)) {
                return true;
            }
        }
        return false;
    }

    public static Variable[] getVariables(List<Variable> variables, String[] names) {
        if (variables == null || names == null) {
            return null;
        }
        if (variables.size() < names.length) {
            return null;
        }
        final Variable[] result = new Variable[names.length];
        for (int i = 0; i < names.length; i++) {
            final String name = names[i];
            for (Variable variable : variables) {
                if (variable.getName().equalsIgnoreCase(name)) {
                    result[i] = variable;
                    break;
                }
            }
            if (result[i] == null) {
                return null;
            }
        }
        return result;
    }

    public static Map<String, Variable> createVariablesMap(List<Variable> globalVariables) {
        final HashMap<String, Variable> map = new HashMap<String, Variable>();
        for (Variable variable : globalVariables) {
            map.put(variable.getName(), variable);
        }
        return map;
    }

    public static boolean allElementsAreNotNull(final Object[] array) {
        if (array != null) {
            for (Object o : array) {
                if (o == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean allAttributesAreNotNullAndHaveTheSameSize(final Attribute[] attributes) {
        if (!allElementsAreNotNull(attributes)) {
            return false;
        } else {
            final Attribute prim = attributes[0];
            for (int i = 1; i < attributes.length; i++) {
                if (prim.getLength() != attributes[i].getLength()) {
                    return false;
                }
            }
        }
        return true;
    }
}

