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

package org.esa.snap.binning.operator;

import org.esa.snap.core.gpf.annotations.Parameter;

/**
 * Configuration of a binning variable.
 *
 * @author Norman Fomferra
 * @see org.esa.snap.binning.VariableContext
 */
public class VariableConfig {
    @Parameter(description = "The name of the variable", notEmpty = true, notNull = true)
    private String name;

    @Parameter(description = "The expression of the variable", notEmpty = true, notNull = true)
    private String expr;

    @Parameter(description = "The valid-expression of the variable. " +
            "If empty, it will be the combination of all variables used in the expression itself.")
    private String validExpr;

    public VariableConfig() {
    }

    public VariableConfig(String name, String expr) {
        this(name, expr, null);
    }

    public VariableConfig(String name, String expr, String validExpr) {
        this.name = name;
        this.expr = expr;
        this.validExpr = validExpr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpr() {
        return expr;
    }

    public void setExpr(String expr) {
        this.expr = expr;
    }

    public String getValidExpr() {
        return validExpr;
    }

    public void setValidExpr(String validExpr) {
        this.validExpr = validExpr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariableConfig that = (VariableConfig) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (expr != null ? !expr.equals(that.expr) : that.expr != null) return false;
        return validExpr != null ? validExpr.equals(that.validExpr) : that.validExpr == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (expr != null ? expr.hashCode() : 0);
        result = 31 * result + (validExpr != null ? validExpr.hashCode() : 0);
        return result;
    }
}
