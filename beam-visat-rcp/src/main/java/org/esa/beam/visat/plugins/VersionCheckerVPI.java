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
package org.esa.beam.visat.plugins;

import com.bc.ceres.swing.UriLabel;
import org.esa.beam.framework.ui.command.CommandAdapter;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.VersionChecker;
import org.esa.beam.visat.AbstractVisatPlugIn;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

public class VersionCheckerVPI extends AbstractVisatPlugIn {

    private static final int DELAY_MILLIS = 5 * 1000;  // 5 seconds delay

    private static final String DISABLE_HINT = "Please note that you can disable the on-line version check\n" +
            "in the preferences dialog.";

    /**
     * Called by VISAT after the plug-in instance has been registered in VISAT's plug-in manager.
     *
     * @param visatApp a reference to the VISAT application instance.
     */
    public void start(VisatApp visatApp) {
        if (!isVersionCheckQuestionSuppressed() || isVersionCheckEnabled()) {
            final Timer timer = new Timer(DELAY_MILLIS, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    runAuto();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }

        CommandAdapter versionCheckerAction = new CommandAdapter() {
            @Override
            public void actionPerformed(CommandEvent event) {
                runManual();
            }
        };
        CommandManager commandManager = visatApp.getCommandManager();
        ExecCommand versionCheckerCommand = commandManager.createExecCommand("checkForUpdate", versionCheckerAction);
        versionCheckerCommand.setText("Check for New Release...");
        versionCheckerCommand.setShortDescription("Checks for a new release");
        versionCheckerCommand.setParent("help");
        versionCheckerCommand.setPlaceAfter("showUpdateDialog");
        versionCheckerCommand.setPlaceBefore("about");
    }

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    @Override
    public void updateComponentTreeUI() {
    }

    private static void runManual() {
        run(false, true);
    }

    private static void runAuto() {
        final boolean prompt = !isVersionCheckQuestionSuppressed();
        if (prompt) {
            showVersionCheckPrompt();
        }
        if (isVersionCheckEnabled()) {
            run(true, prompt);
        }
    }

    private static void run(final boolean auto, final boolean prompt) {
        final SwingWorker swingWorker = new SwingWorker<Integer, Integer>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return getVersionStatus();
            }

            @Override
            public void done() {
                try {
                    showVersionStatus(auto, prompt, get());
                } catch (InterruptedException e) {
                    showVersionCheckFailedMessage(auto, prompt, e);
                } catch (ExecutionException e) {
                    showVersionCheckFailedMessage(auto, prompt, e.getCause());

                }
            }
        };
        swingWorker.execute();
    }

    private static int getVersionStatus() throws IOException {
        final VersionChecker versionChecker = new VersionChecker();
        VisatApp.getApp().getLogger().info(
                "comparing local software version with the one from " + versionChecker.getRemoteVersionUrlString());
        return versionChecker.compareVersions();
    }

    private static void showVersionStatus(boolean auto, boolean prompt, int versionStatus) {
        if (versionStatus < 0) {
            showOutOfDateMessage(auto);
        } else {
            showUpToDateMessage(auto, prompt);
        }
    }

    private static void showVersionCheckPrompt() {
        final String message = MessageFormat.format("{0} is about to check for a new software version.\n" +
                                                            "Do you want {0} to perform the on-line version check now?", /*I18N*/
                                                    VisatApp.getApp().getAppName());
        VisatApp.getApp().showQuestionDialog(message, VisatApp.PROPERTY_KEY_VERSION_CHECK_ENABLED);
    }

    private static void showOutOfDateMessage(boolean auto) {
        VisatApp.getApp().getLogger().info("version check performed, application is antiquated");
        JLabel beamHomeLabel;
        try {
            beamHomeLabel = new UriLabel(new URI(SystemUtils.getApplicationHomepageUrl()));
        } catch (URISyntaxException e) {
            beamHomeLabel = new JLabel(SystemUtils.getApplicationHomepageUrl());
            beamHomeLabel.setForeground(Color.BLUE.darker());
        }
        Object[] message = new Object[]{
                "A new software version is available.\n" +
                        "Please visit the project's homepage at\n", /*I18N*/
                beamHomeLabel,
                "for detailed information.\n" + /*I18N*/
                        (auto ? "\n" + DISABLE_HINT : "")
        };
        JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(),
                                      message,
                                      getDialogTitle(),
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    private static String getDialogTitle() {
        return SystemUtils.getApplicationName() + " Version Check";
    }

    private static void showUpToDateMessage(boolean auto, boolean prompt) {
        VisatApp.getApp().getLogger().info("version check performed, application is up-to-date");
        if (prompt) {
            JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(),
                                          "The software is up-to-date.\n" +  /*I18N*/
                                                  (auto ? "\n" + DISABLE_HINT : ""),
                                          getDialogTitle(),
                                          JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void showVersionCheckFailedMessage(boolean auto, boolean prompt, Throwable t) {
        VisatApp.getApp().getLogger().severe("I/O error: " + t.getMessage());
        if (prompt) {
            JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(),
                                          "The on-line version check failed,\n" +
                                                  "an I/O error occurred.\n" + /*I18N*/
                                                  (auto ? "\n" + DISABLE_HINT : ""),
                                          getDialogTitle(),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean isVersionCheckEnabled() {
        return getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_VERSION_CHECK_ENABLED, true);
    }

    private static boolean isVersionCheckQuestionSuppressed() {
        return getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_VERSION_CHECK_DONT_ASK, false);
    }

    private static PropertyMap getPreferences() {
        return VisatApp.getApp().getPreferences();
    }
}
