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
