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

import org.junit.Test;

import javax.swing.JPopupMenu;

import static org.junit.Assert.assertEquals;

public class DefaultCommandUIFactoryTest {

    @Test
    public void testAddContextDependingMenuItems() {
        DefaultCommandManager manager = new DefaultCommandManager();
        createCommand("com1", manager);
        createCommand("com2", manager);
        createCommand("com3", manager);
        manager.getCommandAt(0).setProperty(Command.ACTION_KEY_CONTEXT, "image");
        manager.getCommandAt(1).setProperty(Command.ACTION_KEY_CONTEXT, "notImage");
        manager.getCommandAt(2).setProperty(Command.ACTION_KEY_CONTEXT, "image");
        manager.getCommandAt(2).setEnabled(false);

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);
        JPopupMenu menu = uiFactory.addContextDependentMenuItems("image", new JPopupMenu());
        assertEquals(2, menu.getComponentCount());

        menu = uiFactory.addContextDependentMenuItems("notImage", new JPopupMenu());
        assertEquals(1, menu.getComponentCount());
    }

    @Test
    public void testAddContextDependingMenuItemsOnlyOneTime() {
        DefaultCommandManager manager = new DefaultCommandManager();
        createCommand("com1", manager);
        createCommand("com2", manager);
        createCommand("com3", manager);
        manager.getCommandAt(0).setProperty(Command.ACTION_KEY_CONTEXT, "band");
        manager.getCommandAt(1).setProperty(Command.ACTION_KEY_CONTEXT, new String[]{"band", "virtualBand"});
        manager.getCommandAt(2).setProperty(Command.ACTION_KEY_CONTEXT, "virtualBand");

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);

        JPopupMenu bandMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("virtualBand", bandMenu);
        assertEquals(2, bandMenu.getComponentCount());

        JPopupMenu virtualBandMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", virtualBandMenu);
        assertEquals(2, virtualBandMenu.getComponentCount());

        JPopupMenu bandAndVbMenu = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", bandAndVbMenu);
        uiFactory.addContextDependentMenuItems("virtualBand", bandAndVbMenu);
        assertEquals(3, bandAndVbMenu.getComponentCount());
    }

    @Test
    public void testPlaceAtContextTop() {
        DefaultCommandManager manager = new DefaultCommandManager();
        ExecCommand standardCommand1 = createCommand("com1", manager);
        ExecCommand standardCommand2 = createCommand("com2", manager);
        ExecCommand topCommand = createCommand("com3", manager);
        standardCommand1.setProperty(Command.ACTION_KEY_CONTEXT, "band");
        standardCommand2.setProperty(Command.ACTION_KEY_CONTEXT, "band");
        topCommand.setProperty(Command.ACTION_KEY_CONTEXT, "band");

        topCommand.setPlaceAtContextTop(true);

        DefaultCommandUIFactory uiFactory = new DefaultCommandUIFactory();
        uiFactory.setCommandManager(manager);

        JPopupMenu popup = new JPopupMenu();
        uiFactory.addContextDependentMenuItems("band", popup);
        assertEquals(3, popup.getComponentCount());
        String[] expectedOrder = new String[]{"com3", "com1", "com2",};
        assertEquals(expectedOrder[0], popup.getComponent(0).getName());
        assertEquals(expectedOrder[1], popup.getComponent(1).getName());
        assertEquals(expectedOrder[2], popup.getComponent(2).getName());
    }

    private static ExecCommand createCommand(String commandId, DefaultCommandManager manager) {
        ExecCommand command = new ExecCommand();
        command.setCommandID(commandId);
        manager.addCommand(command);
        return command;
    }
}
