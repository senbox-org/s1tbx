/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.datamodel;

import org.esa.snap.core.param.ParamFormatException;
import org.esa.snap.core.param.ParamParseException;
import org.esa.snap.core.param.ParamValidateException;
import org.esa.snap.core.param.ParamValidator;
import org.esa.snap.core.param.Parameter;
import org.esa.snap.core.util.ObjectUtils;

public class ProductNodeNameValidator implements ParamValidator {

    /**
     * This key should only be specified
     * if the requested productNodeName have to be unique inside the given product.
     * This applies to bandNames and tiePointGridNames.
     */
    public final static String PRODUCT_PROPERTY_KEY = "product";

    private static final String ERR_MSG_NO_NODE_NAME = "Value must be an node name.\n\n "
            + "A valid node name must not start with a dot. Also a valid\n"
            + "node name must not contain any of the following characters\n"
            + "\\/:*?\"<>|";
    private static final String ERR_MSG_DUPLICATE = "The node name must be unique.\n" +
            "This includes 'band names' and 'tie point grid names'.";

    public boolean equalValues(final Parameter parameter, final Object value1, final Object value2) {
        return ObjectUtils.equalObjects(value1, value2);
    }

    public String format(final Parameter parameter, final Object value) throws ParamFormatException {
        return value != null ? (String) value : "";
    }

    public Object parse(final Parameter parameter, final String text) throws ParamParseException {
        return text != null ? text : "";
    }

    public void validate(Parameter parameter, Object value) throws ParamValidateException {
        final String name = (String) value;
        if (!ProductNode.isValidNodeName(name)) {
            throw new ParamValidateException(parameter, ERR_MSG_NO_NODE_NAME); /*I18N*/
        }
        final Product product = (Product) parameter.getProperties().getPropertyValue(PRODUCT_PROPERTY_KEY);
        if (product != null) {
            if (product.containsRasterDataNode(name)) {
                throw new ParamValidateException(parameter, ERR_MSG_DUPLICATE);
            }
        }
    }
}
