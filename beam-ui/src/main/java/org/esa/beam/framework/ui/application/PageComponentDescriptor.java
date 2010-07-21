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

import javax.swing.Icon;
import java.awt.Dimension;

/**
 * Metadata about a page component. A page descriptor is effectively a
 * singleton page component definition. A page descriptor also acts as a factory
 * which produces new instances of a given page component when requested,
 * typically by a requesting application page. A page window descriptor
 * can also produce a command which launches a page window for display
 * on the page within the current active application window.
 */
public interface PageComponentDescriptor extends PropertyChangeEmitter {

    String PROPERTY_KEY_TITLE = "title";

    String PROPERTY_KEY_TAB_TITLE = "tabTitle";

    String PROPERTY_KEY_DESCRIPTION = "description";

    String PROPERTY_KEY_SMALL_ICON = "smallIcon";
    
    String PROPERTY_KEY_LARGE_ICON = "largeIcon";

    String PROPERTY_KEY_MAXIMIZABLE = "maximizable";

    String PROPERTY_KEY_HELP_ID = "helpId";

    /////////////////////////////////////////////////////////////////////////
    // The factory method for a PageComponent

    PageComponent createPageComponent();

    /////////////////////////////////////////////////////////////////////////
    // Page descriptor properties

    String getId();

    /**
     * @return The help identifier.
     */
    String getHelpId();

    /**
     * @param helpId The help identifier.
     */
    void setHelpId(String helpId);

    /**
     * @return The window title.
     */
    String getTitle();

    /**
     * @param title The window title.
     */
    void setTitle(String title);

    /**
     * @return The window tab-title.
     */
    String getTabTitle();

    /**
     * @param tabTitle The window tab-title.
     */
    void setTabTitle(String tabTitle);


    /**
     * @return The window description.
     */
    String getDescription();

    /**
     * @param description The window description.
     */
    void setDescription(String description);

    /**
     * @return The small window icon or <code>null</code> if not set.
     */
    Icon getSmallIcon();

    /**
     * @return The large window icon or <code>null</code> if not set.
     */
    Icon getLargeIcon();

    /**
     * @param icon The small window icon or <code>null</code> if not set.
     */
    void setSmallIcon(Icon icon);

    /**
     * @param icon The large window icon or <code>null</code> if not set.
     */
    void setLargeIcon(Icon icon);

    /**
     * @return <code>true</code> if the window is visible, <code>false</code> otherwise.
     */
    boolean isVisible();

    /**
     * @param state <code>true</code> if the window shall become visible, <code>false</code> otherwise.
     */
    void setVisible(boolean state);

    /**
     * @return <code>true</code> if the window is enabled, <code>false</code> otherwise.
     */
    boolean isEnabled();

    /**
     * @param state <code>true</code> if the window is enabled, <code>false</code> otherwise.
     */
    void setEnabled(boolean state);

    /**
     * @return <code>true</code> if the window is maximizable, <code>false</code> otherwise.
     */
    boolean isMaximizable();

    /**
     * @param state <code>true</code> if the window is maximizable, <code>false</code> otherwise.
     */
    void setMaximizable(boolean state);

    /**
     * @return The preferred size.
     */
    Dimension getPreferredSize();

    /**
     * @param preferredSize The preferred size.
     */
    void setPreferredSize(Dimension preferredSize);
}
