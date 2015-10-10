/*
 * Copyright (C) 2015 Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sentinel1.rcp;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.ModalDialog;
import org.esa.snap.ui.SnapFileChooser;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@ActionID(category = "File", id = "ViewS1AuxFileAction")
@ActionRegistration(displayName = "#CTL_ViewS1AuxFileActionText", lazy = false)
@ActionReferences({
        @ActionReference(path = "Menu/Radar/SAR Utilities/Auxiliary Files")
})

@NbBundle.Messages({
        "CTL_ViewS1AuxFileActionText=View S1 Auxilary File",
        "CTL_ViewS1AuxFileActionDescription=Open Sentinel-1 orbit file or calibration auxiliary file"
})
public class ViewS1AuxFileAction extends AbstractSnapAction {

    private static final String auxDirPreferenceKey = "s1tbx.last_aux_file_dir";

    public ViewS1AuxFileAction() {

        putValue(Action.NAME, Bundle.CTL_ViewS1AuxFileActionText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ViewS1AuxFileActionDescription());

    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try {
            final SnapFileFilter filter = new SnapFileFilter("S-1 Aux data file",
                                                             new String[]{".zip",".EOF"},
                                                             "S-1 Aux data file");
            final Preferences preferences = SnapApp.getDefault().getPreferences();
            final File currentDir = new File(preferences.get(auxDirPreferenceKey, SystemUtils.getUserHomeDir().getPath()));

            final SnapFileChooser fileChooser = new SnapFileChooser();
            fileChooser.setDialogTitle(Bundle.CTL_ViewS1AuxFileActionDescription());
            fileChooser.setFileFilter(filter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setCurrentDirectory(currentDir);
            final int result = fileChooser.showOpenDialog(SnapApp.getDefault().getMainFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                if (file != null) {
                    final File parentFolder = file.getAbsoluteFile().getParentFile();
                    if (parentFolder != null) {
                        preferences.put(auxDirPreferenceKey, parentFolder.getPath());
                    }

                    String content = readFile(file);
                    TextPaneDialog dlg = new TextPaneDialog(SnapApp.getDefault().getMainFrame(), file.getName(), content);
                    dlg.show();
                }
            }

        } catch (Exception e) {
            SnapDialogs.showError("Unable to import aux data file:"+e.getMessage());
        }
    }

    private String readFile(final File file) throws IOException {
        final StringBuilder str = new StringBuilder();
        if(file.getName().toLowerCase().endsWith(".zip")) {
            final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);
            final Enumeration<? extends ZipEntry> entries = productZip.entries();
            final ZipEntry zipEntry = entries.nextElement();

            try (InputStream fis = productZip.getInputStream(zipEntry)) {
                int content;
                while ((content = fis.read()) != -1) {
                    str.append((char) content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                int content;
                while ((content = fis.read()) != -1) {
                    str.append((char) content);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return str.toString();
    }

    private class TextPaneDialog extends ModalDialog {

        public TextPaneDialog(final Window parent, final String title, final String content) {
            super(parent, title, ID_CLOSE, null);

            final JPanel contentPanel = new JPanel(new BorderLayout(2, 2));
            final JTextPane textPane = new JTextPane();
            textPane.setText(content);

            final JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.setPreferredSize(new Dimension(600, 800));
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            setContent(contentPanel);
        }
    }
}
