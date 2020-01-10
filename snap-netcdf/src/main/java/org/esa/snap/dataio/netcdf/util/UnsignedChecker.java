package org.esa.snap.dataio.netcdf.util;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

public class UnsignedChecker {

    public static void setUnsignedType(Variable variable) {
        Attribute isUnsignedAttribute = variable.findAttributeIgnoreCase("_unsigned");
        boolean isUnsigned = isUnsignedAttribute != null && isUnsignedAttribute.getStringValue().equalsIgnoreCase("true");
        if (isUnsigned) {
            variable.setDataType(variable.getDataType().withSignedness(DataType.Signedness.UNSIGNED));
            variable.removeAttribute("_Unsigned");
        }
        if (variable.getDataType().isUnsigned()) {
            variable.setDataType(variable.getDataType().withSignedness(DataType.Signedness.UNSIGNED));
            variable.removeAttribute("_Unsigned");
        }
    }
}
