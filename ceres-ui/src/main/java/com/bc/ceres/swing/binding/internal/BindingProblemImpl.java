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

package com.bc.ceres.swing.binding.internal;

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingProblem;

class BindingProblemImpl implements BindingProblem {
    private final Binding binding;
    private final BindingException cause;

    public BindingProblemImpl(Binding binding, BindingException cause) {
        Assert.notNull(binding, "binding");
        Assert.notNull(cause, "cause");
        this.binding = binding;
        this.cause = cause;
    }

    @Override
    public Binding getBinding() {
        return binding;
    }

    @Override
    public BindingException getCause() {
        return cause;
    }

    @Override
    public int hashCode() {
        return getBinding().hashCode() + getCause().getMessage().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof BindingProblemImpl) {
            BindingProblem other = (BindingProblem) obj;
            return this.getBinding() == other.getBinding()
                    && this.getCause().getMessage().equals(other.getCause().getMessage());
        }
        return false;
    }

    @Override
    public String toString() {
        return getCause().getMessage();
    }
}
