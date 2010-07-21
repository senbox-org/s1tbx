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

package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.PageComponentListener;

public class PageComponentListenerAdapter implements PageComponentListener {
    private PageComponent activeComponent;

    /**
     * @return the active component
     */
    public PageComponent getActiveComponent() {
        return activeComponent;
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentOpened(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentClosed(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentShown(PageComponent component) {
    }

    /**
     * The default implementation does nothing.
     * @param component the component
     */
    public void componentHidden(PageComponent component) {
    }

    /**
     * The default implementation sets the active component.
     * <p>Subclasses may override but shall call the super method first.</p>
     * @param component the component
     */
    public void componentFocusGained(PageComponent component) {
        this.activeComponent = component;
    }

    /**
     * The default implementation nulls the active component.
     * <p>Subclasses may override but shall call the super method first.</p>
     * @param component the component
     */
    public void componentFocusLost(PageComponent component) {
        this.activeComponent = null;
    }
}