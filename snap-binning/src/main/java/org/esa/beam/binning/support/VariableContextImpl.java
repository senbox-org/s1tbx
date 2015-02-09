/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.support;

import org.esa.beam.binning.VariableContext;

import java.util.ArrayList;

/**
 * This is the variable context for all variables referenced
 * from a given L3 processing request. It therefore provides the list of actual
 * raster data nodes to be read from a given input data product.
 * <p/>
 * The i=0...n-1 measurements in an {@link org.esa.beam.binning.Observation} directly correspond
 * to the i=0...n-1 variables provided by {@code VariableContext}.
 *
 * @author Norman
 */
public class VariableContextImpl implements VariableContext {
    private final ArrayList<String> names;
    private final ArrayList<String> exprs;
    private String maskExpr;

    public VariableContextImpl() {
        this.names = new ArrayList<String>();
        this.exprs = new ArrayList<String>();
    }

    public void defineVariable(String name) {
        defineVariable(name, null);
    }

    public void defineVariable(String name, String expr) {
        final int index = names.indexOf(name);
        if (index >= 0) {
            if (expr != null) {
                exprs.set(index, expr);
            }
        } else {
            names.add(name);
            exprs.add(expr);
        }
    }

    @Override
    public String getValidMaskExpression() {
        return maskExpr;
    }

    public void setMaskExpr(String maskExpr) {
        this.maskExpr = maskExpr;
    }

    @Override
    public int getVariableCount() {
        return names.size();
    }

    @Override
    public String getVariableName(int i) {
        return names.get(i);
    }

    @Override
    public String getVariableExpression(int i) {
        return exprs.get(i);
    }

    @Override
    public int getVariableIndex(String name) {
        return names.indexOf(name);
    }
}
