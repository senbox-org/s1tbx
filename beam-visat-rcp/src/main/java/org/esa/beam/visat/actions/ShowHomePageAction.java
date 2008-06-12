package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;

import java.awt.*;
import java.net.URI;
import java.io.IOException;

/**
 * This action lauchches the default browser to display the BEAM Wiki
 * web page.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ShowHomePageAction extends ExecCommand {
    private static final String HOME_PAGE_URL_DEFAULT =
            "http://www.brockmann-consult.de/beam/";

    /**
     * Launches the default browser to display the BEAM Wiki.
     * Invoked when a command action is performed.
     *
     * @param event the command event.
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        final String homePageUrl = System.getProperty("beam.homePageUrl", HOME_PAGE_URL_DEFAULT);
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
