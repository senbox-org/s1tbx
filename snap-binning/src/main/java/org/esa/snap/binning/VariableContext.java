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

package org.esa.snap.binning;

/**
 * A context providing the names and numbering of variables used for the binning.
 *
 * @author Norman Fomferra
 */
public interface VariableContext {
    /**
     * @return The number of variables.
     */
    int getVariableCount();

    /**
     * The index of the variable with the given name.
     *
     * @param name The variable name.
     * @return The index, or -1 if the name is unknown.
     */
    int getVariableIndex(String name);

    /**
     * The name of the variable at the given index.
     *
     * @param index The index.
     * @return The name.
     */
    String getVariableName(int index);

    /**
     * The band-maths expression of a variable at the given index.
     * If non-{@code null}, the expression is used to compute the variable samples.
     * If {@code null}, the variable is expected to be present in the sample sources.
     *
     * @param index The index.
     * @return The expression. May be {@code null}.
     */
    String getVariableExpression(int index);

    /**
     * The valid-pixel expression of a variable at the given index.
     * If non-{@code null}, the expression is used to compute where variable samples are valid.
     * If {@code null}, valid-pixel expression will be the combination of the valid-pixel
     * expressions of all variables used in the expression itself.
     *
     * @param index The index.
     * @return The valid-pixel expression. May be {@code null}.
     */
    String getVariableValidExpression(int index);

    /**
     * A Boolean band-maths expression identifying valid source samples.
     *
     * @return The valid-mask expression. May be {@code null}.
     */
    String getValidMaskExpression();
}
