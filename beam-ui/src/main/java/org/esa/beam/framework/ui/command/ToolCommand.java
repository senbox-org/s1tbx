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
package org.esa.beam.framework.ui.command;

import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.InteractorListener;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import org.esa.beam.util.Guardian;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenuItem;
import java.awt.event.ActionEvent;

/**
 * A command which activates a tool.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class ToolCommand extends SelectableCommand {

    private Interactor interactor;
    private final InteractorListener toolListener;

    public ToolCommand(String commandID) {
        super(commandID);
        interactor = NullInteractor.INSTANCE;
        toolListener = new InternalInteractorListener();
    }

    public ToolCommand(String commandID, CommandStateListener listener, Interactor interactor) {
        this(commandID);
        setInteractor(interactor);
        addCommandStateListener(listener);
    }

    public Interactor getInteractor() {
        return interactor;
    }

    public final void setInteractor(Interactor interactor) {
        Guardian.assertNotNull("interactor", interactor);
        Interactor oldTool = this.interactor;
        if (interactor == oldTool) {
            return;
        }
        if (oldTool != null) {
            oldTool.removeListener(toolListener);
        }
        this.interactor = interactor;
        this.interactor.addListener(toolListener);
    }

    /**
     * Adds a command state listener.
     *
     * @param l the command listener
     */
    public void addCommandStateListener(CommandStateListener l) {
        addEventListener(CommandStateListener.class, l);
    }

    /**
     * Removes a command state listener.
     *
     * @param l the command listener
     */
    public void removeCommandStateListener(CommandStateListener l) {
        removeEventListener(CommandStateListener.class, l);
    }

    /**
     * Creates a menu item (a <code>JMenuItem</code> or <code>JCheckBoxMenuItem</code> instance) for this command
     * group.
     */
    @Override
    public JMenuItem createMenuItem() {
        return getCommandUIFactory().createMenuItem(this);
    }

    /**
     * Creates an appropriate tool bar button for this command.
     */
    @Override
    public AbstractButton createToolBarButton() {
        return getCommandUIFactory().createToolBarButton(this);
    }

    @Override
    protected Action createAction() {
        return new AbstractAction() {

            /**
             * Invoked when an action occurs.
             */
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                    fireActionPerformed(actionEvent, null);
            }
        };
    }

    private class InternalInteractorListener extends AbstractInteractorListener {

        @Override
        public void interactorActivated(Interactor interactor) {
            setSelected(true);
        }

        @Override
        public void interactorDeactivated(Interactor interactor) {
            setSelected(false);
        }
    }

}

