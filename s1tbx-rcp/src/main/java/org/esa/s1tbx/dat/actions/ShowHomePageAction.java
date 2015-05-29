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

import org.esa.snap.util.SystemUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;

/**
 * This action launches the default browser to display the project web page.
 */
@ActionID(
        category = "Help",
        id = "ShowHomePageAction"
)
@ActionRegistration(
        displayName = "#CTL_ShowHomePageAction_MenuText",
        popupText = "#CTL_ShowHomePageAction_MenuText",
        lazy = true
)
@ActionReferences({
        @ActionReference(
                path = "Help",
                position = 110
        )
})
@NbBundle.Messages({
        "CTL_ShowHomePageAction_MenuText=SNAP Home Page",
        "CTL_ShowHomePageAction_ShortDescription=Show the toolboxes website"
})
public class ShowHomePageAction extends AbstractAction {
    private static final String HOME_PAGE_URL_DEFAULT = "https://sentinel.esa.int/web/sentinel/toolboxes";

    /**
     * Launches the default browser to display the web site.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        final String homePageUrl = System.getProperty(SystemUtils.getApplicationContextId() + ".homePageUrl", HOME_PAGE_URL_DEFAULT);
        final Desktop desktop = Desktop.getDesktop();

        try {
            desktop.browse(URI.create(homePageUrl));
        } catch (IOException e) {
            // TODO - handle
        } catch (UnsupportedOperationException e) {
            // TODO - handle
        }
    }
}
