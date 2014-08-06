package org.esa.beam.dataio.netcdf.util;

import com.bc.ceres.core.Assert;
import org.esa.beam.util.StringUtils;

import java.util.regex.Pattern;

public class VariableNameHelper {

    public static boolean isVariableNameValid(String name) {
        Assert.argument(StringUtils.isNotNullAndNotEmpty(name), "name");
        // copied from nujan sources edu.ucar.ral.nujan.netcdf.NhGroup.checkName()
        return Pattern.matches("^[_a-zA-Z][-_: a-zA-Z0-9]*$", name);
    }

    public static String convertToValidName(String name) {
        Assert.argument(StringUtils.isNotNullAndNotEmpty(name), "name");
        String firstCharExpr = "[_a-zA-Z]";
        char replacementChar = '_';
        StringBuilder sb = new StringBuilder(name);
        if (!Pattern.matches(firstCharExpr, name.substring(0, 1))) {
            sb.setCharAt(0, replacementChar);
        }
        char[] chars = name.toCharArray();
        String subsequentCharExpr = "[-_: a-zA-Z0-9]";
        for (int i = 1; i < chars.length; i++) {
            char aChar = chars[i];
            if (!Pattern.matches(subsequentCharExpr, String.valueOf(aChar))) {
                aChar = '_';
            }
            sb.setCharAt(i, aChar);
        }
        return sb.toString();
    }
}
