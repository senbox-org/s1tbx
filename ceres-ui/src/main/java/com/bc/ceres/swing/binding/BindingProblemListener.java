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

package com.bc.ceres.swing.binding;

/**
 * A listener used to observe problems which may occur in a {@link BindingContext},
 * e.g. when a user edits a Swing component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.10
 */
public interface BindingProblemListener {
    /**
     * Called when a problem has been reported on a {@link Binding} of a {@link BindingContext}.
     *
     * @param newProblem The new problem.
     * @param oldProblem The old problem, may be {@code null} if no problem existed before.
     */
    void problemReported(BindingProblem newProblem, BindingProblem oldProblem);

    /**
     * Called when a problem has been cleared in a {@link Binding} of a {@link BindingContext}.
     *
     * @param oldProblem The old problem.
     */
    void problemCleared(BindingProblem oldProblem);
}
