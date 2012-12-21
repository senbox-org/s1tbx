/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.nest.util.ResourceUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * This action launches the default browser to display the NEST web page.
 *
 */
public class ShowHomePageAction extends ExecCommand {
    private static final String HOME_PAGE_URL_DEFAULT = "http://www.array.ca/nest/";

    /**
     * Launches the default browser to display the NEST web site.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        final String homePageUrl = System.getProperty(ResourceUtils.getContextID()+".homePageUrl", HOME_PAGE_URL_DEFAULT);
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
