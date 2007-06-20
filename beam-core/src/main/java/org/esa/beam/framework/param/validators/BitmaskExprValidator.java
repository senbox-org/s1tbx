/*
 * $Id: BitmaskExprValidator.java,v 1.1.1.1 2006/09/11 08:16:46 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.param.validators;

import org.esa.beam.framework.dataop.bitmask.BitmaskExpressionParseException;
import org.esa.beam.framework.dataop.bitmask.BitmaskExpressionParser;
import org.esa.beam.framework.param.ParamConstants;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Debug;


/**
 * Validates boolean expressions.
 * @deprecated use {@link BooleanExpressionValidator} instead
 */
public class BitmaskExprValidator extends StringValidator {

    public BitmaskExprValidator() {
    }

    /**
     *
     * @param parameter
     * @param text
     * @return the parsed text
     * @throws ParamParseException
     */
    public Object parse(final Parameter parameter, final String text) throws ParamParseException {

        Debug.assertTrue(text != null);

        if (isAllowedNullText(parameter, text)) {
            return null;
        }

        try {
            BitmaskExpressionParser.parse(text);
        } catch (BitmaskExpressionParseException e) {
            throw new ParamParseException(parameter, ParamConstants.ERR_MSG_INVALID_BITMASK + e.getMessage());
        }

        // Just return text.
        return text;
    }

    /**
     *
     * @param parameter
     * @param value1
     * @param value2
     * @return <code>true</code>, if <code>value1</code> and <code>value2</code> are equal otherwise <code>false</code>.
     */
    public boolean equalValues(final Parameter parameter, final Object value1, final Object value2) {
        return equalValues(false, value1, value2);
    }
}
