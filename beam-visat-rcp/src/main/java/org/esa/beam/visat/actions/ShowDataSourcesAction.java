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
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatActivator;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.bc.ceres.core.runtime.Module;

/**
 * This action opens the default browser to display the BEAM data sources
 * web page.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ShowDataSourcesAction extends ExecCommand {
    // todo - convert to properties for NEST (nf - 11.07.2008)
    private static final String BEAM_HELP_MODULE_NAME = "beam-help";
    private static final String DATASOURCES_RESOURCE_PATH = "doc/help/general/BeamDataSources.html";
    private static final String DATASOURCES_PROPERTY_NAME = "beam.datasources.url";

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
            URI resourceUri = getDataSourcesUri();
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(resourceUri);
        } catch (URISyntaxException e) {
            VisatApp.getApp().showErrorDialog("Illegal resource URI:\n" + e.getMessage());
        } catch (IOException e) {
            VisatApp.getApp().showErrorDialog("An I/O error occured:\n" + e.getMessage());
        } catch (UnsupportedOperationException e) {
            VisatApp.getApp().showErrorDialog("The desktop command 'browse' is not supported.:\n" + e.getMessage());
        }
    }

    private URI getDataSourcesUri() throws URISyntaxException {
        URI resourceUri;
        URL resourceUrl = null;
        Module helpModule = getModule(BEAM_HELP_MODULE_NAME);
        if (helpModule != null) {
            resourceUrl = helpModule.getResource(DATASOURCES_RESOURCE_PATH);
        }
        if (resourceUrl != null) {
            resourceUri = resourceUrl.toURI();
        } else {
            String defaultUriString = System.getProperty(DATASOURCES_PROPERTY_NAME, SystemUtils.BEAM_HOME_PAGE);
            resourceUri = new URI(defaultUriString);
        }
        return resourceUri;
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
