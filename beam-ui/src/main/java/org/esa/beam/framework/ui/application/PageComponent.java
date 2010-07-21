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

import com.bc.ceres.binding.PropertyChangeEmitter;
import com.bc.ceres.swing.selection.SelectionContext;

import javax.swing.Icon;

/**
 * A page component is displayed within an area on the page
 * associated with an application window. There can be multiple components
 * per page; a single page component can only be displayed once on a
 * single page.
 * <p/>
 * Components instances encapsulate the creation of and access to the visual
 * presentation of the underlying control. A component's descriptor --
 * which is effectively a singleton -- can be asked to instantiate new
 * instances of a single page component for display within an application
 * with multiple windows. In other words, a single page component instance is
 * never shared between windows.
 * <p/>
 * <p/>
 * <p>This interface is intended to be implemented by clients. However, the preferred way to implement this interface is via the
 * {@link org.esa.beam.framework.ui.application.support.AbstractToolView}, since the actual interface may change in the future.</p>
 *
 * @author Norman Fomferra (original by Keith Donald of Spring RCP project)
 */
public interface PageComponent extends ControlFactory, PropertyChangeEmitter {
    /**
     * Gets the page component identifier.
     *
     * @return The page component identifier.
     */
    String getId();

    /**
     * Gets the descriptor.
     *
     * @return the descriptor
     * @see #setDescriptor(PageComponentDescriptor)
     */
    PageComponentDescriptor getDescriptor();

    /**
     * Sets the descriptor.
     * <p>Clients must not call this method directly. It is called only once by the framework after a {@link PageComponentContext}
     * has been created and before the framework calls the {@link #setContext} method.</p>
     *
     * @param descriptor the descriptor
     * @see #getDescriptor()
     */
    void setDescriptor(PageComponentDescriptor descriptor);

    /**
     * Gets the page component's context.
     *
     * @return The the page component's context instance as passed into the {@link #setContext(PageComponentContext)} method.
     */
    PageComponentContext getContext();

    /**
     * Initialises this page component with the given context.
     * <p>Clients must not call this method directly. It is called only once by the framework after a {@link PageComponentContext}
     * has been created and before the framework calls the {@link #getControl} method in order to let the client get or create
     * the user interface.</p>
     * <p>Implementors shall store the given tool context instance and
     * let the {@link #getContext} method return it.</p>
     *
     * @param context The page component's context.
     */
    void setContext(PageComponentContext context);

    /**
     * Frees all resources allocated by this page component.
     * <p>Clients must not call this method directly.</p>
     */
    void dispose();

    void componentOpened();

    void componentClosed();

    void componentShown();

    void componentHidden();

    void componentFocusGained();

    void componentFocusLost();

    // todo - harmonize with descriptor
    String getTitle(); // todo - rename to displayName
    Icon getIcon();

    /**
     * Gets the current selection context, if any.
     * @return The current selection context, or {@code null} if none exists.
     * @since BEAM 4.7
     */
    SelectionContext getSelectionContext();
}
