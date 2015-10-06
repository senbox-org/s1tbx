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
package org.esa.snap.core.param;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * A <code>ParamEditor</code> provides a UI component which is used to modify the value of a parameter.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public interface ParamEditor {

    /**
     * Gets the parameter to which this editor belongs to.
     */
    Parameter getParameter();

    /**
     * Gets the label component.
     */
    JLabel getLabelComponent();

    /**
     * Sets the label component.
     */
    void setLabelComponent(JLabel label);

    /**
     * Gets the physical unit label component.
     */
    JLabel getPhysUnitLabelComponent();

    /**
     * Sets the physical unit component.
     */
    void setPhysUnitLabelComponent(JLabel label);

    /**
     * Gets the component used to edit the parameter's value.
     * @return the UI editor component
     */
    JComponent getEditorComponent();

    /**
     * Gets the component used in the GUI. This component is not necessarily the same as the one returned by the {@link
     * #getEditorComponent} method. For example, the editor component could be a {@link javax.swing.JTextArea}, whereas
     * the actual component used in the GUI is a {@link javax.swing.JScrollPane} which decorates the text area field. In
     * most cases this method will simply return the editor component.
     *
     * @return the editor component or the editor component wrapped
     *
     * @see #getEditorComponent()
     */
    JComponent getComponent();

    /**
     * Checks whether or not the editor is enabled,
     */
    boolean isEnabled();

    /**
     * Enables/disables this editor.
     */
    void setEnabled(boolean enabled);

    /**
     * Tells the UI to update it's state.
     */
    void updateUI();

    /**
     * Tells the UI to reconfigure itself, since the parameter's properties have changed.
     */
    void reconfigureUI();

    /**
     * Returns an optional exception handler for this editor.
     */
    ParamExceptionHandler getExceptionHandler();
}

