/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.about;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.esa.snap.rcp.about.AboutBox;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import java.awt.*;

/**
 * @author Norman
 */
@AboutBox(displayName = "RSTB", position = 100)
public class RstbAboutBox extends JPanel {

    public RstbAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        ModuleInfo moduleInfo = Modules.getDefault().ownerOf(RstbAboutBox.class);
        ImageIcon aboutImage = new ImageIcon(RstbAboutBox.class.getResource("about_rstb.jpg"));
        JLabel iconLabel = new JLabel(aboutImage);
        add(iconLabel, BorderLayout.CENTER);
        add(new JLabel("<html><b>Radarsat-2 Toolbox (RSTB) version " + moduleInfo.getImplementationVersion() + "</b>", SwingConstants.RIGHT), BorderLayout.SOUTH);
    }
}
