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

import org.esa.snap.rcp.about.AboutBox;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Norman
 */
@AboutBox(displayName = "RSTB", position = 100)
public class RstbAboutBox extends JPanel {

    private final static String releaseNotesHTTP = "https://github.com/senbox-org/s1tbx/blob/master/ReleaseNotes.md";

    public RstbAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        ImageIcon aboutImage = new ImageIcon(RstbAboutBox.class.getResource("about_rstb.jpg"));
        JLabel iconLabel = new JLabel(aboutImage);
        add(iconLabel, BorderLayout.CENTER);
        add(createVersionPanel(), BorderLayout.SOUTH);
    }

    private JPanel createVersionPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        final ModuleInfo moduleInfo = Modules.getDefault().ownerOf(RstbAboutBox.class);
        panel.add(new JLabel("<html><b>Radarsat Toolbox (RSTB) version " + moduleInfo.getImplementationVersion() + "</b>",
                SwingConstants.RIGHT));
        final URI releaseNotesURI = getReleaseNotesURI();
        if (releaseNotesURI != null) {
            final JLabel releaseNoteLabel = new JLabel("<html><a href=\"" + releaseNotesURI.toString() + "\">Release Notes</a>",
                    SwingConstants.RIGHT);
            releaseNoteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            releaseNoteLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(releaseNotesHTTP));
            panel.add(releaseNoteLabel);
        }
        return panel;
    }

    private URI getReleaseNotesURI() {
        try {
            return new URI(releaseNotesHTTP);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
