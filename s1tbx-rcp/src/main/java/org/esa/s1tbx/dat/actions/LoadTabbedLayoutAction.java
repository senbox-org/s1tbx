/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.actions;

import com.jidesoft.swing.LayoutPersistence;
import org.esa.snap.framework.ui.command.CommandEvent;
import org.esa.snap.util.ResourceUtils;
import org.esa.snap.visat.VisatApp;
import org.esa.snap.visat.actions.AbstractVisatAction;

import java.io.File;

/**
 */
public class LoadTabbedLayoutAction extends AbstractVisatAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        loadTabbedLayout();
    }

    public static void loadTabbedLayout() {
        final LayoutPersistence layoutPersistence = VisatApp.getApp().getMainFrame().getLayoutPersistence();

        final File layoutPath = new File(ResourceUtils.getResFolder(), "tabbed.layout");
        layoutPersistence.loadLayoutDataFromFile(layoutPath.getAbsolutePath());
    }
}
