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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.SystemUtils;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

/**
 * This action launches the default browser to display the BEAM home page.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ShowHomePageAction extends ExecCommand {

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
    }

    /**
     * Launches the default browser to display the BEAM Wiki.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        final String homePageUrl = SystemUtils.getApplicationHomepageUrl();

        try {
            final Desktop desktop = Desktop.getDesktop();
            desktop.browse(URI.create(homePageUrl));
        } catch (IOException e) {
            // TODO - handle
        } catch (UnsupportedOperationException e) {
            // TODO - handle
        }
    }
}
