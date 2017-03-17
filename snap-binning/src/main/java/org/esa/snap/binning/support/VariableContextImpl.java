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

package org.esa.snap.binning.support;

import org.esa.snap.binning.VariableContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the variable context for all variables referenced
 * from a given L3 processing request. It therefore provides the list of actual
 * raster data nodes to be read from a given input data product.
 * <p>
 * The i=0...n-1 measurements in an {@link org.esa.snap.binning.Observation} directly correspond
 * to the i=0...n-1 variables provided by {@code VariableContext}.
 *
 * @author Norman
 */
public class VariableContextImpl implements VariableContext {
    private final ArrayList<String> names;
    private final Map<String, Definition> entries;
    private String maskExpr;

    public VariableContextImpl() {
        this.names = new ArrayList<>();
        this.entries = new HashMap<>();
    }

    public void defineVariable(String name) {
        defineVariable(name, null, null);
    }

    public void defineVariable(String name, String expr) {
        defineVariable(name, expr, null);
    }

    public void defineVariable(String name, String expr, String validExpr) {
        if (!names.contains(name)) {
            names.add(name);
        }
        if (expr != null) {
            entries.put(name, new Definition(expr, validExpr));
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
        Definition definition = entries.get(names.get(i));
        return definition != null ? definition.expr : null;
    }

    @Override
    public String getVariableValidExpression(int i) {
        Definition definition = entries.get(names.get(i));
        return definition != null ? definition.validExpr : null;
    }

    @Override
    public int getVariableIndex(String name) {
        return names.indexOf(name);
    }

    private static class Definition {
        private String expr;
        private String validExpr;

        private Definition(String expr, String validExpr) {
            this.expr = expr;
            this.validExpr = validExpr;
        }
    }
}
