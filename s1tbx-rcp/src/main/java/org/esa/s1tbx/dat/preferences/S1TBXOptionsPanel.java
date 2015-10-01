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
package org.esa.s1tbx.dat.preferences;

import org.esa.snap.rcp.SnapApp;
import org.esa.snap.runtime.Config;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

final class S1TBXOptionsPanel extends javax.swing.JPanel {

    private static String useFileCache = "s1tbx.readers.useFileCache";

    private javax.swing.JCheckBox useFileCacheCheckBox;

    S1TBXOptionsPanel(final S1TBXOptionsPanelController controller) {
        initComponents();
        // listen to changes in form fields and call controller.changed()
        useFileCacheCheckBox.addItemListener(e -> controller.changed());

    }

    private void initComponents() {
        useFileCacheCheckBox = new javax.swing.JCheckBox();
        Mnemonics.setLocalizedText(useFileCacheCheckBox,
                                   NbBundle.getMessage(S1TBXOptionsPanel.class,
                                                       "S1TBXOptionsPanel.useFileCacheCheckBox.text")); // NOI18N


        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addComponent(useFileCacheCheckBox)
                                                            .addGap(0, 512, Short.MAX_VALUE)
                                          ).addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                          .addComponent(useFileCacheCheckBox)
                                          .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                          .addContainerGap())
        );
    }

    void load() {
        useFileCacheCheckBox.setSelected(
                Config.instance().preferences().getBoolean(useFileCache, false));
    }

    void store() {
        final Preferences preferences = Config.instance().preferences();
        preferences.putBoolean(useFileCache, useFileCacheCheckBox.isSelected());

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            SnapApp.getDefault().getLogger().severe(e.getMessage());
        }
    }

    boolean valid() {
        // Check whether form is consistent and complete
        return true;
    }

}
