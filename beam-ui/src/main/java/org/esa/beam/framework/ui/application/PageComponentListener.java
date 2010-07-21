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
 * A listener which can be added to an application page.
 * <p>Clients may implement this interface. However, the preferred way to implement this interface is via the
 * {@link org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter}, since this interface may handle more
 * events in the future.</p>
 */
public interface PageComponentListener {
    /**
     * Notifies this listener that the given component has been created.
     *
     * @param component the component that was created
     */
    public void componentOpened(PageComponent component);

    /**
     * Notifies this listener that the given component has been closed.
     *
     * @param component the component that was closed
     */
    public void componentClosed(PageComponent component);

    /**
     * Notifies this listener that the given component has been given focus
     *
     * @param component the component that was given focus
     */
    public void componentFocusGained(PageComponent component);

    /**
     * Notifies this listener that the given component has lost focus.
     *
     * @param component the component that lost focus
     */
    public void componentFocusLost(PageComponent component);

    /**
     * Notifies this listener that the given component was shown.
     *
     * @param component the component that was shown
     */
    public void componentShown(PageComponent component);

    /**
     * Notifies this listener that the given component was hidden.
     *
     * @param component the component that was hidden
     */
    public void componentHidden(PageComponent component);
}
