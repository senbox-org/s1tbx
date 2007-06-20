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

import org.esa.beam.framework.ui.tool.Tool;
import org.esa.beam.framework.ui.tool.ToolAdapter;
import org.esa.beam.framework.ui.tool.ToolEvent;
import org.esa.beam.framework.ui.tool.ToolListener;
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
 * @version $Revision: 1.3 $  $Date: 2006/11/22 13:05:36 $
 */
public class ToolCommand extends SelectableCommand {

    private Tool _tool;
    private final ToolListener _toolListener;

    public ToolCommand(String commandID) {
        super(commandID);
        _toolListener = new InternalToolListener();
    }

    public ToolCommand(String commandID, CommandStateListener listener, Tool tool) {
        super(commandID);
        _toolListener = new InternalToolListener();
        setTool(tool);
        addCommandStateListener(listener);
    }

    public Tool getTool() {
        return _tool;
    }

    public void setTool(Tool tool) {
        Guardian.assertNotNull("tool", tool);
        Tool oldTool = _tool;
        if (tool == oldTool) {
            return;
        }
        if (oldTool != null) {
            oldTool.removeToolListener(_toolListener);
        }
        _tool = tool;
        _tool.addToolListener(_toolListener);
        setSelected(_tool.isActive());
        setEnabled(_tool.isEnabled());
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected) {
            _tool.activate();
        } else {
            _tool.deactivate();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
//        Debug.trace("ToolCommand.setEnabled " + enabled);
        super.setEnabled(enabled);
        _tool.setEnabled(enabled);
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
        return (AbstractAction) new AbstractAction() {

            /**
             * Invoked when an action occurs.
             */
            public void actionPerformed(ActionEvent actionEvent) {
                if (!_tool.isActive()) {
                    _tool.activate();
                    fireActionPerformed(actionEvent, null);
                }
            }
        };
    }


    class InternalToolListener extends ToolAdapter {

        /**
         * Invoked if a tool was activated.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolActivated(ToolEvent toolEvent) {
            setSelected(true);
        }

        /**
         * Invoked if a tool was activated.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolDeactivated(ToolEvent toolEvent) {
            setSelected(false);
        }

        /**
         * Invoked if a tool was enabled.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolEnabled(ToolEvent toolEvent) {
            setEnabled(true);
        }

        /**
         * Invoked if a tool was disabled.
         *
         * @param toolEvent the event which caused the state change.
         */
        @Override
        public void toolDisabled(ToolEvent toolEvent) {
            setEnabled(false);
        }

    }

}

