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

package com.bc.ceres.swing.actions;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.net.URL;

/**
 * An abstract base class for generic actions which can is executed
 * either when {@link #actionPerformed} is called from a Swing UI,
 * or programmatically by invoking {@link #execute}.
 *
 * @author Norman Fomferra
 * @see #actionPerformed(java.awt.event.ActionEvent)
 * @since Ceres 0.10
 */
public abstract class AbstractSystemAction extends AbstractAction {

    protected AbstractSystemAction(String name, KeyStroke acceleratorKey, String iconResource) {
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        putValue(Action.NAME, name);
        putValue(Action.ACCELERATOR_KEY, acceleratorKey);
        if (iconResource != null) {
            putValue(Action.SMALL_ICON, loadIcon("icons_16x16/" + iconResource));
            putValue(Action.LARGE_ICON_KEY, loadIcon("icons_22x22/" + iconResource));
        }
    }

    /**
     * Invoked when an action occurs. The method is implemented as follows:
     * <pre>
     * if (isExecutable()) {
     *     execute();
     * }
     * updateState();
     * </pre>
     *
     * @param e The action event.
     *
     * @see #isExecutable()
     * @see #execute()
     * @see #updateState()
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isExecutable()) {
            execute();
        }
        updateState();
    }

    public abstract boolean isExecutable();

    public abstract void execute();

    /**
     * Invoked to update this action's state, usually in reponse to some application specific events.
     * The method is implemented as follows:
     * <pre>
     * boolean executable = isExecutable();
     * setEnabled(executable);
     * </pre>
     */
    public void updateState() {
        boolean executable = isExecutable();
        // System.out.println(getClass().getSimpleName() + ".updateState: executable=" + executable);
        setEnabled(executable);
    }

    protected ImageIcon loadIcon(String resource) {
        URL url = getClass().getResource(resource);
        if (url == null) {
            return null;
        }
        return new ImageIcon(url);
    }
}