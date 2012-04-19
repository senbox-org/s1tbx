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

import com.bc.ceres.core.runtime.Module;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatActivator;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This action opens the default browser to display the BEAM data sources
 * web page.
 *
 * @author Ralf Quast
 * @author Norman Fomferra
 */
public class ShowDataSourcesAction extends ExecCommand {

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE));
    }

    /**
     * Opens the default browser to display the BEAM data sources web page.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        try {
            tryUri();
        } catch (URISyntaxException e) {
            VisatApp.getApp().showErrorDialog("Illegal resource URI:\n" + e.getMessage());
        } catch (IOException e) {
            VisatApp.getApp().showErrorDialog("An I/O error occurred:\n" + e.getMessage());
        } catch (UnsupportedOperationException e) {
            VisatApp.getApp().showErrorDialog("The desktop command 'browse' is not supported.:\n" + e.getMessage());
        }
    }

    private void tryUri() throws URISyntaxException, IOException {
        try {
            tryRemoteUri();
        } catch (Exception e) {
            tryLocalUri();
        }
    }

    private void tryRemoteUri() throws URISyntaxException, IOException {
        final URI uri = getRemoteDataSourcesUri();
        if (uri != null) {
            Desktop.getDesktop().browse(uri);
        } else {
            tryLocalUri();
        }
    }

    private void tryLocalUri() throws URISyntaxException, IOException {
        final URI uri = getLocalDataSourcesUri();
        if (uri != null) {
            Desktop.getDesktop().browse(uri);
        }
    }

    private URI getRemoteDataSourcesUri() throws URISyntaxException {
        String uri = System.getProperty(SystemUtils.getApplicationContextId() + ".datasources.url");
        if (uri != null) {
            return new URI(uri);
        } else {
            return null;
        }
    }

    private URI getLocalDataSourcesUri() throws URISyntaxException {
        URL url = null;
        Module helpModule = getModule("beam-help");
        if (helpModule != null) {
            url = helpModule.getResource("doc/help/general/BeamDataSources.html");
        }
        if (url != null) {
            return url.toURI();
        } else {
            return null;
        }
    }

    private static Module getModule(String symbolicName) {
        final Module[] modules = VisatActivator.getInstance().getModuleContext().getModules();
        for (Module module : modules) {
            if (module.getSymbolicName().equals(symbolicName)) {
                return module;
            }
        }
        return null;
    }
}
