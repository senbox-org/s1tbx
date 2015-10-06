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
package org.esa.snap.core.param;

import org.esa.snap.core.param.validators.BooleanValidator;
import org.esa.snap.core.param.validators.ColorValidator;
import org.esa.snap.core.param.validators.FileValidator;
import org.esa.snap.core.param.validators.NumberValidator;
import org.esa.snap.core.param.validators.StringArrayValidator;
import org.esa.snap.core.param.validators.StringValidator;

import java.util.Map;

/**
 * A <code>ParamValidatorRegistry</code> stores the different validators for each of the different parameter types.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ParamValidatorRegistry {

    private static Map _validators = new java.util.Hashtable();

    static {
        registerValidator(java.lang.Short.class, new NumberValidator());
        registerValidator(java.lang.Integer.class, new NumberValidator());
        registerValidator(java.lang.Long.class, new NumberValidator());
        registerValidator(java.lang.Float.class, new NumberValidator());
        registerValidator(java.lang.Double.class, new NumberValidator());
        registerValidator(java.lang.String.class, new StringValidator());
        registerValidator(java.lang.String[].class, new StringArrayValidator());
        registerValidator(java.lang.Boolean.class, new BooleanValidator());
        registerValidator(java.awt.Color.class, new ColorValidator());
        registerValidator(java.io.File.class, new FileValidator());
    }


    /**
     * Returns the default validator, which is guaranteed to be different from <code>null</code>. The method first look
     * for a validator registred for the <code>String</code> class, if it is not found then a new instance of
     * <code>StringValidator</code> is returned.
     *
     * @see #getValidator(Class)
     */
    public static ParamValidator getDefaultValidator() {
        ParamValidator validator = getValidator(java.lang.String.class);
        if (validator == null) {
            validator = new StringValidator();
        }
        return validator;
    }

    /**
     * Returns a validator for the given value type, which is guaranteed to be different from <code>null</code>. <p> If
     * given value type is <code>null</code>, the method returns the value of <code>getDefaultValidator()</code>.
     *
     * @see #getDefaultValidator()
     */
    public static ParamValidator getValidator(Class valueType) {
        return valueType != null
               ? (ParamValidator) _validators.get(valueType)
               : getDefaultValidator();
    }

    public static void registerValidator(Class valueType, ParamValidator validator) {
        _validators.put(valueType, validator);
    }

    public static boolean deregisterValidator(Class valueType) {
        return _validators.remove(valueType) != null;
    }

    /**
     * Private constructor. Used to prevent instantiation of this class.
     */
    private ParamValidatorRegistry() {
    }
}




