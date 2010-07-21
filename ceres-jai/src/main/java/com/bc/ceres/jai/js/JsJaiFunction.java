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

package com.bc.ceres.jai.js;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ParameterListDescriptor;

public class JsJaiFunction extends ScriptableObject implements Function {

    private final OperationDescriptor operationDescriptor;
    private final String name;

    public JsJaiFunction(OperationDescriptor operationDescriptor) {
        super();
        this.operationDescriptor = operationDescriptor;
        this.name = operationDescriptor.getName().toLowerCase();
    }

    public JsJaiFunction(Scriptable scope, Scriptable prototype, OperationDescriptor operationDescriptor) {
        super(scope, prototype);
        this.operationDescriptor = operationDescriptor;
        this.name = operationDescriptor.getName().toLowerCase();
    }

    public OperationDescriptor getOperationDescriptor() {
        return operationDescriptor;
    }

    public String getClassName() {
        return name;
    }

    public Scriptable construct(Context context, Scriptable scope, Object[] args) {
        return new JsJaiFunction(operationDescriptor);
    }

    public Object call(Context context, Scriptable scope, Scriptable thisObj, Object[] args) {
        final ParameterBlockJAI pb = new ParameterBlockJAI(operationDescriptor);
        final int numSources = operationDescriptor.getNumSources();
        final ParameterListDescriptor pld = operationDescriptor.getParameterListDescriptor("rendered");
        final int numParams = pld.getNumParameters();
        final String[] paramNames = pld.getParamNames();
        final Class[] paramTypes = pld.getParamClasses();
        try {
            for (int i = 0; i < numSources; i++) {
                if (i < args.length) {
                    pb.addSource(args[i]);
                } else {
                    final String[] sourceNames = operationDescriptor.getSourceNames();
                    throw new IllegalArgumentException("Missing argument #" + (i + 1) + " (" + sourceNames[i] + ")");
                }
            }
            for (int i = 0; i < numParams; i++) {
                final String paramName = paramNames[i];
                final Class paramType = paramTypes[i];
                final Object paramValue;
                final int j = numSources + i;
                if (j < args.length) {
                    paramValue = convertJsToJaiValue(args[j], paramType);
                } else {
                    paramValue = pld.getParamDefaultValue(paramName);
                    if (paramValue == ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
                        throw new IllegalArgumentException("Missing argument #" + (j + 1) + " (" + paramName + ")");
                    }
                }
                pb.setParameter(paramName, paramValue);
            }
            return JAI.create(getClassName(), pb);
        } catch (IllegalArgumentException t) {
            throw new WrappedException(t);
        }
    }

    private Object convertJsToJaiValue(Object arg, Class paramType) {
        if (Number.class.isAssignableFrom(paramType)) {
            if (paramType == Byte.class) {
                return (byte) Context.toNumber(arg);
            } else if (paramType == Short.class) {
                return (short) Context.toNumber(arg);
            } else if (paramType == Integer.class) {
                return (int) Context.toNumber(arg);
            } else if (paramType == Long.class) {
                return (long) Context.toNumber(arg);
            } else if (paramType == Float.class) {
                return (float) Context.toNumber(arg);
            } else {
                return Context.toNumber(arg);
            }
        } else if (Boolean.class.isAssignableFrom(paramType)) {
            return Context.toBoolean(arg);
        } else if (String.class.isAssignableFrom(paramType)) {
            return Context.toString(arg);
        } else {
            return Context.jsToJava(arg, paramType);
        }
    }

    @Override
    public Object getDefaultValue(Class aClass) {
        if (aClass == String.class) {
            return "[" + getClassName() + "]";
        } else if (aClass == Boolean.class) {
            return false;
        } else if (aClass == Number.class) {
            return 0;
        } else if (aClass == Scriptable.class) {
            return this;
        } else {
            return null;
        }
    }
}
