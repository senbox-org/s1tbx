/*
 * Copyright (C) 2014-2016 CS ROMANIA
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
 *  with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf.descriptor.template;

/**
 * Wrapper for scripting context implementations.
 *
 * @author Cosmin Cara
 */
public abstract class TemplateContext<C> {
    protected C context;

    /**
     * Default constructor
     *
     * @param wrappedContext    The wrapped scripting context
     */
    public TemplateContext(C wrappedContext) {
        this.context = wrappedContext;
    }

    /**
     * Returns the wrapped context
     */
    public C getContext() { return this.context; }

    /**
     * Returns the value of the named context parameter.
     *
     * @param name  The parameter name
     */
    public abstract Object getValue(String name);
}
