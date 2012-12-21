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
package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic Check List Dialog
 */
public class CheckListDialog extends ModalDialog {
    private final List<JCheckBox> checkBoxList = new ArrayList<JCheckBox>(3);
    private final Map<String, Boolean> items;
    private boolean ok = false;

    public CheckListDialog(final String title, final Map<String, Boolean> items) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK, null);
        this.items = items;

        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        
        for(String name : items.keySet()) {
            final JCheckBox checkBox = new JCheckBox(name);
            checkBoxList.add(checkBox);
            content.add(checkBox);

            checkBox.setSelected(items.get(name));
        }

        getJDialog().setMinimumSize(new Dimension(200, 100));

        setContent(content);
    }

    protected void onOK() {
        for(JCheckBox checkBox : checkBoxList) {
            items.put(checkBox.getText(), checkBox.isSelected());
        }

        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

}