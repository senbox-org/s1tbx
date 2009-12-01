/*
 * $Id: ToolCommand.java,v 1.3 2006/11/22 13:05:36 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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

    private Interactor tool;
    private final InteractorListener toolListener;

    public ToolCommand(String commandID) {
        super(commandID);
        tool = NullInteractor.INSTANCE;
        toolListener = new InternalToolListener();
    }

    public ToolCommand(String commandID, CommandStateListener listener, Interactor tool) {
        this(commandID);
        setTool(tool);
        addCommandStateListener(listener);
    }

    public Interactor getTool() {
        return tool;
    }

    public final void setTool(Interactor tool) {
        Guardian.assertNotNull("tool", tool);
        Interactor oldTool = this.tool;
        if (tool == oldTool) {
            return;
        }
        if (oldTool != null) {
            oldTool.removeListener(toolListener);
        }
        this.tool = tool;
        this.tool.addListener(toolListener);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        adjustToolActivationState();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        adjustToolActivationState();
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
            public void actionPerformed(ActionEvent actionEvent) {
                if (!tool.isActive()) {
                    tool.activate();
                    fireActionPerformed(actionEvent, null);
                }
            }
        };
    }

    private void adjustToolActivationState() {
        if (isSelected() && isEnabled()) {
            if (!tool.isActive()) {
                tool.activate();
            }
        } else if (!isSelected() || !isEnabled()) {
            if (tool.isActive()) {
                tool.deactivate();
            }
        }
    }


    private class InternalToolListener extends AbstractInteractorListener {

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

