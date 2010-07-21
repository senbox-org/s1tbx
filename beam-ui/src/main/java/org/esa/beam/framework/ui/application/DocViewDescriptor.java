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

import org.esa.beam.framework.ui.command.Command;


/**
 * Metadata about a view; a view descriptor is effectively a singleton view
 * definition. A descriptor also acts as a factory which produces new instances
 * of a given view when requested, typically by a requesting application page. A
 * view descriptor can also produce a command which launches a view for display
 * on the page within the current active window.
 *
 * @author Marco Peters (original by Keith Donald of Spring RCP project)
 */
public interface DocViewDescriptor extends PageComponentDescriptor {

    /**
     * Create a command that when executed, will attempt to open the
     * page component described by this descriptor in the provided
     * application window.
     *
     * @param applicationPage The application page.
     *
     * @return The show page component command.
     */
    Command createOpenViewCommand(ApplicationPage applicationPage);
}