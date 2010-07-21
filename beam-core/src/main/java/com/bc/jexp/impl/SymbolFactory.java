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
package com.bc.jexp.impl;

import com.bc.jexp.EvalEnv;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.Variable;

/**
 * A utility class, which can be used to create constants or variables
 * of a primitive boolean or numeric type.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final class SymbolFactory {

    private SymbolFactory() {
    }

    /**
     * Creates a new <code>boolean</code> constant.
     * @param name the constant's name
     * @param value the constant's final value
     * @return a symbol representing the constant, never <code>null</code>
     */
    public static Symbol createConstant(final String name, final boolean value) {
        return new VariableB(name, value);
    }

    /**
     * Creates a new <code>int</code> constant.
     * @param name the constant's name
     * @param value the constant's final value
     * @return a symbol representing the constant, never <code>null</code>
     */
    public static Symbol createConstant(final String name, final int value) {
        return new VariableI(name, value);
    }

    /**
     * Creates a new <code>double</code> constant.
     * @param name the constant's name
     * @param value the constant's final value
     * @return a symbol representing the constant, never <code>null</code>
     */
    public static Symbol createConstant(final String name, final double value) {
        return new VariableD(name, value);
    }

    /**
     * Creates a new <code>boolean</code> variable.
     * @param name the variable's name
     * @param value the variable's initial value
     * @return the variable, never <code>null</code>
     */
    public static Variable createVariable(final String name, final boolean value) {
        return new VariableB(name, value);
    }

    /**
     * Creates a new <code>int</code> variable.
     * @param name the variable's name
     * @param value the variable's initial value
     * @return the variable, never <code>null</code>
     */
    public static Variable createVariable(final String name, final int value) {
        return new VariableI(name, value);
    }

    /**
     * Creates a new <code>double</code> variable.
     * @param name the variable's name
     * @param value the variable's initial value
     * @return the variable, never <code>null</code>
     */
    public static Variable createVariable(final String name, final double value) {
        return new VariableD(name, value);
    }

    private static final class VariableB extends AbstractSymbol.B implements Variable {

        private boolean _value;

        private VariableB(final String name, final boolean value) {
            super(name);
            _value = value;
        }

        public boolean evalB(final EvalEnv env) {
            return _value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            _value = v;
        }

        public void assignI(final EvalEnv env, final int v) {
            _value = Term.toB(v);
        }

        public void assignD(final EvalEnv env, final double v) {
            _value = Term.toB(v);
        }
    }

    private static final class VariableI extends AbstractSymbol.I implements Variable {

        private int _value;

        private VariableI(final String name, final int value) {
            super(name);
            _value = value;
        }

        public int evalI(final EvalEnv env) {
            return _value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            _value = Term.toI(v);
        }

        public void assignI(final EvalEnv env, final int v) {
            _value = v;
        }

        public void assignD(final EvalEnv env, final double v) {
            _value = Term.toI(v);
        }
    }

    private static final class VariableD extends AbstractSymbol.D implements Variable {

        private double _value;

        private VariableD(final String name, final double value) {
            super(name);
            _value = value;
        }

        public double evalD(final EvalEnv env) {
            return _value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            _value = Term.toD(v);
        }

        public void assignI(final EvalEnv env, final int v) {
            _value = v;
        }

        public void assignD(final EvalEnv env, final double v) {
            _value = v;
        }
    }
}
