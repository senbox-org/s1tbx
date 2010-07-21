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

package org.esa.beam.framework.ui.application;

/**
 * A <code>PageComponentPane</code> is a container that holds the
 * <code>PageComponent</code>'s control, and can add extra decorations (add a toolbar,
 * a border, ...).
 * <p/>
 * This allows for adding extra behaviour to <code>PageComponent</code>s that have to
 * be applied to all <code>PageComponent</code>.
 *
 * @author Norman Fomferra (original by Peter De Bruycker of Spring RCP project)
 */
public interface PageComponentPane extends ControlFactory {

    /**
     * Gets the contained page component.
     *
     * @return The page component.
     */
    PageComponent getPageComponent();
}
