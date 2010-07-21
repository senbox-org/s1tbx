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
 * Mediator between the application and the view. The application uses this
 * class to get the view's local action handlers. The view uses this class to
 * get information about how the view is displayed in the application (for
 * example, on which window).
 * <p/>
 * <p>Clients shall not implement this interface, it is provided by the framework via the
 * {@link PageComponent#getContext} method.</p>
 *
 * @author Norman Fomferra (original by Keith Donald of Spring RCP project)
 */
public interface PageComponentContext {
    /**
     * Gets the application's page.
     *
     * @return The application page.
     */
    ApplicationPage getPage();

    /**
     * Gets the associated page component pane.
     *
     * @return The page component pane.
     */
    PageComponentPane getPane();
}
