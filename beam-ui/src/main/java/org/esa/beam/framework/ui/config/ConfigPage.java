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
package org.esa.beam.framework.ui.config;

import java.awt.Component;

import javax.swing.Icon;

import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.util.PropertyMap;

/**
 * This interface represents a page within a configuration dialog.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public interface ConfigPage {

    ParamGroup getConfigParams();

    PropertyMap getConfigParamValues(PropertyMap propertyMap);

    void setConfigParamValues(PropertyMap propertyMap, ParamExceptionHandler errorHandler);


    /**
     * Returns the key used to identify this page in a multiple page config dialog.
     *
     * @return the key, must not be <code>null</code> and unique within the page.
     */
    String getKey();

    /**
     * Returns the human readable tile of the page which is displayed in the config dialog's tree view and title bar.
     *
     * @return the title, must not be <code>null</code>
     */
    String getTitle();

    /**
     * Returns an icon for the page which is displayed in the config dialog's tree view.
     *
     * @return an icon or <code>null</code> if no icon is used
     */
    Icon getIcon();

    /**
     * Returns an array of sub-pages this page has. Subpages are displayed in the config dialog's tree view as children
     * of this page.
     *
     * @return an array of sub-pages or <code>null</code> if no sub-pages are used
     */
    ConfigPage[] getSubPages();

    /**
     * Returns the UI component which lets a user edit this page. The component is shown on the right side o the config
     * dialog if a user clicks on this page in the tree view.
     *
     * @return the UI component, must not be <code>null</code>
     */
    Component getPageUI();

    /**
     * Applies the modifications made on this page.
     */
    void applyPage();

    /**
     * Restores the modifications made on this page.
     */
    void restorePage();

    /**
     * Checks whether or not this page has been modified.
     */
    boolean isModified();

    /**
     * Called when the config dialog is commited. All user inputs have already been verified.
     */
    void onOK();

    /**
     * Verifies whether or not this page is valid or not.
     */
    boolean verifyUserInput();

    /**
     * Called when the config dialog is set to visible.
     */
    void updatePageUI();
}
