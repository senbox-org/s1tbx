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
package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.Variable;

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
        return new ConstantB(name, value);
    }

    /**
     * Creates a new <code>int</code> constant.
     * @param name the constant's name
     * @param value the constant's final value
     * @return a symbol representing the constant, never <code>null</code>
     */
    public static Symbol createConstant(final String name, final int value) {
        return new ConstantI(name, value);
    }

    /**
     * Creates a new <code>double</code> constant.
     * @param name the constant's name
     * @param value the constant's final value
     * @return a symbol representing the constant, never <code>null</code>
     */
    public static Symbol createConstant(final String name, final double value) {
        return new ConstantD(name, value);
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

    public static final class ConstantB extends AbstractSymbol.B {

        private boolean value;

        public ConstantB(final String name, final boolean value) {
            super(name);
            this.value = value;
        }

        public boolean evalB(final EvalEnv env) {
            return value;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    public static final class ConstantI extends AbstractSymbol.I {

        private int value;

        public ConstantI(String name, int value) {
            super(name);
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public int evalI(final EvalEnv env) {
            return value;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    public static class ConstantD extends AbstractSymbol.D {

        private double value;

        public ConstantD(String name, double value) {
            super(name);
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double evalD(final EvalEnv env) {
            return value;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    public static final class VariableB extends AbstractSymbol.B implements Variable {

        private boolean value;

        public VariableB(final String name, final boolean value) {
            super(name);
            this.value = value;
        }

        public boolean evalB(final EvalEnv env) {
            return value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            value = v;
        }

        public void assignI(final EvalEnv env, final int v) {
            value = Term.toB(v);
        }

        public void assignD(final EvalEnv env, final double v) {
            value = Term.toB(v);
        }

        @Override
        public boolean isConst() {
            return false;
        }
    }

    public static final class VariableI extends AbstractSymbol.I implements Variable {

        private int value;
        private Integer min;
        private Integer max;
        private Integer step;

        public VariableI(String name, int value) {
            this(name, value, null, null, null);
        }

        public VariableI(String name, int value, Integer min, Integer max, Integer step) {
            super(name);
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public Integer getMin() {
            return min;
        }

        public void setMin(Integer min) {
            this.min = min;
        }

        public Integer getMax() {
            return max;
        }

        public void setMax(Integer max) {
            this.max = max;
        }

        public Integer getStep() {
            return step;
        }

        public void setStep(Integer step) {
            this.step = step;
        }

        public int evalI(final EvalEnv env) {
            return value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            value = Term.toI(v);
        }

        public void assignI(final EvalEnv env, final int v) {
            value = v;
        }

        public void assignD(final EvalEnv env, final double v) {
            value = Term.toI(v);
        }

        @Override
        public boolean isConst() {
            return false;
        }
    }

    public static class VariableD extends AbstractSymbol.D implements Variable {

        private double value;
        private Double min;
        private Double max;
        private Double step;

        public VariableD(String name, double value) {
            this(name, value, null, null, null);
        }

        public VariableD(String name, double value, Double min, Double max, Double step) {
            super(name);
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public Double getMin() {
            return min;
        }

        public void setMin(Double min) {
            this.min = min;
        }

        public Double getMax() {
            return max;
        }

        public void setMax(Double max) {
            this.max = max;
        }

        public Double getStep() {
            return step;
        }

        public void setStep(Double step) {
            this.step = step;
        }

        public double evalD(final EvalEnv env) {
            return value;
        }

        public void assignB(final EvalEnv env, final boolean v) {
            value = Term.toD(v);
        }

        public void assignI(final EvalEnv env, final int v) {
            value = v;
        }

        public void assignD(final EvalEnv env, final double v) {
            value = v;
        }

        @Override
        public boolean isConst() {
            return false;
        }
    }

}
